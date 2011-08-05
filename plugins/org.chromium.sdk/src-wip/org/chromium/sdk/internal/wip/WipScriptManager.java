// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import static org.chromium.sdk.util.BasicUtil.containsKeySafe;
import static org.chromium.sdk.util.BasicUtil.getSafe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptBase;
import org.chromium.sdk.internal.wip.protocol.input.debugger.GetScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData;
import org.chromium.sdk.internal.wip.protocol.output.debugger.GetScriptSourceParams;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Callback;
import org.chromium.sdk.util.AsyncFutureMerger;
import org.chromium.sdk.util.AsyncFutureRef;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * Keeps all current scripts for the debug session and handles script source loading.
 */
class WipScriptManager {
  private final WipTabImpl tabImpl;
  // Access must be synchronized.
  private final Map<String, ScriptData> scriptIdToData = new HashMap<String, ScriptData>();

  /**
   * A future for script pre-load operation. User may call {@link #getScripts} at any time,
   * but we return result only once we have loaded all pre-existing scripts.
   */
  private final AsyncFutureRef<Void> scriptsPreloaded;

  /** Accessed from Dispatch thread only. */
  private ScriptPopulateMode populateMode = new ScriptPopulateMode();

  WipScriptManager(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
    this.scriptsPreloaded = populateMode.createAndInitMasterFuture();
  }

  WipTabImpl getTabImpl() {
    return tabImpl;
  }

  // Run command in dispatch thread so that no scripts event could happen in the meantime.
  RelayOk getScripts(final GenericCallback<Collection<Script>> callback,
      SyncCallback syncCallback) {

    // Async command chain here, wrap syncCallback to guaranteed calling.
    RelaySyncCallback relay = new RelaySyncCallback(syncCallback);

    // Guard for the step one.
    final RelaySyncCallback.Guard guardOne = relay.newGuard();

    // Chain commands are in the reverse order.

    // Wait for script pre-load operation and return scripts.
    final AsyncFuture.Callback<Void> futureCallback = new AsyncFuture.Callback<Void>() {
      @Override
      public void done(Void res) {
        if (callback != null) {
          callback.success(getCurrentScripts());
        }
      }
    };

    // Start everything in dispatch thread (otherwise user may be called from this thread).
    Runnable mainRunnable = new Runnable() {
      @Override
      public void run() {
        RelayOk relayOk =
            scriptsPreloaded.getAsync(futureCallback, guardOne.getRelay().getSyncCallback());
        guardOne.discharge(relayOk);
      }
    };

    return tabImpl.getCommandProcessor().runInDispatchThread(mainRunnable,
        guardOne.asSyncCallback());
  }


  private Collection<Script> getCurrentScripts() {
    synchronized (scriptIdToData) {
      List<Script> list = new ArrayList<Script>(scriptIdToData.size());
      for (ScriptData data : scriptIdToData.values()) {
        if (data.sourceLoadedFuture.isDone()) {
          list.add(data.scriptImpl);
        }
      }
      return list;
    }
  }

  public void scriptIsReportedParsed(ScriptParsedEventData data) {
    final String sourceID = data.scriptId();

    String url = data.url();
    if (url.isEmpty()) {
      url = null;
    }

    ScriptBase.Descriptor<String> descriptor = new ScriptBase.Descriptor<String>(Script.Type.NORMAL,
        sourceID, url, (int) data.startLine(), (int) data.startColumn(), -1);
    final WipScriptImpl script = new WipScriptImpl(this, descriptor);
    final ScriptData scriptData = new ScriptData(script);

    synchronized (scriptIdToData) {
      if (containsKeySafe(scriptIdToData, sourceID)) {
        throw new IllegalStateException("Already has script with id " + sourceID);
      }
      scriptIdToData.put(sourceID, scriptData);
    }

    scriptData.sourceLoadedFuture.initializeRunning(new SourceLoadOperation(script, sourceID));

    final ScriptPopulateMode populateModeSaved = populateMode;

    AsyncFuture.Callback<Boolean> callback;
    SyncCallback syncCallback;

    if (populateModeSaved == null) {
      callback = new AsyncFuture.Callback<Boolean>() {
        @Override
        public void done(Boolean res) {
          tabImpl.getTabListener().getDebugEventListener().scriptLoaded(script);
        }
      };
      syncCallback = null;
    } else {
      populateModeSaved.anotherSourceToWait();

      callback = new AsyncFuture.Callback<Boolean>() {
        @Override
        public void done(Boolean res) {
          populateModeSaved.sourceLoaded(res);
        }
      };
      syncCallback = new SyncCallback() {
        @Override
        public void callbackDone(RuntimeException e) {
          populateModeSaved.sourceLoadedSync(e);
        }
      };
    }

    scriptData.sourceLoadedFuture.getAsync(callback, syncCallback);
  }

  /**
   * Asynchronously loads script source.
   */
  private final class SourceLoadOperation implements AsyncFuture.Operation<Boolean> {
    private final WipScriptImpl script;
    private final String sourceID;

    private SourceLoadOperation(WipScriptImpl script, String sourceID) {
      this.script = script;
      this.sourceID = sourceID;
    }

    @Override
    public RelayOk start(final Callback<Boolean> operationCallback, SyncCallback syncCallback) {
      GenericCallback<GetScriptSourceData> commandCallback =
          new GenericCallback<GetScriptSourceData>() {
        @Override
        public void success(GetScriptSourceData data) {
          String source = data.scriptSource();
          script.setSource(source);
          operationCallback.done(true);
        }
        @Override
        public void failure(Exception exception) {
          throw new RuntimeException(exception);
        }
      };
      GetScriptSourceParams params = new GetScriptSourceParams(sourceID);
      return tabImpl.getCommandProcessor().send(params, commandCallback, syncCallback);
    }
  }

  private class ScriptData {
    final WipScriptImpl scriptImpl;
    final AsyncFutureRef<Boolean> sourceLoadedFuture = new AsyncFutureRef<Boolean>();

    ScriptData(WipScriptImpl scriptImpl) {
      this.scriptImpl = scriptImpl;
    }
  }

   /**
   * Asynchronously loads all script sources that will be referenced from a new debug context
   * (from its stack frames).
   * Must be called from Dispatch thread.
   */
  RelayOk loadScriptSourcesAsync(Set<String> ids, ScriptSourceLoadCallback callback,
      SyncCallback syncCallback) {
    Queue<ScriptData> scripts = new ArrayDeque<ScriptData>(ids.size());
    Map<String, WipScriptImpl> result = new HashMap<String, WipScriptImpl>(ids.size());
    synchronized (scriptIdToData) {
      for (String id : ids) {
        ScriptData data = getSafe(scriptIdToData, id);
        if (data == null) {
          // We probably can't get a script source id without the script already
          // having been reported to us directly.
          throw new IllegalArgumentException("Unknown script id: " + id);
        }
        result.put(data.scriptImpl.getId(), data.scriptImpl);
        if (!data.sourceLoadedFuture.isDone()) {
          scripts.add(data);
        }
      }
    }

    // Start a chain of asynchronous operations.
    // Make sure we call this sync callback sooner or later.
    RelaySyncCallback relay =  new RelaySyncCallback(syncCallback);

    return loadNextScript(scripts, result, callback, relay);
  }

  interface ScriptSourceLoadCallback {
    void done(Map<String, WipScriptImpl> loadedScripts);
  }

  static String convertAlienSourceId(Object sourceIdObj) {
    if (sourceIdObj instanceof String == false) {
      throw new IllegalArgumentException("Script id must be string");
    }
    return (String) sourceIdObj;
  }

  private RelayOk loadNextScript(final Queue<ScriptData> scripts,
      final Map<String, WipScriptImpl> result, final ScriptSourceLoadCallback callback,
      final RelaySyncCallback relay) {
    final ScriptData data = scripts.poll();
    if (data == null) {
      // Terminate the chain of asynchronous loads and pass a result to the callback.
      RelayOk relayOk;
      try {
        if (callback != null) {
          callback.done(result);
        }
      } finally {
        relayOk = relay.finish();
      }
      return relayOk;
    }

    // Create a guard for the case that we fail before issuing next #loadNextScript() call.
    final RelaySyncCallback.Guard guard = relay.newGuard();

    AsyncFuture.Callback<Boolean> futureCallback = new AsyncFuture.Callback<Boolean>() {
      @Override
      public void done(Boolean res) {
        RelayOk relayOk = loadNextScript(scripts, result, callback, relay);
        // We successfully relayed responsibility for operationDestructable to next async call,
        // discharge guard.
        guard.discharge(relayOk);
      }
    };

    // The async operation will call a guard even if something failed within the AsyncFuture.
    return data.sourceLoadedFuture.getAsync(futureCallback, relay.getSyncCallback());
  }

  public void pageReloaded() {
    synchronized (scriptIdToData) {
      scriptIdToData.clear();
    }
  }

  void endPopulateScriptMode() {
    populateMode.endMode();
    populateMode = null;
  }

  /**
   * Right after initialization we come into 'populate scripts' mode, when back-end
   * reports about all pre-existing scripts as if they have just been parsed.
   * <p>
   * We treat this notifications differently: all these scripts must be returned from
   * {@link JavascriptVm#getScripts} from the beginning and only truly new scripts get
   * reported via {@link DebugEventListener#scriptLoaded}.
   * <p>
   * This means that until 'populate mode' ends (and all sources are loaded),
   * {@link JavascriptVm#getScripts} call blocks.
   */
  private static class ScriptPopulateMode {
    /**
     * Future for script preload operation. It completes when all pre-exising scripts
     * are fully loaded (with sources). The operation result value is an array of
     * source loading success/failure flags.
     */
    private final AsyncFutureMerger<Boolean> populateAndLoadSourcesFuture =
        new AsyncFutureMerger<Boolean>();

    /**
     * Reports that 'populate script' mode is finished. However we may still be waiting for
     * the corresponding script sources.
     */
    void endMode() {
      populateAndLoadSourcesFuture.subOperationDone(null);
      populateAndLoadSourcesFuture.subOperationDoneSync(null);
    }

    /**
     * We learned about another pre-existing script. Now we have to wait for its source.
     */
    void anotherSourceToWait() {
      populateAndLoadSourcesFuture.addSubOperation();
    }

    void sourceLoaded(Boolean result) {
      populateAndLoadSourcesFuture.subOperationDone(result);
    }

    /**
     * Additional method that completes {@link #sourceLoaded} and used to be compatible with
     * {@link SyncCallback} paradigm.
     */
    void sourceLoadedSync(RuntimeException e) {
      populateAndLoadSourcesFuture.subOperationDoneSync(e);
    }

    /**
     * Creates a 'master operation' future that hides the complex result value
     * of {@link #populateAndLoadSourcesFuture}. It hides it from Java GC also,
     * so the {@link ArrayList} gets collected once operation is finished.
     */
    AsyncFutureRef<Void> createAndInitMasterFuture() {
      AsyncFutureRef<Void> asyncFutureRef = new AsyncFutureRef<Void>();
      asyncFutureRef.initializeRunning(new AsyncFuture.Operation<Void>() {
        @Override
        public RelayOk start(final Callback<Void> callback, SyncCallback syncCallback) {
          AsyncFuture<?> innerFuture = populateAndLoadSourcesFuture.getFuture();
          return innerFuture.getAsync(new Callback<Object>() {
                @Override
                public void done(Object res) {
                  callback.done(null);
                }
              }, syncCallback);
        }
      });
      return asyncFutureRef;
    }
  }
}

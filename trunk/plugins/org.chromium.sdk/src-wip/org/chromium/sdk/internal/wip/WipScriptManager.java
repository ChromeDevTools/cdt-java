// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptImpl;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.protocol.WipProtocol;
import org.chromium.sdk.internal.wip.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.input.ScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.output.GetScriptSource;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Callback;
import org.chromium.sdk.util.AsyncFutureRef;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructableWrapper;
import org.chromium.sdk.util.DestructingGuard;

/**
 * Keeps all current scripts for the debug session and handles script source loading.
 */
class WipScriptManager {
  private final WipTabImpl tabImpl;
  private final Map<Long, ScriptData> scriptIdToData = new HashMap<Long, ScriptData>();

  WipScriptManager(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  Collection<Script> getScripts() {
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

  public void scriptIsReportedParsed(ParsedScriptSourceData data) {
    final long sourceID = WipProtocol.parseSourceId(data.sourceID());

    String url = data.url();
    if (url.isEmpty()) {
      url = null;
    }

    ScriptImpl.Descriptor descriptor = new ScriptImpl.Descriptor(Script.Type.NORMAL,
        sourceID, url, (int) data.lineOffset(), (int) data.columnOffset(), -1);
    final ScriptImpl script = new ScriptImpl(descriptor, null);
    final ScriptData scriptData = new ScriptData(script);

    synchronized (scriptIdToData) {
      if (scriptIdToData.containsKey(sourceID)) {
        throw new IllegalStateException("Already has script with id " + sourceID);
      }
      scriptIdToData.put(sourceID, scriptData);
    }

    scriptData.sourceLoadedFuture.initializeRunning(new SourceLoadOperation(script, sourceID));

    scriptData.sourceLoadedFuture.getAsync(new AsyncFuture.Callback<Boolean>() {
          @Override
          public void done(Boolean res) {
            if (res) {
              tabImpl.getTabListener().getDebugEventListener().scriptLoaded(script);
            }
          }
        },
        null);
  }

  /**
   * Asynchronously loads script source.
   */
  private final class SourceLoadOperation implements AsyncFuture.Operation<Boolean> {
    private final ScriptImpl script;
    private final long sourceID;

    private SourceLoadOperation(ScriptImpl script, long sourceID) {
      this.script = script;
      this.sourceID = sourceID;
    }

    @Override
    public void start(final Callback<Boolean> operationCallback, SyncCallback syncCallback) {
      WipCommandCallback commandCallback = new WipCommandCallback.Default() {
        @Override
        protected void onSuccess(WipCommandResponse.Success success) {
          ScriptSourceData data;
          try {
            data = success.data().asScriptSourceData();
          } catch (JsonProtocolParseException e) {
            throw new RuntimeException(e);
          }
          String source = data.scriptSource();
          script.setSource(source);
          operationCallback.done(true);
        }
        @Override
        protected void onError(String message) {
          throw new RuntimeException(message);
        }
      };
      GetScriptSource getScriptRequest = new GetScriptSource(sourceID);
      tabImpl.getCommandProcessor().send(getScriptRequest, commandCallback, syncCallback);
    }
  }

  private class ScriptData {
    final ScriptImpl scriptImpl;
    final AsyncFutureRef<Boolean> sourceLoadedFuture = new AsyncFutureRef<Boolean>();

    ScriptData(ScriptImpl scriptImpl) {
      this.scriptImpl = scriptImpl;
    }
  }

   /**
   * Asynchronously loads all script sources that will be referenced from a new debug context
   * (from its stack frames).
   * Must be called from Dispatch thread.
   */
  void loadScriptSourcesAsync(Set<Long> ids, ScriptSourceLoadCallback callback,
      SyncCallback syncCallback) {
    Queue<ScriptData> scripts = new ArrayDeque<ScriptData>(ids.size());
    Map<Long, ScriptImpl> result = new HashMap<Long, ScriptImpl>(ids.size());
    synchronized (scriptIdToData) {
      for (Long id : ids) {
        ScriptData data = scriptIdToData.get(id);
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
    Destructable operationDestructable = DestructableWrapper.callbackAsDestructable(syncCallback);

    loadNextScript(scripts, result, callback, operationDestructable);
  }

  interface ScriptSourceLoadCallback {
    void done(Map<Long, ScriptImpl> loadedScripts);
  }

  private void loadNextScript(final Queue<ScriptData> scripts,
      final Map<Long, ScriptImpl> result, final ScriptSourceLoadCallback callback,
      final Destructable operationDestructable) {
    final ScriptData data = scripts.poll();
    if (data == null) {
      // Terminate the chain of asynchronous loads and pass a result to the callback.
      try {
        if (callback != null) {
          callback.done(result);
        }
      } finally {
        operationDestructable.destruct();
      }
      return;
    }

    // Create a guard for the case that we fail before issuing next #loadNextScript() call.
    final DestructingGuard requestGuard = new DestructingGuard();

    AsyncFuture.Callback<Boolean> futureCallback = new AsyncFuture.Callback<Boolean>() {
      @Override
      public void done(Boolean res) {
        loadNextScript(scripts, result, callback, operationDestructable);
        // We successfully relayed responsibility for operationDestructable to next async call,
        // discharge guard.
        requestGuard.discharge();
      }
    };

    requestGuard.addValue(operationDestructable);
    // The async operation will call a guard even if something failed within the AsyncFuture.
    data.sourceLoadedFuture.getAsync(futureCallback,
        DestructableWrapper.guardAsCallback(requestGuard));
  }

  public void pageReloaded() {
    synchronized (scriptIdToData) {
      scriptIdToData.clear();
    }
  }
}
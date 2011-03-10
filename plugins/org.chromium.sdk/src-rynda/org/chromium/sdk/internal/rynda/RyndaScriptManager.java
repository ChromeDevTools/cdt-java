// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

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
import org.chromium.sdk.internal.rynda.protocol.RyndaProtocol;
import org.chromium.sdk.internal.rynda.protocol.input.ParsedScriptSourceData;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse;
import org.chromium.sdk.internal.rynda.protocol.input.ScriptSourceData;
import org.chromium.sdk.internal.rynda.protocol.output.GetScriptSource;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Callback;
import org.chromium.sdk.util.AsyncFutureRef;
import org.chromium.sdk.util.Destructable;
import org.chromium.sdk.util.DestructingGuard;

/**
 * Keeps all current scripts for the debug session and handles script source loading.
 */
class RyndaScriptManager {
  private final RyndaTabImpl ryndaTabImpl;
  private final Map<Long, ScriptData> scriptIdToData = new HashMap<Long, ScriptData>();

  RyndaScriptManager(RyndaTabImpl ryndaTabImpl) {
    this.ryndaTabImpl = ryndaTabImpl;
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
    final long sourceID = RyndaProtocol.parseSourceId(data.sourceID());

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
              ryndaTabImpl.getTabListener().getDebugEventListener().scriptLoaded(script);
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
      RyndaCommandCallback commandCallback = new RyndaCommandCallback.Default() {
        @Override
        protected void onSuccess(RyndaCommandResponse.Success success) {
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
      ryndaTabImpl.getCommandProcessor().send(getScriptRequest, commandCallback, syncCallback);
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
    Destructable destructable = DestructableWrapper.callbackAsDestructable(syncCallback);

    DestructingGuard guard = new DestructingGuard();
    guard.addValue(destructable);
    try {
      loadNextScript(scripts, result, callback, destructable);
      guard.discharge();
    } finally {
      guard.doFinally();
    }
  }

  interface ScriptSourceLoadCallback {
    void done(Map<Long, ScriptImpl> loadedScripts);
  }

  private void loadNextScript(final Queue<ScriptData> scripts,
      final Map<Long, ScriptImpl> result, final ScriptSourceLoadCallback callback,
      final Destructable destructable) {
    final ScriptData data = scripts.poll();
    if (data == null) {
      // Terminate the chain of asynchronous loads and pass a result to the callback.
      ryndaTabImpl.getCommandProcessor().runInDispatchThread(new Runnable() {
        @Override
        public void run() {
          try {
            if (callback != null) {
              callback.done(result);
            }
          } finally {
            destructable.destruct();
          }
        }
      });
      return;
    }

    // Create a guard for the case that we fail before issuing next #waitForTheNextScript()
    // call.
    final DestructingGuard requestGuard = new DestructingGuard();
    requestGuard.addValue(destructable);

    AsyncFuture.Callback<Boolean> futureCallback = new AsyncFuture.Callback<Boolean>() {
      @Override
      public void done(Boolean res) {
        loadNextScript(scripts, result, callback, destructable);
        requestGuard.discharge();
      }
    };

    // The async operation will call a guard even if something failed within the AsyncFuture.
    SyncCallback chainedSyncCallback = DestructableWrapper.guardAsCallback(requestGuard);

    data.sourceLoadedFuture.getAsync(futureCallback, chainedSyncCallback);
  }

  public void pageReloaded() {
    synchronized (scriptIdToData) {
      scriptIdToData.clear();
    }
  }
}
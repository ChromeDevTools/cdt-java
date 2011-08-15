// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.Collections;
import java.util.List;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptBase;
import org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue;
import org.chromium.sdk.internal.wip.protocol.input.debugger.EditScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.output.debugger.EditScriptSourceParams;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * Wip implementation of {@link Script}.
 */
class WipScriptImpl extends ScriptBase<String> {
  private final WipScriptManager scriptManager;

  WipScriptImpl(WipScriptManager scriptManager, Descriptor<String> descriptor) {
    super(descriptor);
    this.scriptManager = scriptManager;
  }

  @Override
  public RelayOk setSourceOnRemote(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    return sendLiveEditRequest(newSource, false, callback, syncCallback);
  }

  @Override
  public RelayOk previewSetSource(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    return sendLiveEditRequest(newSource, true, callback, syncCallback);
  }

  private RelayOk sendLiveEditRequest(String newSource, final boolean preview,
      final UpdateCallback updateCallback,
      final SyncCallback syncCallback) {

    if (preview) {
      dispatchResult(null, updateCallback);
      return RelaySyncCallback.finish(syncCallback);
    }

    RelaySyncCallback relay = new RelaySyncCallback(syncCallback);
    final RelaySyncCallback.Guard guard = relay.newGuard();

    EditScriptSourceParams params = new EditScriptSourceParams(getId(), newSource);

    GenericCallback<EditScriptSourceData> commandCallback =
        new GenericCallback<EditScriptSourceData>() {
      @Override
      public void success(EditScriptSourceData value) {
        RelayOk relayOk =
            possiblyUpdateCallFrames(preview, value, updateCallback, guard.getRelay());
        guard.discharge(relayOk);
      }

      @Override
      public void failure(Exception exception) {
        updateCallback.failure(exception.getMessage());
      }
    };

    WipCommandProcessor commandProcessor = scriptManager.getTabImpl().getCommandProcessor();
    return commandProcessor.send(params, commandCallback, guard.asSyncCallback());
  }

  private RelayOk possiblyUpdateCallFrames(boolean preview, final EditScriptSourceData data,
      final UpdateCallback updateCallback, RelaySyncCallback relay) {

    // TODO: support 'step-in recommended'.

    List<CallFrameValue> callFrames = null;
    if (!preview) {
      callFrames = data.callFrames();
    }
    if (callFrames == null) {
      dispatchResult(null, updateCallback);
      return relay.finish();
    } else {
      GenericCallback<Void> setFramesCallback =
          new GenericCallback<Void>() {
        @Override public void success(Void value) {
          dispatchResult(null, updateCallback);
        }
        @Override public void failure(Exception exception) {
          throw new RuntimeException(exception);
        }
      };
      WipContextBuilder contextBuilder = scriptManager.getTabImpl().getContextBuilder();
      return contextBuilder.updateStackTrace(callFrames, setFramesCallback,
          relay.getUserSyncCallback());
    }
  }

  private void dispatchResult(Void result, UpdateCallback updateCallback) {
    if (updateCallback != null) {
      // This is a fake because protocol doesn't support it.
      ChangeDescription fakeChangeDescription = new ChangeDescription() {
        private final FunctionPositions nullPositions = new FunctionPositions() {
          @Override public long getStart() {
            return 0;
          }
          @Override public long getEnd() {
            return 0;
          }
        };

        @Override
        public boolean isStackModified() {
          return false;
        }

        @Override
        public TextualDiff getTextualDiff() {
          return new TextualDiff() {
            @Override public List<Long> getChunks() {
              return Collections.emptyList();
            }
          };
        }

        @Override public String getCreatedScriptName() {
          return null;
        }

        @Override
        public OldFunctionNode getChangeTree() {
          return new OldFunctionNode() {
            @Override public String getName() {
              return "Change descrption is not supported yet";
            }
            @Override
            public FunctionPositions getPositions() {
              return nullPositions;
            }
            @Override public List<? extends OldFunctionNode> children() {
             return Collections.emptyList();
            }
            @Override public OldFunctionNode asOldFunction() {
              return this;
            }
            @Override public ChangeStatus getStatus() {
              return ChangeStatus.UNCHANGED;
            }
            @Override public String getStatusExplanation() {
              return "Change descrption is not supported yet";
            }
            @Override public FunctionPositions getNewPositions() {
              return nullPositions;
            }
            @Override public List<? extends NewFunctionNode> newChildren() {
              return Collections.emptyList();
            }
          };
        }
      };
      updateCallback.success(null, null);
    }
  }
}

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.List;

import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.ScriptBase;
import org.chromium.sdk.internal.liveeditprotocol.LiveEditProtocolParserAccess;
import org.chromium.sdk.internal.liveeditprotocol.LiveEditResult;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.wip.protocol.WipProtocol;
import org.chromium.sdk.internal.wip.protocol.input.debugger.CallFrameValue;
import org.chromium.sdk.internal.wip.protocol.input.debugger.SetScriptSourceData;
import org.chromium.sdk.internal.wip.protocol.output.debugger.SetScriptSourceParams;
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
  public long getId() {
    return WipScriptManager.parseStringSourceId(getIdImpl());
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

    RelaySyncCallback relay = new RelaySyncCallback(syncCallback);
    final RelaySyncCallback.Guard guard = relay.newGuard();

    SetScriptSourceParams params = new SetScriptSourceParams(getIdImpl(), newSource, preview);

    JavascriptVm.GenericCallback<SetScriptSourceData> commandCallback =
        new JavascriptVm.GenericCallback<SetScriptSourceData>() {
      @Override
      public void success(SetScriptSourceData value) {
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

  private RelayOk possiblyUpdateCallFrames(boolean preview, final SetScriptSourceData data,
      final UpdateCallback updateCallback, RelaySyncCallback relay) {

    // TODO: support 'step-in recommended'.

    List<CallFrameValue> callFrames = null;
    if (!preview) {
      callFrames = data.callFrames();
    }
    if (callFrames == null) {
      dispatchResult(data.result(), updateCallback);
      return relay.finish();
    } else {
      JavascriptVm.GenericCallback<Void> setFramesCallback =
          new JavascriptVm.GenericCallback<Void>() {
        @Override public void success(Void value) {
          dispatchResult(data.result(), updateCallback);
        }
        @Override public void failure(Exception exception) {
          throw new RuntimeException(exception);
        }
      };
      WipContextBuilder contextBuilder = scriptManager.getTabImpl().getContextBuilder();
      return contextBuilder.updateStackTrace(callFrames, setFramesCallback,
          relay.getSyncCallback());
    }
  }

  private void dispatchResult(SetScriptSourceData.Result result, UpdateCallback updateCallback) {
    if (updateCallback != null) {
      LiveEditResult liveEditResult;
      try {
        liveEditResult = LiveEditProtocolParserAccess.get().parse(result.getUnderlyingObject(),
            LiveEditResult.class);
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException("Failed to parse LiveEdit response", e);
      }
      ChangeDescription wrappedChangeDescription =
          UpdateResultParser.wrapChangeDescription(liveEditResult);
      updateCallback.success(null, wrappedChangeDescription);
    }
  }
}

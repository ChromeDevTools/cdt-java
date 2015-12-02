// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse;
import org.chromium.sdk.internal.wip.protocol.output.runtime.ReleaseObjectGroupParams;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.wip.PermanentRemoteValueMapping;

class PermanentRemoteValueMappingImpl extends WipValueLoader
    implements PermanentRemoteValueMapping {
  private final String id;

  PermanentRemoteValueMappingImpl(WipTabImpl tabImpl, String id) {
    super(tabImpl);
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public RelayOk delete(final GenericCallback<Void> callback, SyncCallback syncCallback) {
    ReleaseObjectGroupParams params = new ReleaseObjectGroupParams(id);
    WipCommandCallback callbackWrapper;
    if (callback == null) {
      callbackWrapper = null;
    } else {
      callbackWrapper = new WipCommandCallback() {
        @Override
        public void messageReceived(WipCommandResponse response) {
          callback.success(null);
        }

        @Override
        public void failure(String message) {
          callback.failure(new Exception(message));
        }
      };
    }
    return getTabImpl().getCommandProcessor().send(params, callbackWrapper, syncCallback);
  }

  @Override
  String getObjectGroupId() {
    return id;
  }

  @Override
  public JsEvaluateContext getEvaluateContext() {
    return new WipContextBuilder.GlobalEvaluateContext(this);
  }
}

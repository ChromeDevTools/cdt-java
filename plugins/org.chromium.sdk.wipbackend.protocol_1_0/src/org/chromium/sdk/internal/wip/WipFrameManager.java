// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.wip;

import org.chromium.sdk.internal.wip.protocol.input.page.FrameNavigatedEventData;
import org.chromium.sdk.internal.wip.protocol.input.page.FrameValue;
import org.chromium.sdk.internal.wip.protocol.input.page.GetResourceTreeData;
import org.chromium.sdk.internal.wip.protocol.output.page.GetResourceTreeParams;
import org.chromium.sdk.util.GenericCallback;

/**
 * Collects information about frame tree. At first class only watches for the url of root frame.
 */
class WipFrameManager {
  private final WipTabImpl tabImpl;
  private boolean urlUnknown = true;

  WipFrameManager(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  void readFrames() {
    GetResourceTreeParams requestParams = new GetResourceTreeParams();
    GenericCallback<GetResourceTreeData> callback =
        new GenericCallback<GetResourceTreeData>() {
          @Override
          public void success(GetResourceTreeData value) {
            FrameValue frame = value.frameTree().frame();
            if (frame.parentId() != null) {
              throw new RuntimeException("Unexpected parentId value");
            }
            String url = frame.url();
            boolean silentUpdate = urlUnknown;
            tabImpl.updateUrl(url, silentUpdate);
            urlUnknown = false;
          }

          @Override public void failure(Exception exception) {
            throw new RuntimeException("Failed to read frame data", exception);
          }
        };

    tabImpl.getCommandProcessor().send(requestParams, callback, null);
  }

  void frameNavigated(FrameNavigatedEventData eventData) {
    FrameValue frame = eventData.frame();
    String parentId = frame.parentId();
    if (parentId == null) {
      String newUrl = frame.url();
      tabImpl.updateUrl(newUrl, false);
      urlUnknown = false;
    }
  }
}

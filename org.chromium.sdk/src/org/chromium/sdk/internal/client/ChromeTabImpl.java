// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.client;

import org.chromium.sdk.client.ChromeTab;
import org.chromium.sdk.client.DebugEventListener;
import org.chromium.sdk.client.V8Debugger;
import org.chromium.sdk.tools.v8.mirror.JsDebugContext;

/**
 * A default implementation of the ChromeTab interface.
 */
public class ChromeTabImpl implements ChromeTab {

  private final int tabId;
  private final String url;
  private final ChromeImpl chromeImpl;
  private JsDebugContext context;
  private V8DebuggerImpl v8Debugger;
  private DebugEventListener debugEventListener;

  public ChromeTabImpl(int tabId, String url, ChromeImpl chromeImpl) {
    this.tabId = tabId;
    this.url = url;
    this.chromeImpl = chromeImpl;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public int getId() {
    return tabId;
  }

  @Override
  public V8Debugger getV8Debugger() {
    if (v8Debugger == null) {
      this.v8Debugger = new V8DebuggerImpl(getDebugContext().getV8Handler());
    }
    return v8Debugger;
  }

  @Override
  public JsDebugContext getDebugContext() {
    if (this.context == null) {
      this.context = new JsDebugContext(this);
    }
    return context;
  }

  public DebugEventListener getDebugEventListener() {
    return debugEventListener;
  }

  @Override
  public ChromeImpl getChrome() {
    return chromeImpl;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof ChromeTabImpl)) {
      return false;
    }
    ChromeTabImpl that = (ChromeTabImpl) o;
    return this.tabId == that.tabId &&
        eq(this.url, that.url) &&
        eq(this.chromeImpl, that.chromeImpl);
  }

  @Override
  public int hashCode() {
    return url.hashCode() + chromeImpl.hashCode() + tabId;
  }

  private static boolean eq(Object left, Object right) {
    return left == right || (left != null && left.equals(right));
  }

  @Override
  public void sessionTerminated() {
    chromeImpl.sessionTerminated(this.tabId);
  }
}

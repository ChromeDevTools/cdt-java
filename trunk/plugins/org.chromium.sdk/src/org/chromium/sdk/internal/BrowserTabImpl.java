// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.internal.tools.v8.V8DebuggerToolHandler.AttachmentFailureException;
import org.json.simple.JSONObject;

/**
 * A default, thread-safe implementation of the BrowserTab interface.
 */
public class BrowserTabImpl implements BrowserTab {

  /** Tab ID as reported by the DevTools server. */
  private final int tabId;

  /** The primary tab URL. */
  private final String url;

  /** The host BrowserImpl instance. */
  private final BrowserImpl browserImpl;

  /** The debug context instance for this tab. */
  private final DebugContextImpl context;

  /** The listener to report debug events to. */
  private DebugEventListener debugEventListener;

  public BrowserTabImpl(int tabId, String url, BrowserImpl browserImpl) {
    this.tabId = tabId;
    this.url = url;
    this.browserImpl = browserImpl;
    this.context = new DebugContextImpl(this);
  }

  public String getUrl() {
    return url;
  }

  public int getId() {
    return tabId;
  }

  public DebugContextImpl getDebugContext() {
    return context;
  }

  public synchronized DebugEventListener getDebugEventListener() {
    return debugEventListener;
  }

  public BrowserImpl getBrowser() {
    return browserImpl;
  }

  public synchronized boolean attach(DebugEventListener listener) {
    Result result = null;
    try {
      result = getDebugContext().getV8Handler().attachToTab();
    } catch (AttachmentFailureException e) {
      // fall through and return false
    }
    this.debugEventListener = listener;
    return Result.OK == result;
  }

  public boolean detach() {
    final Result result = getDebugContext().getV8Handler().detachFromTab();
    return Result.OK == result;
  }

  public boolean isAttached() {
    return context.getV8Handler().isAttached();
  }

  public void getScripts(final ScriptsCallback callback) {
    context.loadAllScripts(callback);
  }

  public void setBreakpoint(Breakpoint.Type type, String target, int line,
      int position, boolean enabled, String condition, int ignoreCount,
      final BreakpointCallback callback) {
    context.getV8Handler().getV8CommandProcessor().getBreakpointProcessor()
        .setBreakpoint(type, target, line, position, enabled, condition, ignoreCount, callback);  
  }

  public void sessionTerminated() {
    browserImpl.sessionTerminated(this.tabId);
  }

}

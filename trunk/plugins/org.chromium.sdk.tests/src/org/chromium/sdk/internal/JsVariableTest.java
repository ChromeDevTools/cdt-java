// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashSet;
import java.util.Set;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.internal.transport.ChromeStub;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.junit.Before;
import org.junit.Test;

/**
 * A test for the JsVariable implementor.
 */
public class JsVariableTest implements DebugEventListener {

  private ChromeStub messageResponder;
  private JsStackFrameImpl stackFrame;

  private ValueMirror eventMirror;

  @Before
  public void setUpBefore() throws Exception {
    this.messageResponder = new FixtureChromeStub();
    Browser browser = ((BrowserFactoryImpl) BrowserFactory.getInstance())
        .create(new FakeConnection(messageResponder));
    browser.connect();
    BrowserTab[] tabs = browser.getTabs();
    BrowserTab browserTab = tabs[0];
    browserTab.attach(this);
    eventMirror = new ValueMirror(
        "event", 11, new ValueMirror.PropertyReference[] {
            ValueMirror.newPropertyReference(FixtureChromeStub.getNumber3Ref(), "x"),
            ValueMirror.newPropertyReference(FixtureChromeStub.getNumber3Ref(), "y"),
        }, null);
    FrameMirror frameMirror = new FrameMirror(
        ((BrowserTabImpl) browserTab).getDebugContext(),
        null,
        "fooscript", 12, FixtureChromeStub.getScriptId(),
        "foofunction");
    this.stackFrame = new JsStackFrameImpl(
        frameMirror, 0, ((BrowserTabImpl) browserTab).getDebugContext());
  }

  @Test
  public void testEnsureProperties() throws Exception {
    JsVariableImpl var = new JsVariableImpl(stackFrame, eventMirror);
    var.ensureProperties(eventMirror.getProperties());
    JsValueImpl value = var.getValue();
    assertNotNull(value.asObject());
    JsVariableImpl[] variables = value.asObject().getProperties();
    assertEquals(2, variables.length); // "x" and "y"
    JsVariableImpl firstVar = variables[0];
    JsVariableImpl secondVar = variables[1];
    Set<String> names = new HashSet<String>();
    names.add("x"); //$NON-NLS-1$
    names.add("y"); //$NON-NLS-1$

    names.remove(firstVar.getName());
    names.remove(secondVar.getName());
    assertEquals(0, names.size());

    JsValueImpl firstVal = firstVar.getValue();
    JsValueImpl secondVal = firstVar.getValue();
    assertEquals("3", firstVal.getValueString()); //$NON-NLS-1$
    assertEquals("3", secondVal.getValueString()); //$NON-NLS-1$
  }

  public void closed() {
  }

  public void disconnected() {
  }

  public void navigated(String newUrl) {
  }

  public void resumed() {
  }

  public void suspended(DebugContext context) {
  }
}

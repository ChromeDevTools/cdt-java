// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.SortedMap;

import org.chromium.sdk.Browser;
import org.chromium.sdk.BrowserFactory;
import org.chromium.sdk.BrowserTab;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.Script;
import org.chromium.sdk.internal.transport.ChromeStub;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;

/**
 * A test for the JsVariable implementor.
 */
public class JsArrayImplTest implements DebugEventListener {

  private ChromeStub messageResponder;
  private CallFrameImpl callFrame;

  private ValueMirror arrayMirror;

  @Before
  public void setUpBefore() throws Exception {
    this.messageResponder = new FixtureChromeStub();
    Browser browser = ((BrowserFactoryImpl) BrowserFactory.getInstance())
        .create(new FakeConnection(messageResponder));
    browser.connect();
    BrowserTab[] tabs = browser.getTabs();
    BrowserTab browserTab = tabs[0];
    browserTab.attach(this);
    JSONObject valueObject = (JSONObject) JSONValue.parse(
        "{\"handle\":" + FixtureChromeStub.getNumber3Ref() +
        ",\"type\":\"number\",\"value\":3,\"text\":\"3\"}");
    arrayMirror = new ValueMirror(
        "array", 11, new ValueMirror.PropertyReference[] {
            ValueMirror.newPropertyReference(FixtureChromeStub.getNumber3Ref(), "[1]", valueObject),
            ValueMirror.newPropertyReference(FixtureChromeStub.getNumber3Ref(), "[3]", valueObject),
        }, null);
    DebugContextImpl debugContext = ((BrowserTabImpl) browserTab).getDebugContext();
    FrameMirror frameMirror = new FrameMirror(
        debugContext,
        null,
        "fooscript", 12, FixtureChromeStub.getScriptId(),
        "foofunction");
    this.callFrame = new CallFrameImpl(frameMirror, 0, debugContext, debugContext.getToken());
  }

  @Test
  public void testArrayData() throws Exception {
    JsArrayImpl jsArray = new JsArrayImpl(callFrame, "", arrayMirror);
    assertNotNull(jsArray.asArray());
    Collection<JsVariableImpl> properties = jsArray.getProperties();
    assertEquals(2, properties.size());
    assertEquals(4, jsArray.length());
    SortedMap<Integer, ? extends JsVariable> sparseArray = jsArray.toSparseArray();
    assertEquals(2, sparseArray.size());
    JsVariable firstElement = sparseArray.get(1);
    JsVariable thirdElement = sparseArray.get(3);
    assertNull(jsArray.get(-1));
    assertNull(jsArray.get(0));
    assertEquals(firstElement, jsArray.get(1));
    assertNull(jsArray.get(2));
    assertEquals(thirdElement, jsArray.get(3));
    assertNull(jsArray.get(10));
    checkElementData(firstElement);
    checkElementData(thirdElement);
  }

  private static void checkElementData(JsVariable arrayElement) {
    assertNotNull(arrayElement);
    JsValue value = arrayElement.getValue();
    assertEquals(JsValue.Type.TYPE_NUMBER, value.getType());
    assertEquals("3", value.getValueString());
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

  public void scriptLoaded(Script newScript) {
  }
}

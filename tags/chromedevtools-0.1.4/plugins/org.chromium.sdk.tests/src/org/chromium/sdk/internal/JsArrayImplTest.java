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
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.protocol.data.ValueHandle;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.transport.ChromeStub;
import org.chromium.sdk.internal.transport.FakeConnectionFactory;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;

/**
 * A test for the JsVariable implementor.
 */
public class JsArrayImplTest {

  private ChromeStub messageResponder;
  private CallFrameImpl callFrame;

  private ValueMirror arrayMirror;

  private final StubListener listener = new StubListener();

  @Before
  public void setUpBefore() throws Exception {
    this.messageResponder = new FixtureChromeStub();
    Browser browser = ((BrowserFactoryImpl) BrowserFactory.getInstance())
        .create(new FakeConnectionFactory(messageResponder));
    BrowserTab browserTab = browser.createTabFetcher().getTabs().get(0).attach(listener);

    listener.expectSuspendedEvent();
    messageResponder.sendSuspendedEvent();
    DebugContext debugContext = listener.getDebugContext();

    String propertyRefText = "{'ref':" + FixtureChromeStub.getNumber3Ref() +
        ",'type':'number','value':3,'text':'3'}";

    String valueHandleJsonText = (
            "{'protoObject':{'ref':55516,'className':'Array','type':'object'}," +
            "'text':'#<an Array>'," +
            "'handle':5559,'" +
            "constructorFunction':{'ref':55515,'inferredName':''," +
            "'name':'Array','type':'function'}," +
            "'prototypeObject':{'ref':5553,'type':'undefined'}," +
            "'className':'Array','properties':[{'name':'length'," +
            "'value':{'ref':55517,'value':2,'type':'number'}}," +
            "{'name':1,'value':" + propertyRefText + "}," +
            "{'name':3,'value':"+ propertyRefText +"}],'type':'object'}"
        ).replace('\'', '"');
    JSONObject valueHandleJson = (JSONObject) JSONValue.parse(valueHandleJsonText);
    ValueHandle valueHandle =
        V8ProtocolUtil.getV8Parser().parse(valueHandleJson, ValueHandle.class);

    arrayMirror = V8Helper.createMirrorFromLookup(valueHandle).getValueMirror();

    InternalContext internalContext = ContextBuilder.getInternalContextForTests(debugContext);

    FrameMirror frameMirror = new FrameMirror(
        null,
        "fooscript", 12, FixtureChromeStub.getScriptId(),
        "foofunction");
    this.callFrame = new CallFrameImpl(frameMirror, 0, internalContext);
  }

  @Test
  public void testArrayData() throws Exception {
    JsArrayImpl jsArray = new JsArrayImpl(callFrame, "", arrayMirror);
    assertNotNull(jsArray.asArray());
    Collection<JsVariableImpl> properties = jsArray.getProperties();
    assertEquals(2 + 1, properties.size()); // 2 array element properties and one length property.
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
}

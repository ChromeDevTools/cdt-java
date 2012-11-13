// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import static org.chromium.sdk.tests.internal.JsonBuilderUtil.jsonObject;
import static org.chromium.sdk.tests.internal.JsonBuilderUtil.jsonProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.StandaloneVm;
import org.chromium.sdk.internal.BrowserFactoryImplTestGate;
import org.chromium.sdk.internal.browserfixture.FixtureChromeStub;
import org.chromium.sdk.internal.browserfixture.StubListener;
import org.chromium.sdk.internal.transport.ChromeStub;
import org.chromium.sdk.internal.transport.FakeConnection;
import org.chromium.sdk.internal.v8native.CallFrameImpl;
import org.chromium.sdk.internal.v8native.ContextBuilder;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.protocol.input.FrameObject;
import org.chromium.sdk.internal.v8native.protocol.input.V8ProtocolParserAccess;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeRef;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.Test;

/**
 * A test for the JsVariable implementor.
 */
public class JsObjectImplTest {

  private ChromeStub messageResponder;
  private CallFrameImpl callFrame;

  private ValueMirror eventMirror;

  private final StubListener listener = new StubListener();

  @Before
  public void setUpBefore() throws Exception {
    this.messageResponder = new FixtureChromeStub();
    StandaloneVm javascriptVm = BrowserFactoryImplTestGate.createStandalone(
        new FakeConnection(messageResponder), FakeConnection.HANDSHAKER);
    javascriptVm.attach(listener);

    listener.expectSuspendedEvent();
    messageResponder.sendSuspendedEvent();
    DebugContext debugContext = listener.getDebugContext();

    JSONObject valueObject = (JSONObject) JSONValue.parse(
        "{\"ref\":" + FixtureChromeStub.getNumber3Ref() +
        ",\"type\":\"number\",\"value\":3,\"text\":\"3\"}");
    SomeRef someRef = V8ProtocolParserAccess.get().parseSomeRef(valueObject);
    DataWithRef dataWithRef = DataWithRef.fromSomeRef(someRef);
    SubpropertiesMirror.ListBased subpropertiesMirror = new SubpropertiesMirror.ListBased(
        new PropertyReference("x", dataWithRef),
        new PropertyReference("y", dataWithRef)
    );
    InternalContext internalContext = ContextBuilder.getInternalContextForTests(debugContext);
    eventMirror = internalContext.getValueLoader().addDataToMap(Long.valueOf(11),
        Type.TYPE_OBJECT, null, null,
        subpropertiesMirror);

    FrameObject frameObject;
    {
      JSONObject jsonObject = jsonObject(
          jsonProperty("line", 12L),
          jsonProperty("index", 0L),
          jsonProperty("sourceLineText", ""),
          jsonProperty("script",
              jsonObject(
                  jsonProperty("ref", Long.valueOf(FixtureChromeStub.getScriptId()))
              )
          ),
          jsonProperty("func",
              jsonObject(
                  jsonProperty("name", "foofunction")
              )
          )
      );
      frameObject = V8ProtocolParserAccess.get().parseFrameObject(jsonObject);
    }

    this.callFrame = new CallFrameImpl(frameObject, internalContext);
  }

  @Test
  public void testObjectData() throws Exception {
    JsObjectBase<?> jsObject = new JsObjectBase.Impl(
        callFrame.getInternalContext().getValueLoader(), "test_object", eventMirror);
    assertNotNull(jsObject.asObject());
    assertNull(jsObject.asArray());
    Collection<JsVariableImpl> variables = jsObject.getProperties();
    assertEquals(2, variables.size()); // "x" and "y"
    Iterator<JsVariableImpl> it = variables.iterator();
    JsVariableImpl firstVar = it.next();
    JsVariableImpl secondVar = it.next();
    Set<String> names = new HashSet<String>();
    names.add("x"); //$NON-NLS-1$
    names.add("y"); //$NON-NLS-1$

    names.remove(firstVar.getName());
    names.remove(secondVar.getName());
    assertEquals(0, names.size());

    JsValueBase firstVal = firstVar.getValue();
    JsValueBase secondVal = firstVar.getValue();
    assertEquals("3", firstVal.getValueString()); //$NON-NLS-1$
    assertEquals("3", secondVal.getValueString()); //$NON-NLS-1$
    assertNull(firstVal.asObject());
    assertNull(secondVal.asObject());

    JsVariable xProperty = jsObject.getProperty("x");
    assertEquals("x", xProperty.getName()); //$NON-NLS-1$
    assertEquals("test_object.x", xProperty.getFullyQualifiedName()); //$NON-NLS-1$
    JsVariable yProperty = jsObject.getProperty("y");
    assertEquals("y", yProperty.getName()); //$NON-NLS-1$
    assertEquals("test_object.y", yProperty.getFullyQualifiedName()); //$NON-NLS-1$
  }
}

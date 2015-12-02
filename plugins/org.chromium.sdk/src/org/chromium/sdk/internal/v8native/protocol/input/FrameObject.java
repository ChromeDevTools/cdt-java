// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.FieldLoadStrategy;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.v8native.protocol.input.data.PropertyObject;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeRef;
import org.json.simple.JSONObject;

/**
 * A frame mirror object type. Technically it is almost subtype of {@link SomeHandle}:
 * it gets serialized from the same code; however, it never gets handle field so
 * we have to treat it as a separate type. Hopefully frame object will never
 * get mixed with other objects on remote side.
 */
@JsonType
public interface FrameObject {

  long index();

  JSONObject func();

  String text();

  long line();

  String sourceLineText();

  @JsonOptionalField
  SomeRef script();

  @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
  List<PropertyObject> arguments();

  @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
  List<PropertyObject> locals();

  @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
  SomeRef receiver();

  @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
  List<ScopeRef> scopes();

  Boolean constructCall();

  String type();

  Long position();

  Long column();

  Boolean debuggerFrame();
}

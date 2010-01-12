// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import java.util.List;

import org.chromium.sdk.internal.protocol.data.PropertyObject;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocol.data.SomeRef;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.json.simple.JSONObject;

/**
 * A frame mirror object type. Technically it is almost subtype of {@link SomeHandle}:
 * it gets serialized from the same code; however, it never gets handle field so
 * we have to treat it as a separate type. Hopefully frame object will never
 * get mixed with other objects on remote side.
 */
@JsonType
public interface FrameObject {

  long getIndex();

  JSONObject getFunc();

  String getText();

  long getLine();

  String getSourceLineText();

  @JsonOptionalField
  SomeRef getScript();

  List<PropertyObject> getArguments();

  List<PropertyObject> getLocals();

  SomeRef getReceiver();

  List<ScopeRef> getScopes();

  Boolean constructCall();

  String type();

  Long position();

  Long column();

  Boolean debuggerFrame();

}

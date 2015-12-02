// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input.data;

import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.json.simple.JSONObject;

@JsonType
public interface ScriptHandle extends JsonSubtype<SomeHandle> {

  long id();
  long lineOffset();
  long columnOffset();
  long lineCount();

  @JsonOptionalField
  Object data();

  // either sourceStart or source
  @JsonOptionalField
  String sourceStart();

  @JsonOptionalField
  String source();

  long sourceLength();
  long scriptType();
  long compilationType();


  @JsonOptionalField
  SomeSerialized evalFromScript();

  @JsonOptionalField
  JSONObject evalFromLocation();


  @JsonOptionalField
  SomeRef context();

  String text();

  @JsonOptionalField
  String name();
}

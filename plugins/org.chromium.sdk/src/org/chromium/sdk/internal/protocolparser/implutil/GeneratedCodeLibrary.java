// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.implutil;

import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.json.simple.JSONObject;

/**
 * A small library of classes and methods used from generated parser code.
 */
public class GeneratedCodeLibrary {
  public abstract static class AbstractType {
    public abstract Object parseJson(JSONObject json) throws JsonProtocolParseException;
    public abstract Object parseAnything(Object object) throws JsonProtocolParseException;
  }

  public static class JsonValueBase {
    protected final JSONObject underlying;

    protected JsonValueBase(Object underlying) throws JsonProtocolParseException {
      if (underlying instanceof JSONObject == false) {
        throw new JsonProtocolParseException("JSON object input expected");
      }
      this.underlying = (JSONObject) underlying;
    }
  }

  public static class ObjectValueBase {
    protected final Object underlying;

    protected ObjectValueBase (Object underlying) {
      this.underlying = underlying;
    }
  }
}

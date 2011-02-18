// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8.request;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

/**
 * Represents a generic JSONStreamAware V8 request message (so that it can
 * serialize itself into JSON.)
 */
public class DebuggerMessage implements JSONStreamAware {

  private final int sequence;

  private final String command;

  private final Map<String, Object> arguments = new HashMap<String, Object>();


  public DebuggerMessage(String command) {
    this.sequence = SeqGenerator.getInstance().next();
    this.command = command;
  }

  public Integer getSeq() {
    return sequence;
  }

  public String getType() {
    return V8MessageType.REQUEST.value;
  }

  public String getCommand() {
    return command;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  protected final void putArgument(String key, Object object) {
    if (object != null) {
      arguments.put(key, object);
    }
  }

  protected final void putNullableArgument(String key, Object object) {
    arguments.put(key, object);
  }

  private final void putArgumentString(String key, Object object) {
    arguments.put(key, object.toString());
  }

  protected final void putArgumentStringIfNotNull(String key, Object object) {
    if (object != null) {
      putArgumentString(key, object);
    }
  }

  public void writeJSONString(Writer out) throws IOException {
    LinkedHashMap<String, Object> obj = new LinkedHashMap<String, Object>();
    obj.put("seq", sequence);
    obj.put("type", V8MessageType.REQUEST.value);
    obj.put("command", command);
    if (!arguments.isEmpty()) {
      obj.put("arguments", arguments);
    }
    JSONValue.writeJSONString(obj, out);
  }
}

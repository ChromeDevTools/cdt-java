// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.protocol.output;

import java.util.List;
import java.util.Map;

import org.chromium.sdk.internal.v8native.DebuggerCommand;
import org.chromium.sdk.internal.v8native.value.JsDataTypeUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Represents an "evaluate" V8 request message.
 */
public class EvaluateMessage extends DebuggerMessage {

  /**
   * @param expression to evaluate
   * @param frame number (top is 0).
   * @param global nullable. Default is false
   * @param disableBreak nullable. Default is true
   * @param additionalContext nullable
   */
  public EvaluateMessage(String expression, Integer frame, Boolean global, Boolean disableBreak,
      List<Map.Entry<String, EvaluateMessage.Value>> additionalContext) {
    super(DebuggerCommand.EVALUATE.value);
    putArgument("expression", expression);
    if (frame != null) {
      putArgument("frame", frame);
    }
    putArgument("global", global);
    putArgument("disable_break", disableBreak);
    putArgument("inlineRefs", Boolean.TRUE);
    if (additionalContext != null) {
      JSONArray contextParam = new JSONArray();
      for (Map.Entry<String, EvaluateMessage.Value> en : additionalContext) {
        JSONObject mapping = en.getValue().createJsonParameter();
        mapping.put("name", en.getKey());
        contextParam.add(mapping);
      }
      putArgument("additional_context", contextParam);
    }
  }


  public static abstract class Value {
    public static Value createForType(final Type type) {
      return new Value() {
        @Override JSONObject createJsonParameter() {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("type", type.jsonName);
          return jsonObject;
        }
      };
    }
    public static Value createForValue(final Object value) {
      return new Value() {
        @Override JSONObject createJsonParameter() {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("value", value);
          return jsonObject;
        }
      };
    }
    public static Value createForStringDescription(final Type type, final String description) {
      return new Value() {
        @Override JSONObject createJsonParameter() {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("type", type.jsonName);
          jsonObject.put("stringDescription", description);
          return jsonObject;
        }
      };
    }
    public static Value createForId(final long id) {
      return new Value() {
        @Override JSONObject createJsonParameter() {
          JSONObject jsonObject = new JSONObject();
          jsonObject.put("handle", id);
          return jsonObject;
        }
      };
    }

    abstract JSONObject createJsonParameter();

    public enum Type {
      NULL(JsDataTypeUtil.JSON_NULL_TYPE),
      UNDEFINED(JsDataTypeUtil.JSON_UNDEFINED_TYPE),
      BOOLEAN(JsDataTypeUtil.JSON_BOOLEAN_TYPE),
      NUMBER(JsDataTypeUtil.JSON_NUMBER_TYPE),
      STRING(JsDataTypeUtil.JSON_STRING_TYPE);

      final String jsonName;

      private Type(String jsonName) {
        this.jsonName = jsonName;
      }
    }
  }
}

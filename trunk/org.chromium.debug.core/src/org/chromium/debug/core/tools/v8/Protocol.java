// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.util.JsonUtil;
import org.json.simple.JSONObject;

/**
 * JSON events, command and response types and keys for V8 debugging API.
 */
@SuppressWarnings("nls")
public class Protocol {
  // Known Javascript types for V8 VM
  public final static String TYPE_OBJECT = "object";

  public final static String TYPE_NUMBER = "number";

  public final static String TYPE_STRING = "string";

  public final static String TYPE_FUNCTION = "function";

  public final static String TYPE_BOOLEAN = "boolean";

  public final static String TYPE_DATE = "date";

  public final static String TYPE_UNDEFINED = "undefined";

  public final static String TYPE_NULL = "null";

  // An object which is actually an array. This type is not present
  public final static String TYPE_OBJECT_ARRAY = "array";

  public final static String KEY_SEQ = "seq";

  public final static String KEY_REQSEQ = "request_seq";

  public final static String KEY_TYPE = "type";

  public final static String TYPE_EVENT = "event";

  public final static String TYPE_RESPONSE = "response";

  public final static String TYPE_REQUEST = "request";

  public final static String COMMAND_CONTINUE = "continue";

  public final static String COMMAND_EVALUATE = "evaluate";

  public final static String COMMAND_BACKTRACE = "backtrace";

  public final static String COMMAND_FRAME = "frame";

  public final static String COMMAND_SCRIPTS = "scripts";

  public final static String COMMAND_SOURCE = "source";

  public final static String COMMAND_SETBP = "setbreakpoint";

  public final static String KEY_COMMAND = "command";

  public final static String KEY_RESULT = "result";

  public final static String KEY_SUCCESS = "success";

  public final static String KEY_MESSAGE = "message";

  public final static String KEY_DATA = "data";

  public final static String FRAME_BODY = "body";

  public final static String FRAME_RECEIVER = "receiver";

  public final static String BODY_INDEX = "index";

  public final static String BODY_LOCALS = "locals";

  public final static String BODY_ARGUMENTS = "arguments";

  public final static String ARGUMENT_NAME = "name";

  public final static String ARGUMENT_VALUE = "value";

  public final static String LOCAL_NAME = "name";

  public final static String LOCAL_VALUE = "value";

  public final static String VALUE_REF = "ref";

  public final static String FRAME_REFS = "refs";

  // REF handle object contains name, value, type, handle (ref #)
  public final static String REF_HANDLE = "handle";

  public final static String REF_TEXT = "text";

  public final static String REF_TYPE = "type";

  public final static String REF_VALUE = "value"; // Primitive type value

  public final static String REF_LENGTH = "length"; // Valid for String type

  public final static String REF_PROPERTIES = "properties"; // object props

  public final static String REF_PROP_REF = "ref";

  public final static String REF_PROP_NAME = "name";

  public final static String REF_CONSTRUCTORFUNCTION = "constructorFunction";

  public final static String REF_PROTOOBJECT = "protoObject";

  public final static String REF_PROTOTYPEOBJECT = "prototypeObject";

  public final static String REF_CLASSNAME = "className";

  public final static String REF_PROP_TYPE = "propertyType";

  public static final String CLASSNAME_ARRAY = "Array";

  public static final String CLASSNAME_DATE = "Date";

  public final static String BACKTRACE_BODY = "body";

  public final static String BODY_FRAMES = "frames";

  public final static String BODY_FRAME_TEXT = "text";

  public final static String BODY_FRAME_LINE = "line";

  public final static String BODY_FRAME_SRCLINE = "sourceLineText";

  public final static String BODY_FRAME_POSITION = "position";

  public final static String BREAK_BODY = "body";

  public final static String BREAK_BREAKPOINTS = "breakpoints";

  public final static String KEY_EVENT = "event";

  public final static String EVENT_BREAK = "break";

  // Scripts and Source response
  public final static String SOURCE_CODE = "source";

  // Scripts response
  public final static String BODY_SCRIPTS = "body";

  public final static String BODY_NAME = "name"; // script name

  public final static String BODY_LINEOFFSET = "lineOffset";

  public final static String BODY_LINECOUNT = "lineCount";

  // Source response
  public final static String BODY_SOURCE = "body";

  // SetBreakpoint response
  public final static String BODY_SETBP = "body";

  public final static String BODY_TYPE = "type";

  public final static String BODY_BREAKPOINT = "breakpoint";

  // Evaluate response
  public final static String EVAL_BODY = "body";

  public final static String EVAL_REF = "ref";

  // Continue response
  public final static String BODY_RUNNING = "running";

  /**
   * Maps handle "ref" values to the handles themselves for a quick lookup.
   *
   * @param handles JSONArray of handles received as the "refs" field value
   * @return a mapping of handle ref values to handles
   */
  public static Map<Long, JSONObject> getRefHandleMap(List<?> handles) {
    Map<Long, JSONObject> result = new HashMap<Long, JSONObject>();
    for (int i = 0, size = handles.size(); i < size; ++i) {
      JSONObject handle = (JSONObject) handles.get(i);
      Long refValue = JsonUtil.getAsLong(handle, Protocol.REF_HANDLE);
      result.put(refValue, handle);
    }

    return result;
  }

  /**
   * Gets a reference number associated with the given ref object.
   *
   * @param refObject the ref object
   * @return reference number or -1 if no reference value
   */
  public static Long getValueRef(JSONObject refObject) {
    JSONObject argValue = JsonUtil.getAsJSON(refObject, Protocol.ARGUMENT_VALUE);
    if (argValue != null) {
      Long argValueRef = JsonUtil.getAsLong(argValue, Protocol.VALUE_REF);
      if (argValueRef != null) {
        return argValueRef;
      }
    }

    return -1L;
  }

}

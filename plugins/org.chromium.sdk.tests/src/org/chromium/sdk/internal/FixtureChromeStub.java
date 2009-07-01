// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.Breakpoint.Type;
import org.chromium.sdk.BrowserTab.BreakpointCallback;
import org.chromium.sdk.internal.tools.ToolName;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceCommand;
import org.chromium.sdk.internal.tools.v8.BreakpointImpl;
import org.chromium.sdk.internal.tools.v8.DebuggerCommand;
import org.chromium.sdk.internal.tools.v8.DebuggerToolCommand;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.processor.BreakpointProcessor;
import org.chromium.sdk.internal.transport.ChromeStub;
import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.Connection.NetListener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 * A ChromeStub implementation with a certain fixture applied.
 */
@SuppressWarnings("unchecked")
public class FixtureChromeStub implements ChromeStub {

  private static final Map<Long, String> refToObjectMap = new HashMap<Long, String>();
  static {
    // MouseEvent
    refToObjectMap.put(Long.valueOf(getMouseEventRef()),
        "{\"handle\":11,\"type\":\"object\",\"className\":\"MouseEvent\"," +
        "\"constructorFunction\":{\"ref\":19},\"protoObject\":{\"ref\":73}," +
        "\"prototypeObject\":{\"ref\":2},\"properties\":[" +
        "{\"name\":\"x\",\"propertyType\":3,\"ref\":" + getNumber3Ref() + "}," +
        "{\"name\":\"y\",\"propertyType\":3,\"ref\":" + getNumber3Ref() + "}]}");
    // Script
    refToObjectMap.put(Long.valueOf(getScriptRef()),
        "{\"handle\":5,\"type\":\"script\",\"name\":\"file:///C:/1.js\"," +
        "\"id\":" + getScriptId() + ",\"lineOffset\":0,\"columnOffset\":0,\"lineCount\":32," +
        "\"source\":\"SomeObject = function() {\\r\\n  this.fieldOne = \\\"One\\\";\\r\\n};" +
        "\\r\\n\\r\\nSomeObject.prototype.methodTwo = function() {\\r\\n  alert(this.fieldOne);" +
        "\\r\\n}\\r\\n\\r\\n\\r\\nfunction clicked() {\\r\\n  var obj = {\\r\\n" +
        "    objField : { internalObj : { intField : 1}, simpleString : \\\"foo\\\" },\\r\\n" +
        "    someNumber : 3\\r\\n  };\\r\\n  var a = 1;\\r\\n\\r\\n  var arr;\\r\\n" +
        "  arr = [\\\"foo\\\", 3, new Date(), obj, arr];\\r\\n" +
        "  for (var i = 5; i < 240; i += 2) {\\r\\n    arr[i] = \\\"bar\\\";\\r\\n  }\\r\\n" +
        "  arr[100] = 0.99999887;\\r\\n//  var b = a + 1\\r\\n//  console.log('Foo');\\r\\n" +
        "  anotherScript();\\r\\n}\\r\\n\\r\\nfunction anotherScript() {\\r\\n  var i = 0;\\r\\n" +
        "  i += 2;\\r\\n  var someObj = new SomeObject();\\r\\n}\\r\\n\",\"sourceLength\":595," +
        "\"scriptType\":2,\"context\":{\"ref\":4},\"text\":\"file:///C:/1.js (lines: 32)\"}");
    // Function
    refToObjectMap.put(Long.valueOf(getFunctionRef()),
        "{\"handle\":" + getFunctionRef() + ",\"type\":\"function\",\"className\":\"Function\"," +
        "\"constructorFunction\":{\"ref\":31},\"protoObject\":{\"ref\":32}," +
        "\"prototypeObject\":{\"ref\":33},\"name\":\"clicked\",\"inferredName\":\"\"," +
        "\"resolved\":true,\"source\":\"function clicked() {\\r\\n  var obj = {\\r\\n" +
        "    objField : { internalObj : { intField : 1}, simpleString : \\\"foo\\\" },\\r\\n" +
        "    someNumber : 3\\r\\n  };\\r\\n  var a = 1;\\r\\n\\r\\n  var arr;\\r\\n" +
        "  arr = [\\\"foo\\\", 3, new Date(), obj, arr];\\r\\n" +
        "  for (var i = 5; i < 240; i += 2) {\\r\\n    arr[i] = \\\"bar\\\";\\r\\n  }\\r\\n" +
        "  arr[100] = 0.99999887;\\r\\n//  var b = a + 1\\r\\n//  console.log('Foo');\\r\\n" +
        "  anotherScript();\\r\\n}\",\"script\":{\"ref\":" + getScriptRef() +
        "},\"properties\":[{\"name\":\"name\"," +
        "\"attributes\":7,\"propertyType\":3,\"ref\":34},{\"name\":\"caller\",\"attributes\":7," +
        "\"propertyType\":3,\"ref\":11},{\"name\":\"length\",\"attributes\":7,\"propertyType\":3," +
        "\"ref\":35},{\"name\":\"arguments\",\"attributes\":7,\"propertyType\":3,\"ref\":327}," +
        "{\"name\":\"prototype\",\"attributes\":4,\"propertyType\":3,\"ref\":33}]," +
        "\"text\":\"function clicked() {\\r\\n  var obj = {\\r\\n    objField : { internalObj :" +
        " { intField : 1}, simpleString : \\\"foo\\\" },\\r\\n    someNumber : 3\\r\\n  };\\r\\n" +
        "  var a = 1;\\r\\n\\r\\n  var arr;\\r\\n  arr = [\\\"foo\\\", 3, new Date(), obj, arr];" +
        "\\r\\n  for (var i = 5; i < 240; i += 2) {\\r\\n    arr[i] = \\\"bar\\\";\\r\\n  }\\r\\n" +
        "  arr[100] = 0.99999887;\\r\\n//  var b = a + 1\\r\\n//  console.log('Foo');\\r\\n" +
        "  anotherScript();\\r\\n}\"}");
    // Number "3"
    refToObjectMap.put(Long.valueOf(getNumber3Ref()),
        "{\"handle\":" + getNumber3Ref() + ",\"type\":\"number\",\"value\":3,\"text\":\"3\"}");
  }

  public static int getNumber3Ref() {
    return 65;
  }

  public static int getFunctionRef() {
    return 0;
  }

  public static int getScriptRef() {
    return 5;
  }

  public static int getScriptId() {
    return 566;
  }

  public static int getMouseEventRef() {
    return 1;
  }

  private final Map<Long, BreakpointImpl> breakpoints = new HashMap<Long, BreakpointImpl>();
  private boolean isRunning = true;
  private NetListener listener;
  private static long breakpointCounter = 1;
  private static long seqCounter = 1;

  private static final BreakpointProcessor NULL_BREAKPOINT_PROCESSOR =
    new BreakpointProcessor(null) {

      @Override
      public void changeBreakpoint(BreakpointImpl breakpointImpl, BreakpointCallback callback) {
      }

      @Override
      public void clearBreakpoint(BreakpointImpl breakpointImpl, BreakpointCallback callback) {
      }

      @Override
      public void messageReceived(JSONObject response) {
      }

      @Override
      public void setBreakpoint(Type type, String target, int line, int position,
          boolean enabled, String condition, int ignoreCount, BreakpointCallback callback) {
      }
    };

  private static long nextBreakpointId() {
    return breakpointCounter++;
  }

  private static long nextSeq() {
    return seqCounter++;
  }

  @Override
  public Message respondTo(Message requestMessage) {
    // needs request_seq and command at the end
    JSONObject content;
    try {
      content = JsonUtil.jsonObjectFromJson(requestMessage.getContent());
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    String protocolCommand = JsonUtil.getAsString(content, V8Protocol.KEY_COMMAND);
    String response = null;
    boolean success = true;
    if (DebuggerToolCommand.DEBUGGER_COMMAND.commandName.equals(protocolCommand)) {
      JSONObject responseMessage = new JSONObject();
      JSONObject data = JsonUtil.getAsJSON(content, "data");
      Object responseBodyObject = new JSONObject();
      JSONObject responseBody = (JSONObject) responseBodyObject;
      Long seq = JsonUtil.getAsLong(data, V8Protocol.KEY_SEQ);
      String debuggerCommandString = JsonUtil.getAsString(data, V8Protocol.KEY_COMMAND);
      DebuggerCommand debuggerCommand = DebuggerCommand.forString(debuggerCommandString);
      JSONObject args = JsonUtil.getAsJSON(data, "arguments");
      switch (debuggerCommand) {
        case LOOKUP:
          JSONArray handles = JsonUtil.getAsJSONArray(args, "handles");
          for (int i = 0; i < handles.size(); i++) {
            Long ref = (Long) handles.get(i);
            String objectData = refToObjectMap.get(ref);
            if (objectData != null) {
              try {
                responseBody.put(String.valueOf(ref), JsonUtil.jsonObjectFromJson(objectData));
              } catch (ParseException e) {
                throw new RuntimeException(e);
              }
            } else {
              success = false;
            }
          }
          break;
        case SETBREAKPOINT:
        case CHANGEBREAKPOINT:
        {
          long id = (debuggerCommand == DebuggerCommand.SETBREAKPOINT)
              ? nextBreakpointId()
              : JsonUtil.getAsLong(args, "breakpoint");
          breakpoints.put(id, new FakeBreakpoint(Type.SCRIPT, id,
              JsonUtil.getAsBoolean(args, "enabled"),
              JsonUtil.getAsLong(args, "ignoreCount").intValue(),
              JsonUtil.getAsString(args, "condition")));
          responseBody.put("type", "script");
          responseBody.put("breakpoint", id);
          break;
        }
        case CLEARBREAKPOINT: {
          long id = JsonUtil.getAsLong(args, "breakpoint");
          breakpoints.remove(id);
          responseBody.put("type", "script");
          responseBody.put("breakpoint", id);
          break;
        }
        case CONTINUE:
          isRunning = true;
          break;
        case BACKTRACE:
          constructBacktrace(responseMessage, responseBody);
          break;
        case SCRIPTS:
          responseBodyObject = constructScripts();
          break;
        case SOURCE:
          //constructSource();
      }
      responseMessage.put("seq", nextSeq());
      responseMessage.put("request_seq", seq);
      responseMessage.put("command", debuggerCommandString);
      responseMessage.put("type", "response");
      if (success) {
        responseMessage.put("success", true);
        responseMessage.put("running", isRunning);
        responseMessage.put("body", responseBodyObject);
      } else {
        responseMessage.put("success", false);
        responseMessage.put("message", "An error occurred");
      }
      response = responseMessage.toJSONString();
      response = createDebuggerCommandResponse(response);
    } else if (DebuggerToolCommand.ATTACH.commandName.equals(protocolCommand)) {
      response = "{\"command\":\"attach\",\"result\":0}";
    } else if (DebuggerToolCommand.DETACH.commandName.equals(protocolCommand)) {
      response = "{\"command\":\"detach\",\"result\":0}";
    } else if (DevToolsServiceCommand.LIST_TABS.commandName.equals(protocolCommand)) {
      response =
          "{\"command\":\"list_tabs\",\"data\":[[2,\"file:///C:/1.html\"]],\"result\":0}";
    } else if (DevToolsServiceCommand.VERSION.commandName.equals(protocolCommand)) {
      response = "{\"command\":\"version\",\"data\":\"0.1\",\"result\":0}";
    }
    if (response == null) {
      // Unhandled request
      return null;
    }
    return MessageFactory.createMessage(
        requestMessage.getTool(),
        requestMessage.getDestination(),
        response);
  }

  private Object constructScripts() {
    JSONArray scripts = new JSONArray();
    scripts.add(getJsonObjectByRef(getScriptRef()));
    return scripts;
  }

  private String createDebuggerCommandResponse(String response) {
    return "{\"command\":\"debugger_command\",\"result\":0,\"data\":" + response + "}";
  }

  private void constructBacktrace(JSONObject response, JSONObject responseBody) {
    response.put("refs", getRefs());
    responseBody.put("fromFrame", 0);
    responseBody.put("toFrame", 1);
    responseBody.put("totalFrames", 1);
    JSONArray frames = new JSONArray();
    JSONObject frame = new JSONObject();
    frame.put("type", "frame");
    frame.put("index", 0);
    frame.put("receiver", getReceiver());
    frame.put("func", getFunc());
    frame.put("script", getScript());
    frame.put("constructCall", false);
    frame.put("debuggerFrame", false);
    frame.put("arguments", new JSONArray());
    frame.put("locals", getLocalsArray());
    frame.put("position", 305);
    frame.put("line", 18);
    frame.put("column", 3);
    frame.put("sourceLineText", "   foo = bar;");
    frame.put("text", "#00 clicked() file:///C:/1.js line 18 column 3 (position 305)");
    frames.add(frame);
    responseBody.put("frames", frames);
  }

  private JSONArray getRefs() {
    JSONArray refs = new JSONArray();
    refs.add(getJsonObjectByRef(getScriptRef()));
    refs.add(getJsonObjectByRef(getFunctionRef()));
    return refs;
  }

  private JSONObject getJsonObjectByRef(int ref) {
    try {
      return JsonUtil.jsonObjectFromJson(refToObjectMap.get(Long.valueOf(ref)));
    } catch (ParseException e) {
      return null;
    }
  }

  private JSONArray getLocalsArray() {
    JSONArray locals = new JSONArray();
    JSONObject local = new JSONObject();
    JSONObject value = new JSONObject();
    local.put("name", "a");
    value.put("ref", getNumber3Ref());
    value.put("type", "number");
    value.put("value", 1);
    local.put("value", local);
    return locals;
  }

  private JSONObject getScript() {
    JSONObject script = new JSONObject();
    script.put("ref", getScriptRef());
    return script;
  }

  private JSONObject getFunc() {
    JSONObject func = new JSONObject();
    func.put("ref", getFunctionRef());
    func.put("type", "function");
    func.put("name", "clicked");
    func.put("scriptId", getScriptId());
    return func;
  }

  private JSONObject getReceiver() {
    JSONObject receiver = new JSONObject();
    receiver.put("ref", getMouseEventRef());
    receiver.put("type", "object");
    receiver.put("className", "global");
    return receiver;
  }

  public void sendEvent(Message eventMessage) {
    listener.messageReceived(eventMessage);
  }

  public void hitBreakpoints(Collection<Long> breakpointIds) {
    isRunning = false;
    JSONObject eventObject = new JSONObject();
    eventObject.put("seq", nextSeq());
    eventObject.put("type", "event");
    eventObject.put("event", "break");
    JSONObject body = new JSONObject();
    JSONArray bps = new JSONArray();
    bps.addAll(breakpointIds);
    body.put("breakpoints", bps);
    // TODO: add other data if needed
    eventObject.put("body", body);
    sendEvent(createMessage(createDebuggerCommandResponse(eventObject.toJSONString())));
  }

  private Message createMessage(String content) {
    return MessageFactory.createMessage(ToolName.V8_DEBUGGER.value, "2", content);
  }

  private static class FakeBreakpoint extends BreakpointImpl {
    public FakeBreakpoint(Type type, long id, boolean enabled, int ignoreCount, String condition) {
      super(type, id, enabled, ignoreCount, condition, NULL_BREAKPOINT_PROCESSOR);
    }
  }

  @Override
  public void setNetListener(NetListener listener) {
    this.listener = listener;
  }

  public void tabClosed() {
    sendEvent(createMessage("{\"command\":\"closed\",\"result\":0}"));
  }

  public void tabNavigated(String newUrl) {
    sendEvent(
        createMessage("{\"command\":\"navigated\",\"result\":0,\"data\":\"" + newUrl + "\"}"));
  }
}

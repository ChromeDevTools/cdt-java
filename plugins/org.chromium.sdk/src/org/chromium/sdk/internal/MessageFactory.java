// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.HashMap;
import java.util.Map;

import org.chromium.sdk.internal.tools.ToolName;
import org.chromium.sdk.internal.tools.devtools.DevToolsServiceCommand;
import org.chromium.sdk.internal.tools.v8.DebuggerToolCommand;
import org.chromium.sdk.internal.transport.Message;
import org.chromium.sdk.internal.transport.Message.Header;

/**
 * A facility that creates transport {@link Message}s for sending requests to Chrome
 * using the available ChromeDevTools Protocol commands.
 */
public class MessageFactory {

  public static Message ping() {
    return createDevToolsMessage(DevToolsServiceCommand.PING);
  }

  public static Message version() {
    return createDevToolsMessage(DevToolsServiceCommand.VERSION);
  }

  public static Message listTabs() {
    return createDevToolsMessage(DevToolsServiceCommand.LIST_TABS);
  }

  public static Message attach(String tabUid) {
    return createDebuggerMessage(DebuggerToolCommand.ATTACH, tabUid, null);
  }

  public static Message detach(String tabUid) {
    return createDebuggerMessage(DebuggerToolCommand.DETACH, tabUid, null);
  }

  public static Message debuggerCommand(String tabUid, String json) {
    return createDebuggerMessage(DebuggerToolCommand.DEBUGGER_COMMAND, tabUid, json);
  }

  public static Message evaluateJavascript(String tabUid, String javascript) {
    return createDebuggerMessage(DebuggerToolCommand.EVALUATE_JAVASCRIPT, tabUid,
        quoteString(javascript));
  }

  public static Message createMessage(String tool, String destination, String content) {
    Map<String, String> headers = new HashMap<String, String>();
    if (tool != null) {
      headers.put(Header.TOOL.name, tool);
    }
    if (destination != null) {
      headers.put(Header.DESTINATION.name, destination);
    }
    return new Message(headers, content);
  }

  private static String quoteString(String string) {
    return "\"" + string + "\"";
  }

  private static Message createDevToolsMessage(DevToolsServiceCommand command) {
    final String content = "{\"command\":" + quoteString(command.commandName) + "}";
    return createMessage(ToolName.DEVTOOLS_SERVICE.value, null, content);
  }

  private static Message createDebuggerMessage(
      DebuggerToolCommand command, String destination, String dataField) {
    StringBuilder sb = new StringBuilder("{\"command\":\"");
    sb.append(command.commandName).append('"');
    if (dataField != null) {
      sb.append(",\"data\":").append(dataField);
    }
    sb.append('}');
    final String content = sb.toString();
    return createMessage(ToolName.V8_DEBUGGER.value, destination, content);
  }

  private MessageFactory() {
    // not instantiable
  }

}

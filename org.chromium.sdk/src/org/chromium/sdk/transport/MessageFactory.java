// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.transport;

import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.tools.ToolName;
import org.chromium.debug.core.tools.devtools.DevToolsServiceCommand;
import org.chromium.debug.core.tools.v8.V8DebuggerToolCommand;
import org.chromium.debug.core.transport.Message.Header;

/**
 * A facility to create TransportMessages using the available protocol commands.
 */
public class MessageFactory {

  private static final MessageFactory INSTANCE = new MessageFactory();

  public static MessageFactory getInstance() {
    return INSTANCE;
  }

  public Message ping() {
    return createDevToolsMessage(DevToolsServiceCommand.PING);
  }

  public Message version() {
    return createDevToolsMessage(DevToolsServiceCommand.VERSION);
  }

  public Message listTabs() {
    return createDevToolsMessage(DevToolsServiceCommand.LIST_TABS);
  }

  public Message attach(String tabUid) {
    return createDebuggerMessage(V8DebuggerToolCommand.ATTACH, tabUid, null);
  }

  public Message detach(String tabUid) {
    return createDebuggerMessage(V8DebuggerToolCommand.DETACH, tabUid, null);
  }

  public Message debuggerCommand(String tabUid, String json) {
    return createDebuggerMessage(V8DebuggerToolCommand.DEBUGGER_COMMAND,
        tabUid, json);
  }

  public Message evaluateJavascript(String tabUid, String javascript) {
    return createDebuggerMessage(V8DebuggerToolCommand.EVALUATE_JAVASCRIPT,
        tabUid, quoteString(javascript));
  }

  private Message createDevToolsMessage(
      final DevToolsServiceCommand command) {
    final String content = "{\"command\":" + //$NON-NLS-1$
        quoteString(command.commandName()) + "}"; //$NON-NLS-1$
    return createMessage(ToolName.DEVTOOLS_SERVICE.value, null, content);
  }

  private Message createDebuggerMessage(V8DebuggerToolCommand command,
      String destination, String dataField) {
    StringBuilder sb = new StringBuilder("{\"command\":\""); //$NON-NLS-1$
    sb.append(command.commandName).append('"');
    if (dataField != null) {
      sb.append(",\"data\":").append(dataField); //$NON-NLS-1$
    }
    sb.append('}');
    final String content = sb.toString();
    return createMessage(ToolName.V8_DEBUGGER.value, destination, content);
  }

  private static Message createMessage(String tool,
      String destination, String content) {
    Map<String, String> headers = new HashMap<String, String>();
    if (tool != null) {
      headers.put(Header.TOOL.value, tool);
    }
    if (destination != null) {
      headers.put(Header.DESTINATION.value, destination);
    }
    return new Message(headers, content);
  }

  private static String quoteString(String string) {
    return "\"" + string + "\""; //$NON-NLS-1$ //$NON-NLS-2$
  }

  private MessageFactory() {
    // a single instance
  }

}

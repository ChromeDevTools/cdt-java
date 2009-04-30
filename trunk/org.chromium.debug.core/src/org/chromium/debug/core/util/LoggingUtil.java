// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.debug.core.ChromiumDebugPlugin;

/**
 * A generic logging utility.
 */
public class LoggingUtil {

  public static void log(String loggerName, Level level, String message,
      Exception e) {
    Logger.getLogger(loggerName).log(level, message, e);
  }

  public static void warning(Class<?> clazz, String message) {
    log(clazz.getName(), Level.WARNING, message, null);
  }

  public static void logTransport(String message, Object... arguments) {
    if (!ChromiumDebugPlugin.isTransportDebug()) {
      return;
    }
    logPossiblyMessageFormat(message, arguments);
  }

  public static void logV8DebuggerTool(String message, Object... arguments) {
    if (!ChromiumDebugPlugin.isV8DebuggerToolDebug()) {
      return;
    }
    logPossiblyMessageFormat(message, arguments);
  }

  private static void logPossiblyMessageFormat(String message,
      Object... arguments) {
    System.err.println(arguments.length > 0 ? MessageFormat.format(message,
        arguments) : message);
  }
}

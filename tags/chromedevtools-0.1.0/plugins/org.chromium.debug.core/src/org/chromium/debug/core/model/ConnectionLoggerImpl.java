// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.ConnectionLogger;
import org.eclipse.debug.core.DebugPlugin;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Connection logger that writes both incoming and outgoing streams into
 * logWriter with simple annotations.
 */
public class ConnectionLoggerImpl implements ConnectionLogger {
  public ConnectionLoggerImpl(Writer logWriter) {
    this.logWriter = logWriter;
  }

  public Writer wrapWriter(final Writer streamWriter) {
    return new Writer() {
      @Override
      public void close() throws IOException {
        streamWriter.close();
        flushLogWriter();
      }
      @Override
      public void flush() throws IOException {
        streamWriter.flush();
        flushLogWriter();
      }
      @Override
      public void write(char[] cbuf, int off, int len) throws IOException {
        streamWriter.write(cbuf, off, len);
        
        writeToLog(cbuf, off, len, this,
            Messages.ConnectionLoggerImpl_SentToChrome);
      }
    };
  }
  public Reader wrapReader(final Reader streamReader) {
    return new Reader() {
      @Override
      public void close() throws IOException {
        streamReader.close();
        flushLogWriter();
      }

      @Override
      public int read(char[] cbuf, int off, int len) throws IOException {
        int res = streamReader.read(cbuf, off, len);
        if (res > 0) {
          writeToLog(cbuf, off, res, this,
              Messages.ConnectionLoggerImpl_ReceivedFromChrome);
          flushLogWriter();
        }
        return res;
      }
    };
  }
  
  private synchronized void writeToLog(char[] cbuf, int off, int len, Object source,
      String sourceName) {
    try {
      if (lastSource != source) {
        if (lastSource != null) {
          logWriter.append('\n');
        }
        logWriter.append("> ").append(sourceName).append('\n'); //$NON-NLS-1$
        lastSource = source;
      }
      logWriter.write(cbuf, off, len);
    } catch (IOException e) {
      DebugPlugin.log(e);
    }
  }
  private void flushLogWriter() {
    try {
      logWriter.flush();
    } catch (IOException e) {
      DebugPlugin.log(e);
    }
  }
  
  private final Writer logWriter;
  private Object lastSource = null;
}

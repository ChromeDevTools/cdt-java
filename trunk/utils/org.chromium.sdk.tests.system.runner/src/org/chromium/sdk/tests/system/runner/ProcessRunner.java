// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system.runner;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for running external processes with reading their output and
 * additionally dumping it on console.
 */
public class ProcessRunner {
  interface ProcessFactory {
    Process create(Runtime runtime) throws IOException;
  }

  static ProcessWrapper startProcess(ProcessFactory processFactory, boolean dump) {
    Process process;
    try {
      process = processFactory.create(Runtime.getRuntime());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    ProcessWrapper wrapper = new ProcessWrapper(process, dump);
    wrapper.startThreads();
    return wrapper;
  }

  static ProcessOutput executeProcess(ProcessFactory processFactory, boolean dump) {
    return startProcess(processFactory, dump).getResult();
  }

  static class ProcessWrapper {
    private final Process process;
    private final ProcessRunner.StreamReading outputReading;
    private final ProcessRunner.StreamReading errorReading;

    ProcessWrapper(Process process, boolean dump) {
      this.process = process;
      outputReading = new ProcessRunner.StreamReading(process.getInputStream(), dump);
      errorReading = new ProcessRunner.StreamReading(process.getErrorStream(), dump);
    }

    void startThreads() {
      outputReading.start();
      errorReading.start();
    }

    ProcessOutput getResult() {
      final String outputString = outputReading.getResult();
      final String errorString = errorReading.getResult();
      final int exitCode;
      try {
        exitCode = process.waitFor();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return new ProcessOutput() {
        @Override public int getReturnCode() {
          return exitCode;
        }
        @Override public String getOutput() {
          return outputString;
        }
        @Override public String getError() {
          return errorString;
        }
      };
    }

    public void kill() {
      process.destroy();
    }
  }

  private static class StreamReading {
    private final StringBuilder builder = new StringBuilder();
    private final InputStream stream;
    private final Thread thread;

    StreamReading(InputStream streamParam, final boolean dump) {
      this.stream = streamParam;
      thread = new Thread(new Runnable() {
            @Override
            public void run() {
              byte[] buffer = new byte[1024];
              while (true) {
                int res;
                try {
                  res = stream.read(buffer);
                } catch (IOException e) {
                  // Probably the process terminated and the stream was closed.
                  break;
                }
                if (res == -1) {
                  break;
                }
                String str = new String(buffer, 0, res);
                if (dump) {
                  System.out.print(str);
                }
                builder.append(str);
              }
            }
          },
          StreamReading.class.getName());
    }

    void start() {
      thread.start();
    }

    String getResult() {
      try {
        thread.join();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      return builder.toString();
    }
  }

  interface ProcessOutput {
    String getOutput();
    String getError();
    int getReturnCode();
  }
}

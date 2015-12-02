// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.tests.system.runner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

/**
 * A small script-like program that downloads fresh build of Chromium and runs
 * SDK test against it (org.chromium.sdk.tests.system.Main).
 */
public class Runner {
  public static void main(String[] args) {
    BuildLoader.LoadedBuild build = BuildLoader.load();
    File chromeBinary = build.getChromeBinary();

    administerTest(chromeBinary, "--remote-debugging-port", "DEBUGGING", 9224);

    System.out.println("OK");
  }

  private static void administerTest(final File chromeFile, final String chromeArgument,
      String testArgument, final int port) {
    final File userDataDir = new File(chromeFile.getParent(), "user-data-dir");

    userDataDir.mkdirs();
    File firstRunFile = new File(userDataDir, "First Run");
    try {
      new FileOutputStream(firstRunFile).close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }


    // Check that port is available.
    try {
      ServerSocket serverSocket = new ServerSocket(port);
      serverSocket.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ProcessRunner.ProcessFactory processFactory = new ProcessRunner.ProcessFactory() {
      @Override public Process create(Runtime runtime) throws IOException {
        String[] args = new String[] {
            chromeFile.getAbsolutePath(),
            chromeArgument + "=" + port,
            "--user-data-dir=" + userDataDir.getAbsolutePath(),
            ICustom.INSTANCE.getKitWebPageUrl()
        };
        System.out.println("Running: " + Arrays.toString(args));
        return runtime.exec(args, null, chromeFile.getParentFile());
      }

    };
    ProcessRunner.ProcessWrapper chromeProcess = ProcessRunner.startProcess(processFactory, true);
    try {
      Thread.sleep(4000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    try {
      ICustom.INSTANCE.runTestKit(testArgument, port);
    } finally {
      chromeProcess.kill();
    }
  }
}

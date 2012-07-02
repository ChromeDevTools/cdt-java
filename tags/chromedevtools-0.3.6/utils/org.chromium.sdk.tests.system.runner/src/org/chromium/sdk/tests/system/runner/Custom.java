// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.tests.system.runner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An implementation of {@link ICustom} that uses underlying UNIX framework,
 * wget utility and a test kit jars in home directory.
 */
public class Custom implements ICustom {
  private final String homeDir;

  public Custom() {
    homeDir = System.getenv().get("HOME");
  }

  @Override
  public java.io.File downloadChrome(String url) {
    String dirName = executeSimpleShellCommand("mktemp -d /tmp/chrometestbuild.XXXXXXXXXX").trim();

    File tempDir = new File(dirName);
    if (!tempDir.isDirectory()) {
      throw new RuntimeException();
    }
    {
      File urlFile = new File(tempDir, "url");
      try {
        OutputStream outputStream  = new FileOutputStream(urlFile);
        outputStream.write(url.getBytes());
        outputStream.write('\n');
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    File archiveFile = new File(tempDir, "chrome-build.zip");

    executeSimpleShellCommand("wget " + url + " --progress=dot -O" + archiveFile.getAbsolutePath());
    if (!archiveFile.isFile()) {
      throw new RuntimeException();
    }

    executeSimpleShellCommand("cd " + tempDir.getAbsolutePath() + " && unzip " +
        archiveFile.getAbsolutePath());

    if (homeDir == null) {
      throw new RuntimeException("Home dir name not known");
    }
    String symbolicLinkName = homeDir + "/fresh_chrome";
    executeSimpleShellCommand("ln -sfT " + tempDir.getAbsolutePath() + " " + symbolicLinkName);

    return new File(tempDir, "chrome-linux/chrome");
  }

  private static final String KIT_DIR_IN_HOME = "java.sdk.test/kit";

  private String getKitFileDirName() {
    return homeDir + "/" + KIT_DIR_IN_HOME;
  }

  @Override
  public String getKitWebPageUrl() {
    return "file://" + getKitFileDirName() + "/web/main.html";
  }


  @Override
  public void runTestKit(String testArgument, int port) {
    String testJarPath = getKitFileDirName() + "/chromedevtools.systemtest.jar";

    try {
      executeSimpleShellCommand("java -jar " + testJarPath + " --protocol=" +
          testArgument + " localhost " + port);
    } catch (Exception e) {
      throw new RuntimeException("Test failure", e);
    }
  }

  private static String executeSimpleShellCommand(String cmdLine) {
    ProcessRunner.ProcessOutput output = executeShell(cmdLine);
    int returnCode = output.getReturnCode();
    if (returnCode != 0) {
      throw new RuntimeException("Command failure, exit_code=" + returnCode);
    }
    return output.getOutput();
  }


  private static ProcessRunner.ProcessOutput executeShell(String cmdLine) {
    final String[] args = { "/bin/bash", "-c", cmdLine };
    System.out.println("Executing: " + Arrays.toString(args));

    ProcessRunner.ProcessFactory processFactory = new ProcessRunner.ProcessFactory() {
      @Override
      public Process create(Runtime runtime) throws IOException {
        return runtime.exec(args);
      }
    };
    return ProcessRunner.executeProcess(processFactory, true);
  }
}

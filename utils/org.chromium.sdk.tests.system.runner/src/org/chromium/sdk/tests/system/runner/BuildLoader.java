// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.tests.system.runner;

import java.io.File;
import java.util.List;

/**
 * Gets a fresh Chromium build from build site in internet.
 */
class BuildLoader {

  interface LoadedBuild {
    java.io.File getChromeBinary();
    int getBuildNumber();
  }

  static LoadedBuild load() {
    GoogleStorage googleStorage =
        new GoogleStorage("http://commondatastorage.googleapis.com/chromium-browser-continuous");
    List<GoogleStorage.Resource> buildList = googleStorage.readNodes("Linux_x64/");
    int maxBuildNum = -1;
    GoogleStorage.Dir dir = null;
    for (GoogleStorage.Resource res : buildList) {
      GoogleStorage.Dir nextDir = res.accept(
          new GoogleStorage.Resource.Visitor<GoogleStorage.Dir>() {
        @Override public GoogleStorage.Dir visitFile(GoogleStorage.File file) {
          return null;
        }
        @Override public GoogleStorage.Dir visitDir(GoogleStorage.Dir dir) {
          return dir;
        }
      });
      if (nextDir == null) {
        continue;
      }
      int buildNum = Integer.parseInt(nextDir.getShortName());
      if (buildNum > maxBuildNum) {
        maxBuildNum = buildNum;
        dir = nextDir;
      }
    }
    if (dir == null) {
      throw new RuntimeException();
    }

    final int buildVersion = maxBuildNum;

    GoogleStorage.File zipFile = null;
    for (GoogleStorage.Resource res : dir.getChildren()) {
      GoogleStorage.File nextFile =
          res.accept(new GoogleStorage.Resource.Visitor<GoogleStorage.File>() {
        @Override public GoogleStorage.File visitFile(GoogleStorage.File file) {
          return file;
        }
        @Override public GoogleStorage.File visitDir(GoogleStorage.Dir dir) {
          return null;
        }
      });
      if (nextFile == null) {
        continue;
      }
      String shortName = nextFile.getShortName();
      if (!shortName.endsWith(".zip")) {
        continue;
      }
      zipFile = nextFile;
      break;
    }

    if (zipFile == null) {
      throw new RuntimeException();
    }

    String url = zipFile.getUrl();

    final java.io.File chromeFile = ICustom.INSTANCE.downloadChrome(url);

    return new LoadedBuild() {
      @Override public File getChromeBinary() {
        return chromeFile;
      }
      @Override public int getBuildNumber() {
        return buildVersion;
      }
    };
  }

  // Simply loads a new build.
  public static void main(String[] args) {
    LoadedBuild loadedBuild = load();
    System.out.println("Build #" + loadedBuild.getBuildNumber() + " put into " +
        loadedBuild.getChromeBinary().getPath());
  }
}

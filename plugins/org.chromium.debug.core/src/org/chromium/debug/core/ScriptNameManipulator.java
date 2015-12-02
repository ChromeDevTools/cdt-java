// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core;

import java.util.Iterator;
import java.util.List;

/**
 * Parses script names and generates RegExp for them as needed for 'auto-detect' source look-up
 * feature. Incapsulates knowledge about script name schema. For example it may support
 * plain file names, URLs that hold file name as the 'path' part. There might be
 * more exotic implementations that parses URLs like
 * "http://server/get?dir=foo/bar&file=index.html".
 */
public interface ScriptNameManipulator {
  /**
   * Obtains file path from the script name
   */
  FilePath getFileName(String scriptName);

  /**
   * An interface to parsed file path. Iterator allows lazy parsing.
   */
  interface FilePath extends Iterable<String> {
    String getLastComponent();

    /**
     * @return all file path components in reversed order excluding the last one
     */
    @Override public Iterator<String> iterator();
  }

  /**
   * For a file path (presented as components) creates a pattern that matches
   * script names with this file path.
   */
  ScriptNamePattern createPattern(List<String> components);

  /**
   * A wrapper for a pattern. It is implemented as a plain holder of JavaScript
   * RegExp source.
   */
  class ScriptNamePattern {
    private final String javaScriptString;

    public  ScriptNamePattern(String javaScriptString) {
      this.javaScriptString = javaScriptString;
    }

    public String getJavaScriptRegExp() {
      return javaScriptString;
    }
  }
}

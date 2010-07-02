// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;


/**
 * An objects that holds data for a "script" which is a part of a resource
 * loaded into the browser, identified by its original document URL, line offset
 * in the original document, and the line count this script spans.
 */
public interface Script {

  /**
   * Denotes a script type.
   */
  enum Type {
    /** A native, internal JavaScript VM script */
    NATIVE,

    /** A script supplied by an extension */
    EXTENSION,

    /** A normal user script */
    NORMAL
  }

  /**
   * @return the script type
   */
  Type getType();

  /**
   * @return the original document URL for this script known by Chromium.
   *         A null name for eval'd scripts
   */
  String getName();

  /**
   * @return the script ID as reported by the JavaScript VM debugger
   */
  long getId();

  /**
   * @return the start line of this script in the original document
   *         (zero-based)
   */
  int getStartLine();

  /**
   * @return the start column of this script in the original document
   *         (zero-based)
   */
  int getStartColumn();

  /**
   * @return the end line of this script in the original document (zero-based),
   *         inclusive
   */
  int getEndLine();

  /**
   * @return the currently set source text of this script
   */
  String getSource();

  /**
   * @return whether the source for this script is known
   */
  boolean hasSource();

  /**
   * @return whether the script has been collected on remote
   */
  boolean isCollected();
}

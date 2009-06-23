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
    /** A native, internal Javascript VM script */
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
   */
  String getName();

  /**
   * @return the end line of this script in the original document (zero-based)
   */
  int getEndLine();

  /**
   * @return the start line of this script in the original document (zero-based)
   */
  int getLineOffset();

  /**
   * @return the number of lines this script spans in the original document
   */
  int getLineCount();

  /**
   * Sets the source text of this script.
   *
   * @param source of the script
   */
  void setSource(String source);

  /**
   * @return the currently set source text of this script
   */
  String getSource();

  /**
   * @return whether the source for this script is known
   */
  boolean hasSource();

}

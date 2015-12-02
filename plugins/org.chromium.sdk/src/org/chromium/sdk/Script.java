// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk;


/**
 * An objects that holds data for a "script" which is a part of a resource
 * loaded into the browser, identified by its original document URL, line offset
 * in the original document, and the line count this script spans.
 * Its {@link UpdatableScript} aspect is refactored out as a separate interface not to
 * overload {@link Script}.
 */
public interface Script extends UpdatableScript {

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
   * Returns the script ID as reported by the JavaScript VM debugger. The actual ID type
   * is backend-specific, however it must provide equals/hashCode/toString methods and
   * be serializable.
   */
  Object getId();

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

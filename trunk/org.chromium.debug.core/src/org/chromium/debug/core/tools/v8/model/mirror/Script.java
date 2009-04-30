// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.model.mirror;

/**
 * Represents a "script" which is a part of a resource loaded into Chromium,
 * identified by its original document URL, line offset in the original
 * document, and the line count this script spans.
 */
public class Script {
  /** File name (URL) known by the browser */
  private final String name;

  /** File name used by Eclipse (can differ from name) */
  private String resourceName;

  private final int lineOffset;

  private final int lineCount;

  private String source;

  /**
   * @param name the original document URL
   * @param lineOffset in the original document
   * @param lineCount of this script in the original document
   */
  public Script(String name, int lineOffset, int lineCount) {
    this.name = name;
    this.lineOffset = lineOffset;
    this.lineCount = lineCount;
    this.source = null;
  }

  /**
   * Sets the Eclipse resource name this script is mapped to.
   * @param resourceName not null
   */
  public void setResourceName(String resourceName) {
    assert this.resourceName == null;
    this.resourceName = resourceName;
  }

  /**
   * @return the currently known Eclipse resource name for this script.
   */
  public String getResourceName() {
    return resourceName;
  }

  /**
   * @return the original document URL for this script known by Chromium.
   */
  public String getName() {
    return name;
  }

  /**
   * @return the end line of this script in the original document (zero-based)
   */
  public int getEndLine() {
    return lineOffset + lineCount;
  }

  /**
   * @return the start line of this script in the original document (zero-based)
   */
  public int getLineOffset() {
    return lineOffset;
  }

  /**
   * @return the number of lines this script spans in the original document
   */
  public int getLineCount() {
    return lineCount;
  }

  /**
   * Sets the source text of this script.
   */
  public void setSource(String source) {
    this.source = source;
  }

  /**
   * @return the currently set source text of this script
   */
  public String getSource() {
    return source;
  }

  /**
   * @return whether the source for this script is known
   */
  public boolean hasSource() {
    return source != null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[Script (").append(hasSource() ? "has" : "no") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        .append(" source): name=").append(getName()) //$NON-NLS-1$
        .append(", resourceName=").append(getResourceName()) //$NON-NLS-1$
        .append("lineRange=[").append(getLineOffset()) //$NON-NLS-1$
        .append(';').append(getEndLine()).append("]]"); //$NON-NLS-1$
    return sb.toString();
  }

}
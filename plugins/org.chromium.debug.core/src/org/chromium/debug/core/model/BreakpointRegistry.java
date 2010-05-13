// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import org.chromium.sdk.Script;

/**
 * A legacy class that holds ScriptIdentifier.
 * TODO(peter.rybin): consider removing this class.
 */
public class BreakpointRegistry {

  /**
   * Script identifier for a breakpoint location.
   */
  public static class ScriptIdentifier {
    private final String name;

    private final long id;

    private final int startLine;

    private final int endLine;

    public static ScriptIdentifier forScript(Script script) {
      String name = script.getName();
      return new ScriptIdentifier(
          name,
          name != null ? -1 : script.getId(),
          script.getStartLine(),
          script.getEndLine());
    }

    private ScriptIdentifier(String name, long id, int startLine, int endLine) {
      this.name = name;
      this.id = id;
      this.startLine = startLine;
      this.endLine = endLine;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (id ^ (id >>> 32));
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + 17 * startLine + 19 * endLine;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ScriptIdentifier)) {
        return false;
      }
      ScriptIdentifier that = (ScriptIdentifier) obj;
      if (this.startLine != that.startLine || this.endLine != that.endLine) {
        return false;
      }
      if (name == null) {
        // an unnamed script, only id is known
        return that.name == null && this.id == that.id;
      }
      // a named script
      return this.name.equals(that.name);
    }
  }
}

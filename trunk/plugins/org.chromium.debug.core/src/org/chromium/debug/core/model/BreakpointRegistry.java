// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.Script;

/**
 * A registry of existing breakpoints associated with their script locations. It
 * is used to restore
 */
public class BreakpointRegistry {

  /**
   * Script identifier for a breakpoint location.
   */
  public static class ScriptIdentifier {
    private final String name;

    private final long id;

    public static ScriptIdentifier forScript(Script script) {
      String name = script.getName();
      return new ScriptIdentifier(name, name != null
          ? -1
          : script.getId());
    }

    private ScriptIdentifier(String name, long id) {
      this.name = name;
      this.id = id;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (id ^ (id >>> 32));
      result = prime * result + ((name == null)
          ? 0
          : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ScriptIdentifier)) {
        return false;
      }
      ScriptIdentifier that = (ScriptIdentifier) obj;
      if (name == null) {
        // an unnamed script, only id is known
        return that.name == null && this.id == that.id;
      }
      // a named script
      return this.name.equals(that.name);
    }
  }

  static class BreakpointLocation {
    private final ScriptIdentifier scriptIdentifier;

    private final int line;

    public BreakpointLocation(ScriptIdentifier scriptIdentifier, int line) {
      this.scriptIdentifier = scriptIdentifier;
      this.line = line;
    }

    public ScriptIdentifier getScriptIdentifier() {
      return scriptIdentifier;
    }

    public int getLine() {
      return line;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + line;
      result = prime * result + ((scriptIdentifier == null)
          ? 0
          : scriptIdentifier.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof BreakpointLocation)) {
        return false;
      }
      BreakpointLocation that = (BreakpointLocation) obj;
      return (this.line == that.line && eq(this.scriptIdentifier, that.scriptIdentifier));
    }
  }

  /**
   * A breakpoint accompanied by its line number in the corresponding enclosing
   * resource.
   */
  public static class BreakpointEntry {
    public final Breakpoint breakpoint;

    public final int line;

    private BreakpointEntry(Breakpoint breakpoint, int line) {
      this.breakpoint = breakpoint;
      this.line = line;
    }

    boolean isWithinScriptRange(Script script) {
      return line >= script.getStartLine() && line <= script.getEndLine();
    }

    @Override
    public int hashCode() {
      return 31 * line + 17 * breakpoint.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof BreakpointEntry)) {
        return false;
      }
      BreakpointEntry that = (BreakpointEntry) obj;
      return this.line == that.line && this.breakpoint.equals(that.breakpoint);
    }
  }

  private final Map<ScriptIdentifier, Collection<BreakpointEntry>> scriptIdToBreakpointEntries =
      new HashMap<ScriptIdentifier, Collection<BreakpointEntry>>();

  /**
   * Adds the given line breakpoint.
   *
   * @param script where the breakpoint is set
   * @param line (0-based, like in V8) in the script
   * @param breakpoint
   */
  public void add(Script script, int line, Breakpoint breakpoint) {
    ScriptIdentifier scriptId = ScriptIdentifier.forScript(script);
    Collection<BreakpointEntry> entries = scriptIdToBreakpointEntries.get(scriptId);
    if (entries == null) {
      entries = new HashSet<BreakpointEntry>();
      scriptIdToBreakpointEntries.put(scriptId, entries);
    }
    entries.add(new BreakpointEntry(breakpoint, line));
  }

  /**
   * Gets breakpoint entries for the given script.
   *
   * @param script to extract the breakpoints for
   * @return the breakpoints that fall within the given script line range
   */
  public Collection<? extends BreakpointEntry> getBreakpointEntries(Script script) {
    Collection<BreakpointEntry> entries =
        scriptIdToBreakpointEntries.get(ScriptIdentifier.forScript(script));
    if (entries == null) {
      return Collections.emptySet();
    }
    Collection<BreakpointEntry> scriptBreakpoints = new LinkedList<BreakpointEntry>();
    // Linear search should work fairly well for a reasonable number of
    // breakpoints per script
    for (BreakpointEntry entry : entries) {
      if (entry.isWithinScriptRange(script)) {
        scriptBreakpoints.add(entry);
      }
    }
    return scriptBreakpoints;
  }

  /**
   * Removes the given line breakpoint.
   *
   * @param script where the breakpoint is set
   * @param line (0-based, like in V8) in the script
   * @param breakpoint
   */
  public void remove(Script script, int line, Breakpoint breakpoint) {
    ScriptIdentifier scriptId = ScriptIdentifier.forScript(script);
    Collection<BreakpointEntry> entries = scriptIdToBreakpointEntries.get(scriptId);
    if (entries == null) {
      return;
    }
    if (entries.size() == 1) {
      scriptIdToBreakpointEntries.remove(scriptId);
    } else {
      entries.remove(new BreakpointEntry(breakpoint, line));
    }
  }

  protected static boolean eq(Object left, Object right) {
    return left == right || (left != null && left.equals(right));
  }

}

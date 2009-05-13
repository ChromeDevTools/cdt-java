// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.tools.v8.model.mirror;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.tools.v8.Protocol;
import org.chromium.debug.core.util.JsonUtil;
import org.chromium.debug.core.util.WorkspaceUtil;
import org.eclipse.core.runtime.CoreException;
import org.json.simple.JSONObject;

/**
 * Manages scripts known in the current stack context.
 */
public class ScriptManager {

  private final Map<String, Set<Script>> nameToScript =
      new HashMap<String, Set<Script>>();

  private final DebugTargetImpl debugTarget;

  /**
   * @param debugTarget the DebugTargetImpl instance
   */
  public ScriptManager(DebugTargetImpl debugTarget) {
    this.debugTarget = debugTarget;
  }

  /**
   * @param name script name
   * @param addSetIfNull add a new empty set for name if none found
   * @return never null (Collections.emptySet() if none found)
   */
  private Set<Script> getScripts(String name, boolean addSetIfNull) {
    Set<Script> set = nameToScript.get(name);
    if (set == null && addSetIfNull) {
      set = new HashSet<Script>();
      nameToScript.put(name, set);
    }
    return set == null ? Collections.<Script> emptySet() : set;
  }

  /**
   * Adds a script using a "script" V8 response.
   *
   * @param response
   * @return the new script, or null if the response does not contain a script
   *         name
   */
  public Script addScript(JSONObject response) {
    if (!response.containsKey(Protocol.BODY_NAME)) {
      // We do not handle unnamed scripts
      return null;
    }
    String name = JsonUtil.getAsString(response, Protocol.BODY_NAME);
    int lineOffset =
        JsonUtil.getAsLong(response, Protocol.BODY_LINEOFFSET).intValue();
    int lineCount =
        JsonUtil.getAsLong(response, Protocol.BODY_LINECOUNT).intValue();

    Script theScript = findScript(name, lineOffset, lineCount);

    if (theScript == null) {
      theScript = new Script(name, lineOffset, lineCount);
      getScripts(name, true).add(theScript);
    }
    if (response.containsKey(Protocol.SOURCE_CODE)) {
      setSourceCode(response, theScript);
    }

    return theScript;
  }

  /**
   * Associates a source received in a "source" V8 response, with the given
   * script.
   *
   * @param body the JSON response body
   * @param script the script to associate the source with
   */
  public void setSourceCode(JSONObject body, Script script) {
    String src = JsonUtil.getAsString(body, Protocol.SOURCE_CODE);
    if (src == null) {
      return;
    }
    if (script != null && script.getResourceName() == null) {
      String resourceName =
          WorkspaceUtil.createFile(
              this.debugTarget.getDebugProject(), script.getName());
      script.setResourceName(resourceName);
      try {
        WorkspaceUtil.writeFile(debugTarget.getDebugProject(), resourceName,
            fixupSource(src));
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(e);
      }
    }
  }

  /**
   * Fixup source that is of the form (function .... }) the Rhino JS compiler
   * doesn't understand that code as valid. It should need to fix Rhino.
   *
   * TODO(apavlov): Terry says: we need to fix Rhino to allow above expressions.
   *
   * @param source Javascript source code
   * @return cleaned up source code
   */
  private String fixupSource(String source) {
    int startFuncIdx = source.indexOf("(function "); //$NON-NLS-1$
    if (startFuncIdx >= 0 && source.endsWith("})")) { //$NON-NLS-1$
      return source.substring(startFuncIdx + 1, source.length() - 1) + "\n"; //$NON-NLS-1$
    }

    return source;
  }

  /**
   * @param name
   *          script original document URL
   * @param lineOffset
   *          script start line offset in the original document
   * @param lineCount
   *          script line count
   * @return the corresponding script, or null if no such script found
   */
  private Script findScript(String name, int lineOffset, int lineCount) {
    for (Script script : getScripts(name, false)) {
      if (script.getLineOffset() == lineOffset &&
          script.getLineCount() == lineCount) {
        return script;
      }
    }
    return null;
  }

  /**
   * @param resourceName
   *          the Eclipse resource name for a script
   * @return a script corresponding to the given resourceName, or null if no
   *         such script found
   */
  public Script findScriptFromResourceName(String resourceName) {
    for (Set<Script> set : nameToScript.values()) {
      for (Script script : set) {
        if (resourceName.equals(script.getResourceName())) {
          return script;
        }
      }
    }

    return null;
  }

  /**
   * @param resourceName
   *          the Eclipse resource name for a script
   * @return a script original document URL, or null if no such script found
   */
  public String findScriptNameFromResourceName(String resourceName) {
    Script script = findScriptFromResourceName(resourceName);
    return script == null ? null : script.getName();
  }

  /**
   * Finds a script given the original document URL and the line number in the
   * document.
   *
   * @return the script, or null if no such script found
   */
  public Script find(String name, int line) {
    for (Script script : getScripts(name, false)) {
      if (line >= script.getLineOffset() && line < script.getEndLine()) {
        return script;
      }
    }
    return null;
  }

  /**
   * @return whether all known scripts have associated sources
   */
  public boolean isAllSourcesLoaded() {
    for (Set<Script> scripts : nameToScript.values()) {
      for (Script script : scripts) {
        if (!script.hasSource()) {
          return false;
        }
      }
    }
    return true;
  }
}

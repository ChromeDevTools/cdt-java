// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * This object handles the mapping between {@link Script}s and their corresponding resources
 * inside Eclipse.
 */
public class ResourceManager {
  private final Map<IFile, Script> resourceToScript = new HashMap<IFile, Script>();
  private final Map<ScriptIdentifier, IFile> scriptIdToResource =
      new HashMap<ScriptIdentifier, IFile>();
  private final Map<String, IFile> nameToResource = new HashMap<String, IFile>();
  private final IProject debugProject;

  public ResourceManager(IProject debugProject) {
    this.debugProject = debugProject;
  }

  public synchronized void putScript(Script script, IFile resource) {
    ScriptIdentifier scriptId = ScriptIdentifier.forScript(script);
    resourceToScript.put(resource, script);
    scriptIdToResource.put(scriptId, resource);
    nameToResource.put(script.getName(), resource);
  }

  public synchronized Script getScript(IFile resource) {
    return resourceToScript.get(resource);
  }

  public synchronized IFile getResource(Script script) {
    return scriptIdToResource.get(ScriptIdentifier.forScript(script));
  }

  public synchronized boolean scriptHasResource(Script script) {
    return getResource(script) != null;
  }

  public synchronized IFile getResource(String name) {
    return nameToResource.get(name);
  }

  public synchronized void clear() {
    deleteAllScriptFiles();
    resourceToScript.clear();
    scriptIdToResource.clear();
    nameToResource.clear();
  }

  private void deleteAllScriptFiles() {
    if (!resourceToScript.isEmpty()) {
      try {
        ResourcesPlugin.getWorkspace().delete(
            resourceToScript.keySet().toArray(new IFile[resourceToScript.size()]), true, null);
      } catch (CoreException e) {
        ChromiumDebugPlugin.log(e);
      }
    }
  }

  public synchronized void addScript(Script script) {
    IFile scriptFile = getResource(script);
    if (scriptFile == null) {
      scriptFile = ChromiumDebugPluginUtil.createFile(debugProject, getScriptResourceName(script));
      putScript(script, scriptFile);
      writeScriptSource(script, scriptFile);
    }
  }

  public void reloadScript(Script script) {
    IFile scriptFile = getResource(script);
    if (scriptFile == null) {
      throw new RuntimeException("Script file not found"); //$NON-NLS-1$
    }
    writeScriptSource(script, scriptFile);
  }

  private String getScriptResourceName(Script script) {
    String name = script.getName();
    if (name == null) {
      name = Messages.ResourceManager_UnnamedScriptName;
    }
    return name;
  }

  private static void writeScriptSource(Script script, IFile file) {
    if (script.hasSource()) {
      try {
        ChromiumDebugPluginUtil.writeFile(file, script.getSource());
      } catch (final CoreException e) {
        ChromiumDebugPlugin.log(e);
      }
    }
  }

  /**
   * A script identifier class usable as HashMap key.
   */
  private static class ScriptIdentifier {
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

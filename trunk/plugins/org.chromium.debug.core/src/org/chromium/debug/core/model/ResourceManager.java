// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.BreakpointRegistry.BreakpointEntry;
import org.chromium.debug.core.model.BreakpointRegistry.ScriptIdentifier;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;

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
  private final BreakpointRegistry breakpointRegistry;
  private Object fileBeingAdded;

  public ResourceManager(IProject debugProject, BreakpointRegistry breakpointRegistry) {
    this.debugProject = debugProject;
    this.breakpointRegistry = breakpointRegistry;
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
      fileBeingAdded = scriptFile;
      try {
        putScript(script, scriptFile);
        writeScriptSource(script, scriptFile);
        setReadOnly(scriptFile);

        // Perhaps restore breakpoints for the reloaded script
        List<ChromiumLineBreakpoint> breakpoints = new LinkedList<ChromiumLineBreakpoint>();
        for (BreakpointEntry entry : breakpointRegistry.getBreakpointEntries(script)) {
          ChromiumLineBreakpoint lineBreakpoint;
          try {
            lineBreakpoint = new ChromiumLineBreakpoint(scriptFile, entry.line + 1);
          } catch (CoreException e) {
            ChromiumDebugPlugin.log(e);
            continue;
          }
          lineBreakpoint.setBreakpoint(entry.breakpoint);
          breakpoints.add(lineBreakpoint);
        }
        if (!breakpoints.isEmpty()) {
          try {
            DebugPlugin.getDefault().getBreakpointManager().addBreakpoints(
                breakpoints.toArray(new ChromiumLineBreakpoint[breakpoints.size()]));
          } catch (CoreException e) {
            ChromiumDebugPlugin.log(e);
          }
        }
      } finally {
        fileBeingAdded = null;
      }
    }
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

  private void setReadOnly(IFile scriptFile) {
    ResourceAttributes attributes = scriptFile.getResourceAttributes();
    if (attributes != null) {
       attributes.setReadOnly(true);
       try {
        scriptFile.setResourceAttributes(attributes);
      } catch (CoreException e) {
        throw new RuntimeException(e);  // We do not expect it with our file system.
      }
    }
  }

  /**
   * @return whether the given file is being added to the target project
   */
  public boolean isAddingFile(IFile file) {
    return file.equals(fileBeingAdded);
  }
}

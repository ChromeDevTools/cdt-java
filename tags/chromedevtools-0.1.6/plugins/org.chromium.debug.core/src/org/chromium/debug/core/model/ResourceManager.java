// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;
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
  private final IProject debugProject;

  private final Map<VmResourceId, VmResourceInfo> vmResourceId2Info =
      new HashMap<VmResourceId, VmResourceInfo>();
  private final Map<IFile, VmResourceInfo> file2Info = new HashMap<IFile, VmResourceInfo>();

  public ResourceManager(IProject debugProject) {
    this.debugProject = debugProject;
  }

  public synchronized VmResource getVmResource(VmResourceId id) {
    VmResourceInfo info = vmResourceId2Info.get(id);
    if (info == null) {
      return null;
    }
    return info.vmResourceImpl;
  }

  /**
   * @param eclipseSourceName eclipse source file name
   *   (what {@link VmResourceId#getEclipseSourceName()} returns)
   */
  public IFile getFile(String eclipseSourceName) {
    VmResourceId id = VmResourceId.parseString(eclipseSourceName);
    VmResourceInfo info = vmResourceId2Info.get(id);
    if (info == null) {
      return null;
    }
    return info.file;
  }

  public synchronized VmResourceId getResourceId(IFile resource) {
    VmResourceInfo info = file2Info.get(resource);
    if (info == null) {
      return null;
    }
    return info.id;
  }

  public synchronized void addScript(Script newScript) {
    VmResourceId id = VmResourceId.forScript(newScript);
    VmResourceInfo info = vmResourceId2Info.get(id);
    if (info == null) {
      String fileNameTemplate = createFileNameTemplate(id, newScript);
      IFile scriptFile = ChromiumDebugPluginUtil.createFile(debugProject, fileNameTemplate);
      info = new VmResourceInfo(scriptFile, id);
      vmResourceId2Info.put(id, info);
      file2Info.put(scriptFile, info);

      info.scripts.add(newScript);
      writeScriptSource(info.scripts.asCollection(), info.file);
    } else {
      // TODO(peter.rybin): support adding scripts to one resource at once not to rewrite file
      // every time.
      info.scripts.add(newScript);
      writeScriptSource(info.scripts.asCollection(), info.file);
    }
  }

  public void scriptCollected(Script script) {
    // Nothing to do. We only use it for generating resource from several scripts.
  }

  public synchronized void reloadScript(Script script) {
    VmResourceId id = VmResourceId.forScript(script);
    VmResourceInfo info = vmResourceId2Info.get(id);
    if (info == null) {
      throw new RuntimeException("Script file not found"); //$NON-NLS-1$
    }
    info.scripts.add(script);
    writeScriptSource(info.scripts.asCollection(), info.file);
  }

  public synchronized void clear() {
    deleteAllScriptFiles();

    vmResourceId2Info.clear();
    file2Info.clear();
  }

  private void deleteAllScriptFiles() {
    try {
      ResourcesPlugin.getWorkspace().delete(
          file2Info.keySet().toArray(new IFile[file2Info.size()]), true, null);
    } catch (CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  private String createFileNameTemplate(VmResourceId id, Script newScript) {
    return id.createFileNameTemplate(true);
  }

  private static void writeScriptSource(Collection<Script> scripts, IFile file) {
    String fileSource = MockUpResourceWriter.writeScriptSource(scripts);

    try {
      ChromiumDebugPluginUtil.writeFile(file, fileSource);
    } catch (final CoreException e) {
      ChromiumDebugPlugin.log(e);
    }
  }

  private class VmResourceInfo {
    final IFile file;
    final VmResourceId id;
    final ScriptSet scripts = new ScriptSet();
    VmResourceInfo(IFile file, VmResourceId id) {
      this.file = file;
      this.id = id;
    }

    final VmResource vmResourceImpl = new VmResource() {
      public VmResourceId getId() {
        return id;
      }

      public Script getScript() {
        synchronized (ResourceManager.this) {
          return scripts.getSingle();
        }
      }

      public String getFileName() {
        return file.getName();
      }
    };
  }

  private static class ScriptSet {
    private final Map<Long, Script> idToScript = new HashMap<Long, Script>(2);

    Script getSingle() {
      if (idToScript.size() != 1) {
        throw new UnsupportedOperationException(
            "Not supported for compound resources"); //$NON-NLS-1$
      }
      return idToScript.values().iterator().next();
    }

    /**
     * Overwrites old script with the same id.
     */
    public void add(Script newScript) {
      idToScript.put(newScript.getId(), newScript);
    }

    public Collection<Script> asCollection() {
      return idToScript.values();
    }
  }
}

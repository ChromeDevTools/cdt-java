// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.VmResource.Metadata;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.UniqueKeyGenerator;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

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
    ScriptSet scriptSet;
    if (info == null) {
      scriptSet = new ScriptSet();
      info = createAndRegisterResourceFile(id, scriptSet);
    } else {
      // TODO(peter.rybin): support adding scripts to one resource at once not to rewrite file
      // every time.
      scriptSet = (ScriptSet) info.metadata;
    }
    scriptSet.add(newScript);
    writeScriptSource(scriptSet.asCollection(), info.file);
  }

  public synchronized VmResource createTemporaryFile(final Metadata metadata,
      String proposedFileName) {

    UniqueKeyGenerator.Factory<VmResourceInfo> factory =
        new UniqueKeyGenerator.Factory<VmResourceInfo>() {
          public VmResourceInfo tryCreate(String uniqueName) {
            VmResourceId id = VmResourceId.forName(uniqueName);
            VmResourceInfo info = vmResourceId2Info.get(id);
            if (info != null) {
              return null;
            }
            return createAndRegisterResourceFile(id, metadata);
          }
    };

    // Can we have 1000 same-named files?
    final int tryLimit = 1000;
    VmResourceInfo info = UniqueKeyGenerator.createUniqueKey(proposedFileName, tryLimit, factory);
    return info.vmResourceImpl;
  }

  private VmResourceInfo createAndRegisterResourceFile(VmResourceId id,
      VmResource.Metadata metadata) {
    String fileNameTemplate = createFileNameTemplate(id);
    IFile scriptFile = ChromiumDebugPluginUtil.createFile(debugProject, fileNameTemplate);
    VmResourceInfo info = new VmResourceInfo(scriptFile, id, metadata);
    Object conflict;
    conflict = vmResourceId2Info.put(id, info);
    if (conflict != null) {
      throw new RuntimeException();
    }
    conflict = file2Info.put(scriptFile, info);
    if (conflict != null) {
      throw new RuntimeException();
    }
    return info;
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
    ScriptSet scriptSet = (ScriptSet) info.metadata;
    scriptSet.add(script);
    writeScriptSource(scriptSet.asCollection(), info.file);
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

  private String createFileNameTemplate(VmResourceId id) {
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
    final VmResource.Metadata metadata;
    VmResourceInfo(IFile file, VmResourceId id, VmResource.Metadata metadata) {
      this.file = file;
      this.id = id;
      this.metadata = metadata;
    }

    final VmResource vmResourceImpl = new VmResource() {
      public VmResourceId getId() {
        return id;
      }
      public Metadata getMetadata() {
        return metadata;
      }
      public IFile getVProjectFile() {
        return file;
      }
      public void deleteResourceAndFile() {
        ChromiumDebugPluginUtil.removeSafe(vmResourceId2Info, id);
        ChromiumDebugPluginUtil.removeSafe(file2Info, file);

        try {
          file.delete(false, new NullProgressMonitor());
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(e);
        }
      }
      public String getLocalVisibleFileName() {
        String name = file.getName();
        if (name.endsWith(ChromiumDebugPluginUtil.CHROMIUM_EXTENSION_SUFFIX)) {
          return name.substring(0, name.length() -
              ChromiumDebugPluginUtil.CHROMIUM_EXTENSION_SUFFIX.length());
        } else {
          return name;
        }
      }
    };
  }

  private static class ScriptSet implements VmResource.ScriptHolder {
    private final Map<Long, Script> idToScript = new HashMap<Long, Script>(2);

    public Script getSingleScript() {
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

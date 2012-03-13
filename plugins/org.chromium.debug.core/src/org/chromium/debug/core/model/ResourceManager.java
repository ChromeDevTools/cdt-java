// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import static org.chromium.sdk.util.BasicUtil.getSafe;
import static org.chromium.sdk.util.BasicUtil.removeSafe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.VmResource.Metadata;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.UniqueKeyGenerator;
import org.chromium.sdk.Script;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
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

  private final VmResourceIdMap<VmResourceInfo> resourceIdToInfo =
      new VmResourceIdMap<VmResourceInfo>();

  private final Map<IFile, VmResourceInfo> file2Info = new HashMap<IFile, VmResourceInfo>();

  public ResourceManager(IProject debugProject) {
    this.debugProject = debugProject;
  }

  public synchronized VmResource getVmResource(VmResourceId id) {
    VmResourceInfo info = resourceIdToInfo.get(id);
    if (info == null) {
      return null;
    }
    return info.vmResourceImpl;
  }

  public synchronized Collection<? extends VmResource> findVmResources(Pattern pattern) {
    List<VmResource> result = new ArrayList<VmResource>(1);
    for (VmResourceInfo info : file2Info.values()) {
      String name = info.id.getName();
      if (name == null) {
        continue;
      }
      if (!pattern.matcher(name).find()) {
        continue;
      }
      result.add(info.vmResourceImpl);
    }
    return result;
  }

  public synchronized VmResourceId getResourceId(IFile resource) {
    VmResourceInfo info = getSafe(file2Info, resource);
    if (info == null) {
      return null;
    }
    return info.id;
  }

  public synchronized void addScript(Script newScript) {
    VmResourceId id = VmResourceId.forScript(newScript);
    try {
      VmResourceInfo info = resourceIdToInfo.get(id);
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
    } catch (RuntimeException e) {
      throw new RuntimeException("Failed to add script " + id, e);
    }
  }

  public synchronized VmResource createTemporaryFile(final Metadata metadata,
      String proposedFileName) {

    UniqueKeyGenerator.Factory<VmResourceInfo> factory =
        new UniqueKeyGenerator.Factory<VmResourceInfo>() {
          public VmResourceInfo tryCreate(String uniqueName) {
            VmResourceInfo info = resourceIdToInfo.getByName(uniqueName);
            if (info != null) {
              return null;
            }
            // Temporary file has no script id.
            VmResourceId id = new VmResourceId(uniqueName, null);
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
    IFile scriptFile;
    if (id.getName() == null) {
      IFolder specialDir = getOrCreateUnnamedScriptFolder(debugProject);
      scriptFile = ChromiumDebugPluginUtil.createFile(specialDir,
          getFileNameForScriptId(id.getId()));
    } else {
      scriptFile = ChromiumDebugPluginUtil.createFile(debugProject,
          getFileNameForScriptName(id.getName()));
    }

    VmResourceInfo info = new VmResourceInfo(scriptFile, id, metadata);
    resourceIdToInfo.put(id, info);
    Object conflict = file2Info.put(scriptFile, info);
    if (conflict != null) {
      throw new RuntimeException();
    }
    return info;
  }

  private static String getFileNameForScriptName(String scriptName) {
    // Simply take component after the last slash.
    // We may want to use ScriptNameManipulator for this for more accurate work.
    int slashPos = scriptName.lastIndexOf('/');
    return scriptName.substring(slashPos + 1);
  }

  private static String getFileNameForScriptId(Object scriptId) {
    return scriptId.toString();
  }

  private IFolder getOrCreateUnnamedScriptFolder(IProject project) {
    IFolder unnamedDir = project.getFolder(UNNAMED_SCRIPTS_FOLDER_NAME);
    if (!unnamedDir.exists()) {
      try {
        unnamedDir.create(true, true, null);
      } catch (CoreException e) {
        throw new RuntimeException(e);
      }
    }
    return unnamedDir;
  }


  public void scriptCollected(Script script) {
    // Nothing to do. We only use it for generating resource from several scripts.
  }

  public synchronized void reloadScript(Script script) {
    VmResourceId id = VmResourceId.forScript(script);
    VmResourceInfo info = resourceIdToInfo.get(id);
    if (info == null) {
      throw new RuntimeException("Script file not found"); //$NON-NLS-1$
    }
    ScriptSet scriptSet = (ScriptSet) info.metadata;
    scriptSet.add(script);
    writeScriptSource(scriptSet.asCollection(), info.file);
  }

  public synchronized void clear() {
    deleteAllScriptFiles();

    resourceIdToInfo.clear();
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
        resourceIdToInfo.remove(id);
        removeSafe(file2Info, file);

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
    private final Map<Object, Script> idToScript = new HashMap<Object, Script>(2);

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

  private static final String UNNAMED_SCRIPTS_FOLDER_NAME = "unnamed scripts";
}

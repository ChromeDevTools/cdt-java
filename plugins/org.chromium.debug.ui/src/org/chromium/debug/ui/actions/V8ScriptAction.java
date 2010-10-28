// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Shell;

/**
 * A base class for all LiveEdit actions that are scoped to a working file from user workspace.
 * It makes all necessary checks and prepares data in form of {@link ScriptTargetMapping} class.
 * The concrete actions implement the {@link #execute(List, Shell)} method.
 */
public abstract class V8ScriptAction extends FileBasedAction.Single<IFile> {

  public V8ScriptAction() {
    super(false, JS_FILE_NAME_FILTER);
  }

  @Override
  protected void execute(IFile file, Shell shell) {
    List<? extends ScriptTargetMapping> filePairList =
        ChromiumDebugPlugin.getScriptTargetMapping(file);
    execute(filePairList, shell);
  }

  protected abstract void execute(List<? extends ScriptTargetMapping> filePairList, Shell shell);

  /**
   * A temporary method that excludes all cases when there are more than one file pair for a
   * user file. The proper solution ought to provide a UI for user so that he could review
   * which debug sessions should be included in action.
   */
  protected static ScriptTargetMapping getSingleFilePair(
      List<? extends ScriptTargetMapping> pairs) {
    if (pairs.size() == 0) {
      throw new RuntimeException("File is not associated with any V8 VM");
    }
    if (pairs.size() != 1) {
      throw new RuntimeException(
          "File is associated with several V8 VMs, this is not supported yet.");
    }
    return pairs.get(0);
  }

  static final FileBasedAction.FileFilter<IFile> JS_FILE_NAME_FILTER =
      new FileBasedAction.FileFilter<IFile>() {
    @Override
    IFile accept(IFile file) {
      if (filterFileName(file.getName())) {
        return file;
      } else {
        return null;
      }
    }

    /**
     * @return true if action should be enabled for this file name
     */
    private boolean filterFileName(String name) {
      for (String suffix : ChromiumDebugPluginUtil.SUPPORTED_EXTENSIONS_SUFFIX_LIST) {
        if (name.endsWith(suffix)) {
          return true;
        }
      }
      return false;
    }
  };
}


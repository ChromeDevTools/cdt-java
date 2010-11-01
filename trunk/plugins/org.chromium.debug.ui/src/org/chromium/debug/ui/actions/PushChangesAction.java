// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.io.IOException;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.sdk.LiveEditExtension;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.UpdatableScript;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

/**
 * The main action of LiveEdit feature. It gets the current state of a working file and pushes
 * it into running V8 VM.
 */
public class PushChangesAction extends V8ScriptAction {
  @Override
  protected void execute(List<? extends ScriptTargetMapping> filePairList, Shell shell) {
    for (ScriptTargetMapping pair : filePairList) {
      execute(pair);
    }
  }

  private void execute(ScriptTargetMapping filePair) {
    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      public void success(Object report, UpdatableScript.ChangeDescription previewDescription) {
        ChromiumDebugPlugin.log(new Status(IStatus.OK, ChromiumDebugPlugin.PLUGIN_ID,
            "Script has been successfully updated on remote: " + report)); //$NON-NLS-1$
      }
      public void failure(String message) {
        ChromiumDebugPlugin.log(new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
            "Failed to change script on remote: " + message)); //$NON-NLS-1$
      }
    };
    execute(filePair, callback, null, false);
  }

  public static void execute(final ScriptTargetMapping filePair,
      UpdatableScript.UpdateCallback callback, SyncCallback syncCallback, boolean previewOnly) {
    UpdatableScript updatableScript =
        LiveEditExtension.castToUpdatableScript(filePair.getScriptHolder().getSingleScript());

    if (updatableScript == null) {
      throw new RuntimeException();
    }

    byte[] fileData;
    try {
      fileData = ChromiumDebugPluginUtil.readFileContents(filePair.getFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }

    // We are using default charset here like usually.
    String newSource = new String(fileData);

    if (previewOnly) {
      updatableScript.previewSetSource(newSource, callback, syncCallback);
    } else {
      updatableScript.setSourceOnRemote(newSource, callback, syncCallback);
    }
  }
}

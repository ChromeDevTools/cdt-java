// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.PushChangesPlan;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.liveedit.LiveEditResultDialog;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.ChangeDescription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

/**
 * The main action of LiveEdit feature. It gets the current state of a working file and pushes
 * it into running V8 VM.
 */
public class PushChangesAction extends V8ScriptAction {
  @Override
  protected void execute(List<? extends ScriptTargetMapping> filePairList, Shell shell,
      IWorkbenchPart workbenchPart) {
    for (ScriptTargetMapping pair : filePairList) {
      execute(pair, shell);
    }
  }

  private void execute(final ScriptTargetMapping filePair, final Shell shell) {
    final PushChangesPlan plan = PushChangesPlan.create(filePair);

    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      @Override
      public void success(Object report, ChangeDescription changeDescription) {
        ChromiumDebugPlugin.log(new Status(IStatus.OK, ChromiumDebugPlugin.PLUGIN_ID,
            "Script has been successfully updated on remote: " + report)); //$NON-NLS-1$
      }

      @Override
      public void failure(final String message, UpdatableScript.Failure failure) {
        shell.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            LiveEditResultDialog dialog = new LiveEditResultDialog(shell,
                LiveEditResultDialog.createTextInput(message, plan));
            dialog.open();
          }
        });
      }
    };

    plan.execute(false, callback, null);
  }
}

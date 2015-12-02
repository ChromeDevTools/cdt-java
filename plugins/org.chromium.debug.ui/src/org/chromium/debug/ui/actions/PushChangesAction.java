// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.PushChangesPlan;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.liveedit.LiveEditResultDialog;
import org.chromium.debug.ui.liveedit.LiveEditResultDialog.SingleInput;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.ChangeDescription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The main action of LiveEdit feature. It gets the current state of a working file and pushes
 * it into running V8 VM.
 */
public class PushChangesAction extends V8ScriptAction {
  @Override
  protected void execute(List<? extends ScriptTargetMapping> filePairList, Shell shell,
      IWorkbenchPart workbenchPart) {
    LiveEditResultDialog.ErrorPositionHighlighter positionHighlighter =
        createPositionHighlighter(workbenchPart);
    for (ScriptTargetMapping pair : filePairList) {
      execute(pair, shell, positionHighlighter);
    }
  }

  private void execute(final ScriptTargetMapping filePair, final Shell shell,
      final LiveEditResultDialog.ErrorPositionHighlighter positionHighlighter) {
    final PushChangesPlan plan = PushChangesPlan.create(filePair);

    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      @Override
      public void success(boolean resumed, Object report, ChangeDescription changeDescription) {
        ChromiumDebugPlugin.log(new Status(IStatus.OK, ChromiumDebugPlugin.PLUGIN_ID,
            "Script has been successfully updated on remote: " + report)); //$NON-NLS-1$
      }

      @Override
      public void failure(final String message, final UpdatableScript.Failure failure) {
        shell.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            SingleInput textInput = LiveEditResultDialog.createTextInput(message, plan,
                failure);
            LiveEditResultDialog dialog =
                new LiveEditResultDialog(shell, textInput, positionHighlighter);
            dialog.open();
          }
        });
      }
    };

    plan.execute(false, callback, null);
  }

  public static LiveEditResultDialog.ErrorPositionHighlighter createPositionHighlighter(
      IWorkbenchPart workbenchPart) {
    if (workbenchPart instanceof ITextEditor == false) {
      return null;
    }
    final ITextEditor textEditor = (ITextEditor) workbenchPart;
    return new LiveEditResultDialog.ErrorPositionHighlighter() {
      @Override
      public void highlight(int offset, int length) {
        textEditor.selectAndReveal(offset, length);
      }
    };
  }
}

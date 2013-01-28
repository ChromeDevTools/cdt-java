// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.util.List;

import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.liveedit.LiveEditResultDialog;
import org.chromium.debug.ui.liveedit.PushChangesWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;


/**
 * An action that opens a full-featured wizard for pushing changes to remote V8 VM.
 */
public class LiveEditWizardAction extends V8ScriptAction {
  @Override
  protected void execute(List<? extends ScriptTargetMapping> filePairList, Shell shell,
      IWorkbenchPart workbenchPart) {
    LiveEditResultDialog.ErrorPositionHighlighter positionHighlighter =
        PushChangesAction.createPositionHighlighter(workbenchPart);
    PushChangesWizard.start(filePairList, shell, positionHighlighter);
  }
}

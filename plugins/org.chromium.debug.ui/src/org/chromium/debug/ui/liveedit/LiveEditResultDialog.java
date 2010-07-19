// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.TableUtils;
import org.chromium.debug.ui.TableUtils.ColumnBasedLabelProvider;
import org.chromium.debug.ui.TableUtils.ColumnData;
import org.chromium.debug.ui.TableUtils.TrivialAdapter;
import org.chromium.debug.ui.TableUtils.ValueAdapter;
import org.chromium.debug.ui.actions.ChooseVmControl;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * A dialog that shows LiveEdit update results.
 */
class LiveEditResultDialog extends Dialog {

  /**
   * A dialog input. It's essentially an algebraic type, all cases are available
   * via {@link #accept(InputVisitor)} method.
   */
  public interface Input {
    <RES> RES accept(InputVisitor<RES> visitor);
  }

  /**
   * A specialized type of dialog input: one that associated with a particular VM.
   */
  public interface SingleInput extends Input {
    <RES> RES acceptSingle(SingleInputVisitor<RES> visitor);
    ScriptTargetMapping getFilePair();
  }

  public interface InputVisitor<RES> extends SingleInputVisitor<RES> {
    RES visitMultipleResult(MultipleResult multipleResult);
  }

  public interface SingleInputVisitor<RES> {
    RES visitSuccess(SuccessResult successResult);
    RES visitErrorMessage(String text);
  }

  public interface MultipleResult {
    List<? extends SingleInput> getList();
  }

  public interface SuccessResult {
    boolean hasDroppedFrames();
    /** @return may be null */
    OldScriptData getOldScriptData();
  }

  public interface OldScriptData {
    LiveEditDiffViewer.Input getScriptStructure();
    String getOldScriptName();
  }

  private final Input input;

  public LiveEditResultDialog(Shell shell, Input input) {
    super(shell);
    this.input = input;
  }

  @Override
  protected boolean isResizable() {
    return true;
}

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.LiveEditResultDialog_TITLE);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    // create only OK button
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    final Composite composite = (Composite) super.createDialogArea(parent);

    input.accept(new InputVisitor<Void>() {
      public Void visitErrorMessage(String text) {
        createErrorMessageControls(composite, text);
        return null;
      }
      public Void visitSuccess(SuccessResult successResult) {
        createSuccessResultControls(composite, successResult);
        return null;
      }
      public Void visitMultipleResult(MultipleResult multipleResult) {
        createMultipleResultsControl(composite, multipleResult);
        return null;
      }
    });

    return composite;
  }

  private void createErrorMessageControls(Composite parent, String text) {
    Text textControl = new Text(parent, SWT.WRAP);
    Display display = textControl.getDisplay();
    textControl.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    textControl.setText(text);
  }

  private void createSuccessResultControls(Composite parent, SuccessResult successResult) {
    Label label1 = new Label(parent, SWT.NONE);
    label1.setText(Messages.LiveEditResultDialog_SUCCESS);

    if (successResult.hasDroppedFrames()) {
      Label label2 = new Label(parent, SWT.NONE);
      label2.setText(Messages.LiveEditResultDialog_FRAMES_DROPPED);
    }

    final OldScriptData withOldScript = successResult.getOldScriptData();
    if (withOldScript != null) {
      LiveEditDiffViewer.Configuration configuration =
          new LiveEditDiffViewer.Configuration() {
            public String getNewLabel() {
              return Messages.LiveEditResultDialog_CURRENT_SCRIPT;
            }
            public String getOldLabel() {
              return NLS.bind(Messages.LiveEditResultDialog_OLD_SCRIPT,
                  withOldScript.getOldScriptName());
            }
            public boolean oldOnLeft() {
              return false;
            }
      };

      LiveEditDiffViewer viewer = LiveEditDiffViewer.create(parent, configuration);
      viewer.setInput(withOldScript.getScriptStructure());
    }
  }

  /**
   * Result for several VMs' update is shown as a table.
   */
  private void createMultipleResultsControl(Composite parent, MultipleResult multipleResult) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(Messages.LiveEditResultDialog_SEVERAL_VMS);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Table table = new Table(parent, SWT.BORDER);
    table.setFont(parent.getFont());
    table.setLayoutData(new GridData(GridData.FILL_BOTH));

    table.setHeaderVisible(true);

    ValueAdapter<SingleInput, DebugTargetImpl> inputToTargetAdapter =
        new ValueAdapter<SingleInput, DebugTargetImpl>() {
          public DebugTargetImpl convert(SingleInput from) {
            return from.getFilePair().getDebugTarget();
          }
    };

    List<ColumnData<SingleInput, ?>> columnDataList = new ArrayList<ColumnData<SingleInput,?>>(
        ChooseVmControl.createLaunchTargetColumns(inputToTargetAdapter));

    columnDataList.add(
        ColumnData.create(new TrivialAdapter<SingleInput>(), new StatusLabelProvider()));

    // Create physical columns in the table.
    for (ColumnData<?,?> data : columnDataList) {
      data.getLabelProvider().createColumn(table);
    }

    final TableViewer tableViewer = new TableViewer(table);

    IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        MultipleResult input = (MultipleResult) inputElement;
        return input.getList().toArray();
      }
      public void dispose() {}
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    };

    tableViewer.setContentProvider(contentProvider);

    ColumnBasedLabelProvider<SingleInput> labelProvider = new ColumnBasedLabelProvider<SingleInput>(
        TableUtils.createCastAdapter(SingleInput.class), columnDataList);

    tableViewer.setLabelProvider(labelProvider);

    tableViewer.setInput(multipleResult);
  }

  private static class StatusLabelProvider extends TableUtils.ColumnLabelProvider<SingleInput> {
    @Override public Image getColumnImage(SingleInput element) {
      return null;
    }
    @Override public String getColumnText(SingleInput element) {
      return element.acceptSingle(textGetterVisitor);
    }
    @Override public TableColumn createColumn(Table table) {
      TableColumn statusCol = new TableColumn(table, SWT.NONE);
      statusCol.setText(Messages.LiveEditResultDialog_STATUS);
      statusCol.setWidth(200);
      return statusCol;
    }
    private final SingleInputVisitor<String> textGetterVisitor = new SingleInputVisitor<String>() {
      public String visitErrorMessage(String text) {
        return NLS.bind(Messages.LiveEditResultDialog_FAILURE, text);
      }
      public String visitSuccess(SuccessResult successResult) {
        return Messages.LiveEditResultDialog_OK;
      }
    };
  }
}

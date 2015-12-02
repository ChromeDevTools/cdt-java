// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.liveedit;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.PushChangesPlan;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.TableUtils;
import org.chromium.debug.ui.TableUtils.ColumnBasedLabelProvider;
import org.chromium.debug.ui.TableUtils.ColumnData;
import org.chromium.debug.ui.TableUtils.TrivialAdapter;
import org.chromium.debug.ui.TableUtils.ValueAdapter;
import org.chromium.debug.ui.actions.ChooseVmControl;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.CompileErrorFailure;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
public class LiveEditResultDialog extends Dialog {

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
    RES visitErrorMessage(String message, UpdatableScript.Failure failure);
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

  /**
   * Allows dialog to highlight error in source text (presumably, in text editor).
   */
  public interface ErrorPositionHighlighter {
    void highlight(int offset, int length);
  }

  public static SingleInput createTextInput(final String text,
      final PushChangesPlan changesPlan) {
    return createTextInput(text, changesPlan, UpdatableScript.Failure.UNSPECIFIED);
  }

  public static SingleInput createTextInput(final String text,
      final PushChangesPlan changesPlan, final UpdatableScript.Failure failure) {
    return new LiveEditResultDialog.SingleInput() {
      public <RES> RES accept(LiveEditResultDialog.InputVisitor<RES> visitor) {
        return acceptSingle(visitor);
      }
      public <RES> RES acceptSingle(LiveEditResultDialog.SingleInputVisitor<RES> visitor) {
        return visitor.visitErrorMessage(text, failure);
      }
      public ScriptTargetMapping getFilePair() {
        return changesPlan.getScriptTargetMapping();
      }
    };
  }

  private final Input input;
  private final ErrorPositionHighlighter positionHighlighter;

  public LiveEditResultDialog(Shell shell, Input input,
      ErrorPositionHighlighter positionHighlighter) {
    super(shell);
    this.input = input;
    this.positionHighlighter = positionHighlighter;
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
      public Void visitErrorMessage(final String text, UpdatableScript.Failure failure) {
        return failure.accept(new UpdatableScript.Failure.Visitor<Void>() {
          @Override public Void visitUnspecified() {
            createErrorMessageControls(composite, text);
            return null;
          }
          @Override public Void visitCompileError(CompileErrorFailure compileError) {
            createErrorWithPositionControls(composite, Messages.LiveEditResultDialog_COMPILE_ERROR,
                compileError.getCompilerMessage(),
                compileError.getStartPosition(), compileError.getEndPosition());
            return null;
          }
        });
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
    Display display = parent.getDisplay();
    textControl.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    textControl.setText(text);
  }

  private void createErrorWithPositionControls(Composite parent, String localMessage,
      String remoteMessage,
      final TextStreamPosition startPosition,
      TextStreamPosition endPosition) {
    Display display = parent.getDisplay();

    Composite messageComposite;
    {
      messageComposite = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(2, false);
      messageComposite.setLayout(gridLayout);

      Label localMessageLabel = new Label(messageComposite, SWT.NONE);
      localMessageLabel.setText(localMessage);
      Text textControl = new Text(messageComposite, SWT.READ_ONLY);
      textControl.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
      textControl.setText(remoteMessage);
      textControl.setEditable(false);
    }

    Composite positionComposite;
    {
      positionComposite = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = new GridLayout(2, false);
      positionComposite.setLayout(gridLayout);

      Label startLabel = new Label(positionComposite, SWT.NONE);
      startLabel.setText(NLS.bind(Messages.LiveEditResultDialog_LINE_COLUMN,
          startPosition.getLine(), startPosition.getColumn()));

      Button setPositionButton = new Button(positionComposite, SWT.NONE);
      setPositionButton.setText(Messages.LiveEditResultDialog_SELECT_IN_EDITOR);
      if (positionHighlighter == null) {
        setPositionButton.setEnabled(false);
      } else {
        final int length;
        if (endPosition == null) {
          length = 0;
        } else {
          length = endPosition.getOffset() - startPosition.getOffset();
        }
        setPositionButton.addSelectionListener(new SelectionListener() {
          @Override public void widgetSelected(SelectionEvent e) {
            positionHighlighter.highlight(startPosition.getOffset(), length);
          }
          @Override public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(null);
          }
        });
      }
    }
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
      Label new_script_label = new Label(parent, SWT.NONE);
      new_script_label.setText(Messages.LiveEditResultDialog_SCRIPT_CREATED);
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

    ValueAdapter<SingleInput, ConnectedTargetData> inputToTargetAdapter =
        new ValueAdapter<SingleInput, ConnectedTargetData>() {
          public ConnectedTargetData convert(SingleInput from) {
            return from.getFilePair().getConnectedTargetData();
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
      public String visitErrorMessage(final String text, UpdatableScript.Failure failure) {
        String message = failure.accept(new UpdatableScript.Failure.Visitor<String>() {
          @Override public String visitUnspecified() {
            return text;
          }
          @Override public String visitCompileError(CompileErrorFailure compileError) {
            TextStreamPosition start = compileError.getStartPosition();
            return NLS.bind("{0} ({1}:{2})", new Object[] { //$NON-NLS-1$
                compileError.getCompilerMessage(), start.getLine(), start.getColumn() });
          }
        });
        return NLS.bind(Messages.LiveEditResultDialog_FAILURE, message);
      }
      public String visitSuccess(SuccessResult successResult) {
        return Messages.LiveEditResultDialog_OK;
      }
    };
  }
}

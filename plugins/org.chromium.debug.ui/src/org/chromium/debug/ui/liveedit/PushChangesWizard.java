// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.WizardUtils.LogicBasedWizard;
import org.chromium.debug.ui.WizardUtils.PageElements;
import org.chromium.debug.ui.WizardUtils.PageElementsFactory;
import org.chromium.debug.ui.WizardUtils.PageImpl;
import org.chromium.debug.ui.WizardUtils.WizardFinisher;
import org.chromium.debug.ui.WizardUtils.WizardLogic;
import org.chromium.debug.ui.WizardUtils.WizardPageSet;
import org.chromium.debug.ui.actions.ChooseVmControl;
import org.chromium.debug.ui.actions.PushChangesAction;
import org.chromium.debug.ui.liveedit.LiveEditResultDialog.Input;
import org.chromium.debug.ui.liveedit.LiveEditResultDialog.SingleInput;
import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.UpdatableScript;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


/**
 * A wizard that pushes script changes to V8 VM (LiveEdit); it also lets user choose target VM(s),
 * review changes in script text, review which function will be patched.
 */
public class PushChangesWizard {

  public static void start(final List<? extends ScriptTargetMapping> filePairs, Shell shell) {
    // Create pages.
    final PageImpl<ChooseVmPageElements> chooseVmPage = new PageImpl<ChooseVmPageElements>(
        "choose VM", //$NON-NLS-1$
        CHOOSE_VM_PAGE_FACTORY,
        Messages.PushChangesWizard_CHOOSE_VM,
        Messages.PushChangesWizard_CHOOSE_VM_DESCRIPTION);

    final PageImpl<V8PreviewPageElements> v8PreviewPage = new PageImpl<V8PreviewPageElements>(
        "v8 preview", //$NON-NLS-1$
        V8_PREVIEW_PAGE_FACTORY,
        Messages.PushChangesWizard_V8_PREVIEW,
        Messages.PushChangesWizard_V8_PREVIEW_DESCRIPTION);
    final PageImpl<PageElements> multipleVmStubPage = new PageImpl<PageElements>(
        "multiple vm", //$NON-NLS-1$
        MULTIPLE_VM_STUB_PAGE_FACTORY,
        Messages.PushChangesWizard_MULTIPLE_VM,
        Messages.PushChangesWizard_MULTIPLE_VM_DESCRIPTION);

    final PageSet pageSet = new PageSet() {
      public List<? extends PageImpl<?>> getAllPages() {
        return Arrays.<PageImpl<?>>asList(chooseVmPage, v8PreviewPage,
            multipleVmStubPage);
      }
      public PageImpl<ChooseVmPageElements> getChooseVmPage() {
        return chooseVmPage;
      }
      public PageImpl<V8PreviewPageElements> getV8PreviewPage() {
        return v8PreviewPage;
      }
      public PageImpl<PageElements> getMultipleVmStubPage() {
        return multipleVmStubPage;
      }
      public WizardLogic createLogic(final LogicBasedWizard wizardImpl) {
        WizardLogicBuilder logicBuilder = new WizardLogicBuilder(this, wizardImpl);
        return logicBuilder.create(filePairs);
      }
    };

    // Start wizard engine.
    LogicBasedWizard wizard = new LogicBasedWizard(pageSet);
    wizard.setWindowTitle(Messages.PushChangesWizard_TITLE);
    WizardDialog wizardDialog = new WizardDialog(shell, wizard);
    wizardDialog.open();
  }

  /**
   * An access to all wizard pages.
   */
  interface PageSet extends WizardPageSet {
    PageImpl<ChooseVmPageElements> getChooseVmPage();
    PageImpl<V8PreviewPageElements> getV8PreviewPage();
    PageImpl<PageElements> getMultipleVmStubPage();
  }

  ///  Interfaces that link logic with UI controls on each pages.

  interface ChooseVmPageElements extends PageElements {
    ChooseVmControl.Logic getChooseVm();
  }

  interface V8PreviewPageElements extends PageElements {
    LiveEditDiffViewer getPreviewViewer();
  }

  ///  Factories that create UI controls for pages.

  private static final PageElementsFactory<ChooseVmPageElements> CHOOSE_VM_PAGE_FACTORY =
      new PageElementsFactory<ChooseVmPageElements>() {
    public ChooseVmPageElements create(Composite parent) {
      final ChooseVmControl.Logic chooseVm = ChooseVmControl.create(parent);
      chooseVm.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

      return new ChooseVmPageElements() {
        public ChooseVmControl.Logic getChooseVm() {
          return chooseVm;
        }
        public Control getMainControl() {
          return chooseVm.getControl();
        }
      };
    }
  };

  private static final PageElementsFactory<V8PreviewPageElements> V8_PREVIEW_PAGE_FACTORY =
      new PageElementsFactory<V8PreviewPageElements>() {
    public V8PreviewPageElements create(Composite parent) {
      final Composite page = new Composite(parent, SWT.NONE);
      page.setLayout(new GridLayout(1, false));
      LiveEditDiffViewer.Configuration configuration =
          new LiveEditDiffViewer.Configuration() {
            public String getNewLabel() {
              return Messages.PushChangesWizard_CHANGED_SCRIPT;
            }
            public String getOldLabel() {
              return Messages.PushChangesWizard_SCRIPT_IN_VM;
            }
            public boolean oldOnLeft() {
              return false;
            }
      };
      final LiveEditDiffViewer viewer = LiveEditDiffViewer.create(page, configuration);

      return new V8PreviewPageElements() {
        public Control getMainControl() {
          return page;
        }
        public LiveEditDiffViewer getPreviewViewer() {
          return viewer;
        }
      };
    }
  };

  private static final PageElementsFactory<PageElements> MULTIPLE_VM_STUB_PAGE_FACTORY =
      new PageElementsFactory<PageElements>() {
        public PageElements create(Composite parent) {
          final Label label = new Label(parent, 0);

          return new PageElements() {
            public Control getMainControl() {
              return label;
            }
          };
        }
      };

  interface FinisherDelegate {
    LiveEditResultDialog.Input run(IProgressMonitor monitor);
  }

  static class FinisherImpl implements WizardFinisher {
    private final FinisherDelegate delegate;
    FinisherImpl(FinisherDelegate delegate) {
      this.delegate = delegate;
    }
    public boolean performFinish(IWizard wizard, IProgressMonitor monitor) {
      LiveEditResultDialog.Input dialogInput = delegate.run(monitor);
      LiveEditResultDialog dialog =
          new LiveEditResultDialog(wizard.getContainer().getShell(), dialogInput);
      dialog.open();
      return true;
    }
  }

  /**
   * A callback that gets called when user presses 'finish' and a single VM is selected.
   */
  static class SingleVmFinisher implements FinisherDelegate {
    private final ScriptTargetMapping filePair;

    public SingleVmFinisher(ScriptTargetMapping filePair) {
      this.filePair = filePair;
    }
    public Input run(IProgressMonitor monitor) {
      return performSingleVmUpdate(filePair, monitor);
    }
  }

  /**
   * A callback that gets called when user presses 'finish' and several VMs are selected.
   * It performs update and opens result dialog window.
   */
  static class MultipleVmFinisher implements FinisherDelegate {
    private final List<ScriptTargetMapping> targets;
    public MultipleVmFinisher(List<ScriptTargetMapping> targets) {
      this.targets = targets;
    }

    /**
     * Performs updates for each VM and opens dialog window with composite result.
     */
    public Input run(IProgressMonitor monitor) {
      monitor.beginTask(null, targets.size());
      final List<LiveEditResultDialog.SingleInput> results =
          new ArrayList<LiveEditResultDialog.SingleInput>();
      for (ScriptTargetMapping filePair : targets) {
        LiveEditResultDialog.SingleInput dialogInput =
            performSingleVmUpdate(filePair, new SubProgressMonitor(monitor, 1));
        results.add(dialogInput);
      }
      monitor.done();

      final LiveEditResultDialog.MultipleResult multipleResult =
          new LiveEditResultDialog.MultipleResult() {
            public List<? extends SingleInput> getList() {
              return results;
            }
      };

      return new LiveEditResultDialog.Input() {
        public <RES> RES accept(LiveEditResultDialog.InputVisitor<RES> visitor) {
          return visitor.visitMultipleResult(multipleResult);
        }
      };
    }
  }

  /**
   * Performs update to a VM and returns result in form of dialog window input.
   */
  private static LiveEditResultDialog.SingleInput performSingleVmUpdate(
      final ScriptTargetMapping filePair, IProgressMonitor monitor) {
    final LiveEditResultDialog.SingleInput[] input = { null };

    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      public void failure(String message) {
        String text = NLS.bind("Failure: {0}", message);
        input[0] = LiveEditResultDialog.createTextInput(text, filePair);
      }
      public void success(Object report,
          final UpdatableScript.ChangeDescription changeDescription) {
        if (changeDescription == null) {
          input[0] = LiveEditResultDialog.createTextInput(
              Messages.PushChangesWizard_EMPTY_CHANGE, filePair);
        } else {
          final String oldScriptName = changeDescription.getCreatedScriptName();
          final LiveEditResultDialog.OldScriptData oldScriptData;
          if (oldScriptName == null) {
            oldScriptData = null;
          } else {
            final LiveEditDiffViewer.Input previewInput =
                PushResultParser.createViewerInput(changeDescription, filePair, false);
            oldScriptData = new LiveEditResultDialog.OldScriptData() {
              public LiveEditDiffViewer.Input getScriptStructure() {
                return previewInput;
              }
              public String getOldScriptName() {
                return oldScriptName;
              }
            };
          }
          final LiveEditResultDialog.SuccessResult successResult =
              new LiveEditResultDialog.SuccessResult() {
                public LiveEditResultDialog.OldScriptData getOldScriptData() {
                  return oldScriptData;
                }
                public boolean hasDroppedFrames() {
                  return changeDescription.isStackModified();
                }
          };
          input[0] = new LiveEditResultDialog.SingleInput() {
            public <RES> RES accept(LiveEditResultDialog.InputVisitor<RES> visitor) {
              return acceptSingle(visitor);
            }
            public <RES> RES acceptSingle(LiveEditResultDialog.SingleInputVisitor<RES> visitor) {
              return visitor.visitSuccess(successResult);
            }
            public ScriptTargetMapping getFilePair() {
              return filePair;
            }
          };
        }
      }
    };

    CallbackSemaphore syncCallback = new CallbackSemaphore();
    PushChangesAction.execute(filePair, callback, syncCallback, false);
    syncCallback.acquireDefault();

    monitor.done();

    return input[0];
  }
}

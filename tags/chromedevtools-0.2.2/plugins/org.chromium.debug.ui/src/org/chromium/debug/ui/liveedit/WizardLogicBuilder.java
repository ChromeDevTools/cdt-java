// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import static org.chromium.debug.ui.DialogUtils.createConstant;
import static org.chromium.debug.ui.DialogUtils.createErrorOptional;
import static org.chromium.debug.ui.DialogUtils.createOptional;
import static org.chromium.debug.ui.DialogUtils.createProcessor;
import static org.chromium.debug.ui.DialogUtils.handleErrors;

import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.DialogUtils;
import org.chromium.debug.ui.DialogUtils.BranchVariableGetter;
import org.chromium.debug.ui.DialogUtils.Gettable;
import org.chromium.debug.ui.DialogUtils.Message;
import org.chromium.debug.ui.DialogUtils.MessagePriority;
import org.chromium.debug.ui.DialogUtils.NormalExpression;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.OptionalSwitcher;
import org.chromium.debug.ui.DialogUtils.Scope;
import org.chromium.debug.ui.DialogUtils.ScopeEnabler;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueProcessor;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.chromium.debug.ui.WizardUtils.LogicBasedWizard;
import org.chromium.debug.ui.WizardUtils.NextPageEnabler;
import org.chromium.debug.ui.WizardUtils.PageElements;
import org.chromium.debug.ui.WizardUtils.PageImpl;
import org.chromium.debug.ui.WizardUtils.PageListener;
import org.chromium.debug.ui.WizardUtils.WizardFinishController;
import org.chromium.debug.ui.WizardUtils.WizardFinisher;
import org.chromium.debug.ui.WizardUtils.WizardLogic;
import org.chromium.debug.ui.actions.ChooseVmControl;
import org.chromium.debug.ui.liveedit.PushChangesWizard.FinisherDelegate;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.ChangeDescription;

/**
 * Creates Updater-based logic implementation of the wizard. It is responsible for proper data
 * manipulation and view control updates.
 * <p>
 * The wizard pages are arranged in graph with one fork:<br>
 * 'choose vm' -&gt;
 * <ul>
 * <li>[single vm path] -&gt; 'textual preview' -&gt; 'v8 preview'
 * <li>[multiple vm path] -&gt; 'multiple vm stub'
 * </ul>
 */
class WizardLogicBuilder {
  private final Updater updater;
  private final PushChangesWizard.PageSet pageSet;
  private final LogicBasedWizard wizardImpl;

  WizardLogicBuilder(PushChangesWizard.PageSet pageSet, LogicBasedWizard wizardImpl) {
    this.pageSet = pageSet;
    this.wizardImpl = wizardImpl;

    updater = new Updater();
  }

  WizardLogic create(final List<? extends ScriptTargetMapping> targetList) {
    Scope scope = updater.rootScope();

    final boolean skipSingleTargetSelection = true;

    // Wizard logic is described from the first page toward the last pages.

    final PageImpl<PushChangesWizard.ChooseVmPageElements> chooseVmPage = pageSet.getChooseVmPage();

    // A value corresponding to selected VMs on 'choose vm' page.
    final ValueSource<List<ScriptTargetMapping>> selectedVmInput =
        new ValueSource<List<ScriptTargetMapping>>() {
      private final ChooseVmControl.Logic chooseVmControl =
          chooseVmPage.getPageElements().getChooseVm();
      {
        chooseVmControl.setData(targetList);
        chooseVmControl.selectAll();
        final ValueSource<?> thisSource = this;
        ChooseVmControl.Logic.Listener listener = new ChooseVmControl.Logic.Listener() {
          public void checkStateChanged() {
            updater.reportChanged(thisSource);
            updater.update();
          }
        };
        chooseVmControl.addListener(listener);
      }
      public List<ScriptTargetMapping> getValue() {
        return chooseVmControl.getSelected();
      }
    };
    updater.addSource(scope, selectedVmInput);

    // A derived value of selected VMs list; the list is non-empty or the value is error.
    final ValueProcessor<? extends Optional<List<ScriptTargetMapping>>> selectedVmValue =
        createProcessor(new Gettable<Optional<List<ScriptTargetMapping>>>() {
      public Optional<List<ScriptTargetMapping>> getValue() {
        List<ScriptTargetMapping> vmList = selectedVmInput.getValue();
        if (vmList.isEmpty()) {
          return createErrorOptional(
              new Message("Choose at least one VM", MessagePriority.BLOCKING_INFO));
        } else {
          return createOptional(vmList);
        }
      }
    });
    updater.addSource(scope, selectedVmValue);
    updater.addConsumer(scope, selectedVmValue);
    updater.addDependency(selectedVmValue, selectedVmInput);


    // A condition value for up-coming fork between 'single vm' and 'multiple vm' paths.
    Gettable<? extends Optional<? extends Boolean>> singleVmSelectedExpression = handleErrors(
        new NormalExpression<Boolean>() {
          @Calculate
          public Boolean calculate(List<ScriptTargetMapping> selectedVm) {
            return selectedVm.size() == 1;
          }
          @DependencyGetter
          public ValueSource<? extends Optional<List<ScriptTargetMapping>>> getSelectVmSource() {
            return selectedVmValue;
          }
        });

    // A switch between 2 paths: 'single vm' and 'multiple vm'.
    OptionalSwitcher<Boolean> singleVmSelectedSwitch =
        scope.addOptionalSwitch(singleVmSelectedExpression);

    final PreviewAndOptionPath singleVmPath =
        createSingleVmPath(chooseVmPage, singleVmSelectedSwitch, selectedVmValue);
    final PreviewAndOptionPath multipleVmPath =
        createMultipleVmPath(chooseVmPage, singleVmSelectedSwitch, selectedVmValue);

    final PreviewAndOptionPath switchBlockItems = DialogUtils.mergeBranchVariables(
        PreviewAndOptionPath.class, singleVmSelectedSwitch, singleVmPath, multipleVmPath);

    // A simple value converter that wraps wizard delegate as UI-aware wizard finisher.
    ValueProcessor<Optional<? extends WizardFinisher>> finisherValue =
        createProcessor(handleErrors(new NormalExpression<WizardFinisher>() {
              @Calculate
              public WizardFinisher calculate(FinisherDelegate finisherDelegate) {
                return new PushChangesWizard.FinisherImpl(finisherDelegate);
              }
              @DependencyGetter
              public ValueSource<? extends Optional<? extends FinisherDelegate>>
                  getWizardFinisherDelegateSource() {
                return switchBlockItems.getFinisherDelegateValue();
              }
            }));
    updater.addConsumer(scope, finisherValue);
    updater.addSource(scope, finisherValue);
    updater.addDependency(finisherValue, switchBlockItems.getFinisherDelegateValue());

    // A controller that ties finisher value and other warnings to a wizard UI.
    WizardFinishController finishController =
        new WizardFinishController(finisherValue, switchBlockItems.getWarningValue(), wizardImpl);
    updater.addConsumer(scope, finishController);
    updater.addDependency(finishController, switchBlockItems.getFinisherDelegateValue());
    updater.addDependency(finishController, switchBlockItems.getWarningValue());

    return new WizardLogic() {
      public void updateAll() {
        updater.updateAll();
      }
      public PageImpl<?> getStartingPage() {
        return chooseVmPage;
      }
      public void dispose() {
        updater.stopAsync();
      }
    };
  }

  /**
   * An internal interface that defines a uniform output of preview path. The preview path
   * is responsible for returning finisher (which may carry error messages) and it also may
   * return additional warning messages.
   */
  private interface PreviewAndOptionPath {
    @BranchVariableGetter
    ValueSource<? extends Optional<? extends FinisherDelegate>> getFinisherDelegateValue();

    @BranchVariableGetter
    ValueSource<Optional<Void>> getWarningValue();
  }

  /**
   * Creates a 'single vm' page path in wizard. User sees it after choosing exactly one VM.
   * It consists of 2 pages, 'textual preview' and 'v8 preview'.
   */
  private PreviewAndOptionPath createSingleVmPath(PageImpl<?> basePage,
      OptionalSwitcher<Boolean> switcher,
      final ValueSource<? extends Optional<? extends List<ScriptTargetMapping>>> selectedVmValue) {
    // This path consists of 1 page
    final PageImpl<PushChangesWizard.V8PreviewPageElements> v8PreviewPage =
        pageSet.getV8PreviewPage();

    // All logic is inside a dedicated scope, which gets enabled only when user chooses exactly
    // one VM on a previous page. The scope enablement is synchronized with these pages becoming
    // available to user.
    ScopeEnabler scopeEnabler = new NextPageEnabler(basePage, v8PreviewPage);
    Scope scope = switcher.addScope(Boolean.TRUE, scopeEnabler);

    // A value of the single vm, that must be always available within this scope.
    final ValueProcessor<ScriptTargetMapping> singleVmValue =
        createProcessor(new Gettable<ScriptTargetMapping>() {
      public ScriptTargetMapping getValue() {
        // Value targets should be normal (by switcher condition).
        return selectedVmValue.getValue().getNormal().get(0);
      }
    });
    updater.addConsumer(scope, singleVmValue);
    updater.addSource(scope, singleVmValue);
    updater.addDependency(singleVmValue, selectedVmValue);

    // A complex asynchronous value source that feeds update preview data from V8.
    // The data is in raw format.
    final PreviewLoader previewRawResultValue = new PreviewLoader(updater, singleVmValue);
    previewRawResultValue.registerSelf(scope);

    // previewRawResultValue is trigged only when page is actually visible to user.
    v8PreviewPage.addListener(new PageListener() {
      public void onSetVisible(boolean visible) {
        previewRawResultValue.setActive(visible);
      }
    });

    // Parses raw preview value and converts it into a form suitable for the viewer; also handles
    // errors that become warnings.
    final ValueProcessor<Optional<? extends LiveEditDiffViewer.Input>> previewValue =
      createProcessor(handleErrors(
          new NormalExpression<LiveEditDiffViewer.Input>() {
            @Calculate
            public Optional<? extends LiveEditDiffViewer.Input> calculate(
                ChangeDescription previewRawResultParam) {
              ScriptTargetMapping filePair = singleVmValue.getValue();
              ChangeDescription changeDescription = previewRawResultParam;
              Optional<LiveEditDiffViewer.Input> result;
              if (changeDescription == null) {
                result = createOptional(null);
              } else {
                try {
                  LiveEditDiffViewer.Input viewerInput =
                      PushResultParser.createViewerInput(changeDescription, filePair, true);
                  result = createOptional(viewerInput);
                } catch (RuntimeException e) {
                  ChromiumDebugPlugin.log(e);
                  result = createErrorOptional(new Message(
                      "Error in getting preview: " + e.toString(), MessagePriority.WARNING));
                }
              }
              return result;
            }
            @DependencyGetter
            public ValueSource<Optional<UpdatableScript.ChangeDescription>>
                previewRawResultValueSource() {
              return previewRawResultValue;
            }
          }));



    updater.addConsumer(scope, previewValue);
    updater.addSource(scope, previewValue);
    updater.addDependency(previewValue, previewRawResultValue);
    updater.addDependency(previewValue, singleVmValue);

    // A simple consumer that sets preview data to the viewer.
    ValueConsumer v8PreviewInputSetter = new ValueConsumer() {
      public void update(Updater updater) {
        Optional<? extends LiveEditDiffViewer.Input> previewOptional = previewValue.getValue();
        LiveEditDiffViewer.Input viewerInput;
        if (previewOptional.isNormal()) {
          viewerInput = previewOptional.getNormal();
        } else {
          viewerInput = null;
        }
        v8PreviewPage.getPageElements().getPreviewViewer().setInput(viewerInput);
      }
    };
    updater.addConsumer(scope, v8PreviewInputSetter);
    updater.addDependency(v8PreviewInputSetter, previewValue);

    // A warning generator that collects them from v8 preview loader.
    final ValueProcessor<Optional<Void>> warningValue = createProcessor(
        new Gettable<Optional<Void>>() {
      public Optional<Void> getValue() {
        Optional<?> previewResult = previewValue.getValue();
        if (previewResult.isNormal()) {
          return createOptional(null);
        } else {
          return createErrorOptional(previewResult.errorMessages());
        }
      }
    });
    updater.addConsumer(scope, warningValue);
    updater.addSource(scope, warningValue);
    updater.addDependency(warningValue, previewValue);

    // A finisher delegate source, that does not actually depend on most of the code above.
    final ValueProcessor<? extends Optional<FinisherDelegate>> wizardFinisher =
        createProcessor((
            new Gettable<Optional<FinisherDelegate>>() {
          public Optional<FinisherDelegate> getValue() {
            FinisherDelegate finisher =
                new PushChangesWizard.SingleVmFinisher(singleVmValue.getValue());
            return createOptional(finisher);
          }

        }));
    updater.addSource(scope, wizardFinisher);
    updater.addConsumer(scope, wizardFinisher);
    updater.addDependency(wizardFinisher, singleVmValue);

    return new PreviewAndOptionPath() {
      public ValueSource<? extends Optional<FinisherDelegate>> getFinisherDelegateValue() {
        return wizardFinisher;
      }
      public ValueSource<Optional<Void>> getWarningValue() {
        return warningValue;
      }
    };
  }

  /**
   * Creates a 'multiple vm' page path in wizard that is not too reach at this moment.
   * User basically only sees a stub label. We do invest too much into the case when several
   * target VMs are selected.
   */
  private PreviewAndOptionPath createMultipleVmPath(PageImpl<?> basePage,
      OptionalSwitcher<Boolean> switcher,
      final ValueSource<? extends Optional<? extends List<ScriptTargetMapping>>> selectedVmValue) {

    PageImpl<PageElements> multipleVmStubPage = pageSet.getMultipleVmStubPage();

    ScopeEnabler scopeEnabler =
        new NextPageEnabler(basePage, multipleVmStubPage);

    Scope scope = switcher.addScope(Boolean.FALSE, scopeEnabler);

    final ValueProcessor<Optional<? extends FinisherDelegate>> wizardFinisher =
        createProcessor(handleErrors(new NormalExpression<FinisherDelegate>() {
          @Calculate
          public FinisherDelegate calculate(List<ScriptTargetMapping> selectedVm) {
            return new PushChangesWizard.MultipleVmFinisher(selectedVmValue.getValue().getNormal());
          }
          @DependencyGetter
          public ValueSource<? extends Optional<? extends List<ScriptTargetMapping>>>
              getSelectVmSource() {
            return selectedVmValue;
          }
        }));
    updater.addSource(scope, wizardFinisher);
    updater.addConsumer(scope, wizardFinisher);
    updater.addDependency(wizardFinisher, selectedVmValue);

    final ValueSource<Optional<Void>> warningValue =
        createConstant(createOptional((Void) null), updater);

    return new PreviewAndOptionPath() {
      public ValueSource<? extends Optional<? extends FinisherDelegate>>
          getFinisherDelegateValue() {
        return wizardFinisher;
      }
      public ValueSource<Optional<Void>> getWarningValue() {
        return warningValue;
      }
    };
  }
}

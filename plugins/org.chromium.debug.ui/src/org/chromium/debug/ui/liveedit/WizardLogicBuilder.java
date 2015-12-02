// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.liveedit;

import static org.chromium.debug.ui.DialogUtils.createConstant;
import static org.chromium.debug.ui.DialogUtils.createErrorOptional;
import static org.chromium.debug.ui.DialogUtils.createOptional;
import static org.chromium.debug.ui.DialogUtils.createProcessor;
import static org.chromium.debug.ui.DialogUtils.handleErrors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.PushChangesPlan;
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
import org.chromium.debug.ui.liveedit.LiveEditResultDialog.ErrorPositionHighlighter;
import org.chromium.debug.ui.liveedit.LiveEditDiffViewer.Input;
import org.chromium.debug.ui.liveedit.PushChangesWizard.FinisherDelegate;
import org.chromium.sdk.TextStreamPosition;
import org.chromium.sdk.UpdatableScript.ChangeDescription;
import org.chromium.sdk.UpdatableScript.CompileErrorFailure;
import org.eclipse.osgi.util.NLS;

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

  WizardLogic create(final List<? extends ScriptTargetMapping> targetList,
      final ErrorPositionHighlighter positionHighlighter) {
    Scope scope = updater.rootScope();

    final boolean skipSingleTargetSelection = true;

    // Wizard logic is described from the first page toward the last pages.

    final PageImpl<PushChangesWizard.ChooseVmPageElements> chooseVmPage =
        pageSet.getChooseVmPage();

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


    final ValueProcessor<? extends List<Optional<PushChangesPlan>>> selectedChangePlansValue =
        createProcessor(new Gettable<List<Optional<PushChangesPlan>>>() {
      @Override
      public List<Optional<PushChangesPlan>> getValue() {
        List<ScriptTargetMapping> input = selectedVmInput.getValue();
        List<Optional<PushChangesPlan>> result =
            new ArrayList<DialogUtils.Optional<PushChangesPlan>>(input.size());
        for (ScriptTargetMapping mapping : input) {
          Optional<PushChangesPlan> optionalPlan;
          try {
            PushChangesPlan plan = PushChangesPlan.create(mapping);
            optionalPlan = createOptional(plan);
          } catch (RuntimeException e) {
            // TODO: have more specific exception types to catch.
            optionalPlan = createErrorOptional(new Message(
                Messages.WizardLogicBuilder_FAILED_TO_GET,
                MessagePriority.BLOCKING_PROBLEM));
          }
          result.add(optionalPlan);
        }
        return result;
      }
    });
    updater.addSource(scope, selectedChangePlansValue);
    updater.addConsumer(scope, selectedChangePlansValue);
    updater.addDependency(selectedChangePlansValue, selectedVmInput);


    // A derived value of selected VMs list; the list is non-empty or the value is error.
    final ValueProcessor<? extends Optional<List<PushChangesPlan>>> nonEmptySelectedPlansValue =
        createProcessor(new Gettable<Optional<List<PushChangesPlan>>>() {
      public Optional<List<PushChangesPlan>> getValue() {
        List<Optional<PushChangesPlan>> planList = selectedChangePlansValue.getValue();
        if (planList.isEmpty()) {
          return createErrorOptional(
              new Message(Messages.WizardLogicBuilder_CHOOSE_VM, MessagePriority.BLOCKING_INFO));
        }
        List<Message> errorMessages = new LinkedList<Message>();
        List<PushChangesPlan> result = new ArrayList<PushChangesPlan>(planList.size());
        for (Optional<PushChangesPlan> optionalPlan : planList) {
          if (optionalPlan.isNormal()) {
            result.add(optionalPlan.getNormal());
          } else {
            errorMessages.addAll(optionalPlan.errorMessages());
          }
        }
        if (errorMessages.isEmpty()) {
          return createOptional(result);
        } else {
          return createErrorOptional(new HashSet<Message>(errorMessages));
        }
      }
    });
    updater.addSource(scope, nonEmptySelectedPlansValue);
    updater.addConsumer(scope, nonEmptySelectedPlansValue);
    updater.addDependency(nonEmptySelectedPlansValue, selectedChangePlansValue);


    // A condition value for up-coming fork between 'single vm' and 'multiple vm' paths.
    Gettable<? extends Optional<? extends Boolean>> singleVmSelectedExpression = handleErrors(
        new NormalExpression<Boolean>() {
          @Calculate
          public Boolean calculate(List<PushChangesPlan> selectedVm) {
            return selectedVm.size() == 1;
          }
          @DependencyGetter
          public ValueSource<? extends Optional<List<PushChangesPlan>>> getSelectVmSource() {
            return nonEmptySelectedPlansValue;
          }
        });

    // A switch between 2 paths: 'single vm' and 'multiple vm'.
    OptionalSwitcher<Boolean> singleVmSelectedSwitch =
        scope.addOptionalSwitch(singleVmSelectedExpression);

    final PreviewAndOptionPath singleVmPath =
        createSingleVmPath(chooseVmPage, singleVmSelectedSwitch, nonEmptySelectedPlansValue);
    final PreviewAndOptionPath multipleVmPath =
        createMultipleVmPath(chooseVmPage, singleVmSelectedSwitch, nonEmptySelectedPlansValue);

    final PreviewAndOptionPath switchBlockItems = DialogUtils.mergeBranchVariables(
        PreviewAndOptionPath.class, singleVmSelectedSwitch, singleVmPath, multipleVmPath);

    // A simple value converter that wraps wizard delegate as UI-aware wizard finisher.
    ValueProcessor<Optional<? extends WizardFinisher>> finisherValue =
        createProcessor(handleErrors(new NormalExpression<WizardFinisher>() {
              @Calculate
              public WizardFinisher calculate(FinisherDelegate finisherDelegate) {
                return new PushChangesWizard.FinisherImpl(finisherDelegate, positionHighlighter);
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
      final ValueSource<? extends Optional<? extends List<PushChangesPlan>>> selectedVmValue) {
    // This path consists of 1 page
    final PageImpl<PushChangesWizard.V8PreviewPageElements> v8PreviewPage =
        pageSet.getV8PreviewPage();

    // All logic is inside a dedicated scope, which gets enabled only when user chooses exactly
    // one VM on a previous page. The scope enablement is synchronized with these pages becoming
    // available to user.
    ScopeEnabler scopeEnabler = new NextPageEnabler(basePage, v8PreviewPage);
    Scope scope = switcher.addScope(Boolean.TRUE, scopeEnabler);

    // A value of the single vm, that must be always available within this scope.
    final ValueProcessor<PushChangesPlan> singlePlanValue =
        createProcessor(new Gettable<PushChangesPlan>() {
      public PushChangesPlan getValue() {
        // Value targets should be normal (by switcher condition).
        return selectedVmValue.getValue().getNormal().get(0);
      }
    });
    updater.addConsumer(scope, singlePlanValue);
    updater.addSource(scope, singlePlanValue);
    updater.addDependency(singlePlanValue, selectedVmValue);

    // A complex asynchronous value source that feeds update preview data from V8.
    // The data is in raw format.
    final PreviewLoader previewRawResultValue = new PreviewLoader(updater, singlePlanValue);
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
                PreviewLoader.Data previewRawResultParam) {
              final PushChangesPlan changesPlan = singlePlanValue.getValue();

              return previewRawResultParam.accept(
                  new PreviewLoader.Data.Visitor<Optional<LiveEditDiffViewer.Input>>() {
                @Override
                public Optional<LiveEditDiffViewer.Input> visitSuccess(
                    ChangeDescription changeDescription) {
                  if (changeDescription == null) {
                    return createOptional(null);
                  } else {
                    try {
                      LiveEditDiffViewer.Input viewerInput =
                          PushResultParser.createViewerInput(changeDescription, changesPlan, true);
                      return createOptional(viewerInput);
                    } catch (RuntimeException e) {
                      ChromiumDebugPlugin.log(e);
                      String messageText =
                          NLS.bind(Messages.WizardLogicBuilder_ERROR_GETTING_PREVIEW, e.toString());
                      return createErrorOptional(
                          new Message(messageText, MessagePriority.WARNING));
                    }
                  }
                }

                @Override
                public Optional<Input> visitCompileError(CompileErrorFailure compileError) {
                  LiveEditDiffViewer.Input viewerInput =
                      PushResultParser.createCompileErrorViewerInput(compileError, changesPlan,
                          true);
                  return createOptional(viewerInput);
                }
              });
            }
            @DependencyGetter
            public ValueSource<Optional<PreviewLoader.Data>>
                previewRawResultValueSource() {
              return previewRawResultValue;
            }
          }));



    updater.addConsumer(scope, previewValue);
    updater.addSource(scope, previewValue);
    updater.addDependency(previewValue, previewRawResultValue);
    updater.addDependency(previewValue, singlePlanValue);

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
        Optional<PreviewLoader.Data> previewResult = previewRawResultValue.getValue();
        if (previewResult.isNormal()) {
          PreviewLoader.Data data = previewResult.getNormal();
          return data.accept(new PreviewLoader.Data.Visitor<Optional<Void>>() {
            @Override public Optional<Void> visitSuccess(ChangeDescription changeDescription) {
              return createOptional(null);
            }
            @Override
            public Optional<Void> visitCompileError(CompileErrorFailure compileError) {
              TextStreamPosition start = compileError.getStartPosition();
              String messageString = NLS.bind(Messages.WizardLogicBuilder_COMPILE_ERROR_AT,
                  new Object[] { compileError.getCompilerMessage(),
                  start.getLine(), start.getColumn() });
              return createErrorOptional(
                  new Message(messageString, MessagePriority.BLOCKING_PROBLEM));
            }
          });
        } else {
          return createErrorOptional(previewResult.errorMessages());
        }
      }
    });
    updater.addConsumer(scope, warningValue);
    updater.addSource(scope, warningValue);
    updater.addDependency(warningValue, previewRawResultValue);

    // A finisher delegate source, that does not actually depend on most of the code above.
    final ValueProcessor<? extends Optional<FinisherDelegate>> wizardFinisher =
        createProcessor((
            new Gettable<Optional<FinisherDelegate>>() {
          public Optional<FinisherDelegate> getValue() {
            FinisherDelegate finisher =
                new PushChangesWizard.SingleVmFinisher(singlePlanValue.getValue());
            return createOptional(finisher);
          }

        }));
    updater.addSource(scope, wizardFinisher);
    updater.addConsumer(scope, wizardFinisher);
    updater.addDependency(wizardFinisher, singlePlanValue);

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
      final ValueSource<? extends Optional<? extends List<PushChangesPlan>>> selectedVmValue) {

    PageImpl<PageElements> multipleVmStubPage = pageSet.getMultipleVmStubPage();

    ScopeEnabler scopeEnabler =
        new NextPageEnabler(basePage, multipleVmStubPage);

    Scope scope = switcher.addScope(Boolean.FALSE, scopeEnabler);

    final ValueProcessor<Optional<? extends FinisherDelegate>> wizardFinisher =
        createProcessor(handleErrors(new NormalExpression<FinisherDelegate>() {
          @Calculate
          public FinisherDelegate calculate(List<PushChangesPlan> selectedVm) {
            return new PushChangesWizard.MultipleVmFinisher(
                selectedVmValue.getValue().getNormal());
          }
          @DependencyGetter
          public ValueSource<? extends Optional<? extends List<PushChangesPlan>>>
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

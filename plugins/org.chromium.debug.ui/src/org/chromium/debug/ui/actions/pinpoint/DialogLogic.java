// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions.pinpoint;

import static org.chromium.debug.ui.DialogUtils.addModifyListener;
import static org.chromium.debug.ui.DialogUtils.createConstant;
import static org.chromium.debug.ui.DialogUtils.createErrorOptional;
import static org.chromium.debug.ui.DialogUtils.createOptional;
import static org.chromium.debug.ui.DialogUtils.createProcessor;
import static org.chromium.debug.ui.DialogUtils.handleErrors;
import static org.chromium.debug.ui.DialogUtils.mergeBranchVariables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.chromium.debug.core.model.Value;
import org.chromium.debug.ui.DialogUtils;
import org.chromium.debug.ui.DialogUtils.BranchVariableGetter;
import org.chromium.debug.ui.DialogUtils.Gettable;
import org.chromium.debug.ui.DialogUtils.Message;
import org.chromium.debug.ui.DialogUtils.MessagePriority;
import org.chromium.debug.ui.DialogUtils.NormalExpression;
import org.chromium.debug.ui.DialogUtils.OkButtonControl;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.Scope;
import org.chromium.debug.ui.DialogUtils.ScopeEnabler;
import org.chromium.debug.ui.DialogUtils.Switcher;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueProcessor;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.chromium.sdk.DebugContext;
import org.chromium.sdk.EvaluateWithContextExtension;
import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsEvaluateContext.EvaluateCallback;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog window logic implementation.
 */
class DialogLogic {
  /**
   * API to logic for dialog window disposals.
   */
  interface Handle {
    // Initialization after everything has been built.
    void updateAll();

    Runnable getOkRunnable();

    void saveStateInStore();
  }


  /**
   * Builds an updater-based dialog logic. This is more a function-style programming inside
   * (to better deal with various states and transitions of dialog).
   */
  static Handle buildDialogLogic(final DialogImpl.Elements elements,
      final DialogImpl.DialogPreferencesStore dialogPreferencesStore, final Value uiValue) {
    final Updater updater = new Updater();
    Scope rootScope = updater.rootScope();

    // A global dialog warning collection (those warnings that are not tied to the primary
    // result value).
    List<ValueSource<String>> warningSources = new ArrayList<ValueSource<String>>(2);

    // 'Property expression' editor raw value.
    final ValueSource<String> propertyExpressionEditorValue = new ValueSource<String>() {
      private final Text textElement = elements.getExpressionText();
      {
        String text = dialogPreferencesStore.getExpressionText();
        textElement.setText(text);
        // Select all expression but a first dot.
        int selectionStart = text.startsWith(".") ? 1 : 0; //$NON-NLS-1$
        textElement.setSelection(selectionStart, text.length());
        addModifyListener(textElement, this, updater);
      }
      public String getValue() {
        return textElement.getText();
      }
    };
    updater.addSource(rootScope, propertyExpressionEditorValue);

    // A preview context value. It is constant but optional (so it's passed via updater).
    final ValueSource<Optional<DialogLogic.PreviewContext>> evaluatorValue =
        createConstant(PreviewContext.build(uiValue), updater);

    // Property expression parsed as Expression. Parse errors are kept in Optional.
    final ValueProcessor<Optional<DialogLogic.Expression>> parsedPropertyExpressionValue =
      createProcessor(new Gettable<Optional<DialogLogic.Expression>>() {
        @Override
        public Optional<DialogLogic.Expression> getValue() {
          return parseExpression(propertyExpressionEditorValue.getValue());
        }
      });
    updater.addConsumer(rootScope, parsedPropertyExpressionValue);
    updater.addSource(rootScope, parsedPropertyExpressionValue);
    updater.addDependency(parsedPropertyExpressionValue, propertyExpressionEditorValue);


    // 'Show preview' check box value.
    final ValueSource<Boolean> previewCheckBoxValue = new ValueSource<Boolean>() {
      private final Button checkBox = elements.getPreviewCheckBox();
      {
        checkBox.setSelection(dialogPreferencesStore.getPreviewCheck());
        addModifyListener(checkBox, this, updater);
      }
      @Override
      public Boolean getValue() {
        return checkBox.getSelection();
      }
    };

    // A conditional block that holds optional preview section.
    Switcher<Boolean> checkerSwitch = rootScope.addSwitch(previewCheckBoxValue);

    PreviewSwitchOutput switchBlockOutput = fillShowPreviewSwitch(checkerSwitch, updater,
        rootScope, elements, evaluatorValue, parsedPropertyExpressionValue);

    // Preview block may emit warning.
    warningSources.add(switchBlockOutput.warningSource());

    // 'Add watch expression' check box value.
    final ValueSource<Boolean> addWatchExpressionValue = new ValueSource<Boolean>() {
      private final Button checkBox = elements.getAddWatchCheckBox();
      {
        checkBox.setSelection(dialogPreferencesStore.getAddWatchExpression());
        addModifyListener(checkBox, this, updater);
      }
      @Override
      public Boolean getValue() {
        return checkBox.getSelection();
      }
    };
    updater.addSource(rootScope, addWatchExpressionValue);

    // An OK button implementation that do set property in remote VM.
    final ValueProcessor<Optional<? extends Runnable>> okRunnable = createProcessor(handleErrors(
        new NormalExpression<Runnable>() {
          @Calculate
          public Runnable calculate(DialogLogic.PreviewContext previewContext,
              DialogLogic.Expression expression) {
            return new OkRunnable(elements.getParentShell(), expression, previewContext,
                addWatchExpressionValue.getValue());
          }

          @DependencyGetter
          public ValueSource<Optional<DialogLogic.PreviewContext>> previewContextSource() {
            return evaluatorValue;
          }

          @DependencyGetter
          public ValueSource<Optional<DialogLogic.Expression>> parsedPropertyExpressionSource() {
            return parsedPropertyExpressionValue;
          }
        })
    );
    updater.addSource(rootScope, okRunnable);
    updater.addConsumer(rootScope, okRunnable);
    updater.addDependency(okRunnable, evaluatorValue);
    updater.addDependency(okRunnable, parsedPropertyExpressionValue);
    updater.addDependency(okRunnable, addWatchExpressionValue);

    final OkButtonControl<Runnable> okButtonControl =
        new OkButtonControl<Runnable>(okRunnable, warningSources, elements);
    updater.addConsumer(rootScope, okButtonControl);
    updater.addDependency(okButtonControl, okButtonControl.getDependencies());

    return new Handle() {
      @Override
      public void updateAll() {
        updater.updateAll();
      }
      @Override
      public Runnable getOkRunnable() {
        return okButtonControl.getNormalValue();
      }
      @Override
      public void saveStateInStore() {
        dialogPreferencesStore.setExpressionText(elements.getExpressionText().getText());
        dialogPreferencesStore.setPreviewCheck(elements.getPreviewCheckBox().getSelection());
        dialogPreferencesStore.setAddWatchExpression(
            elements.getAddWatchCheckBox().getSelection());
      }
    };
  }

  private static class OkRunnable implements Runnable {
    private final Shell parentShell;
    private final DialogLogic.Expression expression;
    private final DialogLogic.PreviewContext previewContext;
    private final boolean addWatchExpression;

    OkRunnable(Shell parentShell, DialogLogic.Expression expression,
        DialogLogic.PreviewContext previewContext, boolean addWatchExpression) {
      this.parentShell = parentShell;
      this.expression = expression;
      this.previewContext = previewContext;
      this.addWatchExpression = addWatchExpression;
    }

    @Override
    public void run() {
      Expression.SetCallback callback = new Expression.SetCallback() {
        @Override
        public void done(final String errorMessage) {
          if (errorMessage == null) {
            if (addWatchExpression) {
              IExpressionManager expressionManager =
                  DebugPlugin.getDefault().getExpressionManager();
              IWatchExpression watchExpression =
                  expressionManager.newWatchExpression(expression.getWatchExpression());
              expressionManager.addExpression(watchExpression);
            }
          } else {
            parentShell.getDisplay().asyncExec(new Runnable() {
              @Override
              public void run() {
                MessageBox messageBox = new MessageBox(parentShell);
                messageBox.setText(Messages.LogicImpl_RESULT_FAILURE_TITLE);
                messageBox.setMessage(errorMessage);
                messageBox.open();
              }
            });
          }
        }
      };

      expression.execute(previewContext, callback);
    }
  }

  /**
   * Output values from preview switch block in the dialog logic.
   */
  private interface PreviewSwitchOutput {
    /** A warning that preview block may emit. */
    @BranchVariableGetter ValueSource<String> warningSource();
  }

  /**
   * Creates dialog logic elements for the optional preview.
   * @return block output variables
   */
  private static PreviewSwitchOutput fillShowPreviewSwitch(Switcher<Boolean> switcher,
      Updater updater, Scope scope, final DialogImpl.Elements elements,
      ValueSource<Optional<DialogLogic.PreviewContext>> evaluatorValue,
      ValueProcessor<Optional<DialogLogic.Expression>> parsedPropertyNameValue) {

    // Switch branch that corresponds to enabled preview.
    Scope checkerScope = switcher.addScope(Boolean.TRUE, new ScopeEnabler() {
      @Override
      public void setEnabled(boolean enabled, boolean recursive) {
        elements.getPreviewDisplay().setEnabled(enabled);
      }
    });
    PreviewSwitchOutput checkDisplayCase = fillCheckDisplayCase(updater, checkerScope,
        elements, evaluatorValue, parsedPropertyNameValue);

    // Switch branch that corresponds to disabled preview.
    switcher.addScope(Boolean.FALSE, null);

    // Two branches merge their output.
    final PreviewSwitchOutput mergedOutput = mergeBranchVariables(PreviewSwitchOutput.class,
        switcher, checkDisplayCase, null);

    return new PreviewSwitchOutput() {
      @Override
      public ValueSource<String> warningSource() {
        return mergedOutput.warningSource();
      }
    };
  }

  /**
   * Creates enabled preview branch in dialog logic.
   * @return branch output variables
   */
  private static PreviewSwitchOutput fillCheckDisplayCase(Updater updater, Scope scope,
      final DialogImpl.Elements elements,
      ValueSource<Optional<DialogLogic.PreviewContext>> evaluatorValue,
      ValueProcessor<Optional<DialogLogic.Expression>> parsedPropertyNameValue) {

    // Asynchronous processor that checks property expression in a remote VM
    // and returns diagnostic.
    final DialogLogic.CheckerResultProcessor checkerResultSource =
        new CheckerResultProcessor(evaluatorValue, parsedPropertyNameValue, updater);
    updater.addConsumer(scope, checkerResultSource);
    updater.addSource(scope, checkerResultSource);
    updater.addSource(scope, checkerResultSource.getWarningSource());
    updater.addDependency(checkerResultSource, evaluatorValue);
    updater.addDependency(checkerResultSource, parsedPropertyNameValue);

    // Consumes diagnostic from above and sets it into a label.
    ValueConsumer checkerResultConsumer = new ValueConsumer() {
      @Override
      public void update(Updater updater) {
        elements.getPreviewDisplay().setText(checkerResultSource.getValue());
      }
    };
    updater.addConsumer(scope, checkerResultConsumer);
    updater.addDependency(checkerResultConsumer, checkerResultSource);

    return new PreviewSwitchOutput() {
      @Override
      public ValueSource<String> warningSource() {
        return checkerResultSource.getWarningSource();
      }
    };
  }

  /**
   * Contains several objects that are necessary for expression preview or set evaluation.
   * It is essentially a struct.
   */
  private static class PreviewContext {
    /**
     * Builds PreviewContext from what was passed from the action. Takes into account
     * various problems that are returned as error value of {@link Optional}.
     */
    static Optional<DialogLogic.PreviewContext> build(Value uiValue) {
      if (uiValue == null) {
        return createErrorOptional(new Message(Messages.LogicImpl_VALUE_IS_NOT_AVAILABLE,
                MessagePriority.BLOCKING_PROBLEM));
      }
      JsValue jsValue = uiValue.getJsValue();
      JsObject jsObject = jsValue.asObject();
      if (jsObject == null) {
        return createErrorOptional(
            new Message(Messages.LogicImpl_NOT_FOR_PRIMITIVE, MessagePriority.BLOCKING_PROBLEM));
      }

      DebugContext debugContext = uiValue.getEvaluateContext().getDebugContext();
      JavascriptVm javascriptVm = debugContext.getJavascriptVm();
      EvaluateWithContextExtension extenstion = javascriptVm.getEvaluateWithContextExtension();
      if (extenstion == null) {
        return createErrorOptional(new Message(Messages.LogicImpl_FEATURE_NOT_SUPPORTED,
            MessagePriority.BLOCKING_PROBLEM));
      }
      if (uiValue.getDebugTarget().getDebugContext() != debugContext) {
        return createErrorOptional(
            new Message(Messages.LogicImpl_CONTEXT_DISMISSED, MessagePriority.BLOCKING_PROBLEM));
      }
      JsEvaluateContext globalEvaluateContext = debugContext.getGlobalEvaluateContext();

      return createOptional(new PreviewContext(extenstion, globalEvaluateContext, jsObject));
    }

    final EvaluateWithContextExtension extension;
    final JsEvaluateContext globalEvaluateContext;
    final JsObject jsObject;

    private PreviewContext(EvaluateWithContextExtension extension,
        JsEvaluateContext globalEvaluateContext, JsObject jsObject) {
      this.extension = extension;
      this.globalEvaluateContext = globalEvaluateContext;
      this.jsObject = jsObject;
    }
  }

  /**
   * Asynchronous processor. It 'previews' property expression, i.e. checks on remote
   * whether such property empty or already used and if there is some problem with it.
   * Its inputs: preview context and parsed expression.
   * Its outputs: diagnostic string and additional warning.
   */
  private static class CheckerResultProcessor implements ValueSource<String>, ValueConsumer {
    private final ValueSource<Optional<DialogLogic.PreviewContext>> previewContextValue;
    private final ValueProcessor<Optional<DialogLogic.Expression>> parsedExpressionValue;
    private final Updater updater;
    private final CheckerResultProcessor.Monitor monitor = new Monitor();

    public CheckerResultProcessor(
        ValueSource<Optional<DialogLogic.PreviewContext>> previewContextValue,
        ValueProcessor<Optional<DialogLogic.Expression>> parsedPropertyNameValue, Updater updater) {
      this.previewContextValue = previewContextValue;
      this.parsedExpressionValue = parsedPropertyNameValue;
      this.updater = updater;
    }

    @Override
    public String getValue() {
      synchronized (monitor) {
        return monitor.currentValue;
      }
    }

    ValueSource<String> getWarningSource() {
      return warningSource;
    }

    @Override
    public void update(Updater updater) {
      int requestNumber;
      synchronized (monitor) {
        requestNumber = ++monitor.requestId;
      }
      String newValue = calculateValue(requestNumber);
      if (newValue == null) {
        return;
      }
      synchronized (monitor) {
        monitor.currentValue = newValue;
      }
      updater.reportChanged(this);
    }

    /**
     * @return immediately calculated value or null if async calculation has been started
     */
    private String calculateValue(final int requestNumber) {
      Optional<DialogLogic.PreviewContext> evaluatorOptional = previewContextValue.getValue();
      if (!evaluatorOptional.isNormal()) {
        return ""; //$NON-NLS-1$
      }
      DialogLogic.PreviewContext previewContext = evaluatorOptional.getNormal();
      Optional<DialogLogic.Expression> expressionOptional = parsedExpressionValue.getValue();
      if (!expressionOptional.isNormal()) {
        return ""; //$NON-NLS-1$
      }
      DialogLogic.Expression expression = expressionOptional.getNormal();
      Expression.PreviewCallback callback = new Expression.PreviewCallback() {
        @Override
        public void done(String message, String additionalWarning) {
          synchronized (monitor) {
            if (monitor.requestId != requestNumber) {
              return;
            }
            monitor.currentValue = message;
            monitor.additionalWarning = additionalWarning;

            updater.reportChanged(CheckerResultProcessor.this);
            updater.reportChanged(warningSource);
            updater.updateAsync();
          }
        }
      };
      expression.doPreview(previewContext, callback);
      return null;
    }

    private static class Monitor {
      String additionalWarning = null;
      int requestId = 1;
      String currentValue = ""; //$NON-NLS-1$
    }

    private final ValueSource<String> warningSource = new ValueSource<String>() {
      @Override
      public String getValue() {
        synchronized (monitor) {
          return monitor.additionalWarning;
        }
      }
    };
  }

  /**
   * Parsed property expression. The simplest expression could be ".foo", but implementation
   * may support more sophisticated expressions as well.
   * The implementation is responsible for setting or previewing the property within
   * a provided {@link PreviewContext}. It also returns string representation of the property
   * for the watch expression.
   */
  private static abstract class Expression {
    protected abstract void doPreview(DialogLogic.PreviewContext previewContext,
        PreviewCallback callback);

    interface PreviewCallback {
      void done(String message, String additionalWarning);
    }

    protected abstract void execute(DialogLogic.PreviewContext previewContext, SetCallback callback);

    interface SetCallback {
      void done(String errorMessage);
    }

    /**
     * @return expression string representation suitable for watch expression view.
     */
    protected abstract String getWatchExpression();
  }

  /**
   * Parses expression using {@link DotSeparatedExpression} class. More sophisticated
   * parsers could be introduced later.
   */
  private static Optional<DialogLogic.Expression> parseExpression(String string) {
    return DotSeparatedExpression.parse(string);
  }

  /**
   * A parser that expects expression to be in form of ".property1.property2 ... .propertyN".
   * If the intermediate objects do not exist, they will be created as necessary.
   * It may be not very accurate working as JavaScript lexer.
   */
  private static class DotSeparatedExpression extends DialogLogic.Expression {
    static Optional<DialogLogic.Expression> parse(String string) {
      if (string.length() == 0) {
        return createErrorOptional(new Message(Messages.LogicImpl_ENTER_EXPRESSION,
            MessagePriority.BLOCKING_PROBLEM));
      }

      List<String> parts = new ArrayList<String>();
      int pos = 0;
      while (true) {
        if (pos >= string.length() || string.charAt(pos) != '.') {
          return createErrorOptional(new Message(Messages.LogicImpl_DOT_EXPECTED,
              MessagePriority.BLOCKING_PROBLEM));
        }
        pos++;
        int partStartPos = pos;
        if (pos >= string.length()) {
          return createErrorOptional(
              new Message(Messages.LogicImpl_ENTER_AFTER_DOT, MessagePriority.BLOCKING_INFO));
        }
        if (!Character.isJavaIdentifierStart(string.codePointAt(pos))) {
          return createErrorOptional(new Message(Messages.LogicImpl_INVALID_COMPONENT_START,
              MessagePriority.BLOCKING_PROBLEM));
        }
        pos = string.offsetByCodePoints(pos, 1);
        while (pos < string.length() && string.charAt(pos) != '.') {
          if (!Character.isJavaIdentifierPart(string.codePointAt(pos))) {
            return createErrorOptional(new Message(Messages.LogicImpl_INVALID_COMPONENT_CHAR,
                MessagePriority.BLOCKING_PROBLEM));
          }
          pos = string.offsetByCodePoints(pos, 1);
        }
        parts.add(string.substring(partStartPos, pos));
        if (pos >= string.length()) {
          break;
        }
      }
      return DialogUtils.<DialogLogic.Expression>createOptional(new DotSeparatedExpression(parts));
    }

    private final List<String> parts;

    DotSeparatedExpression(List<String> parts) {
      this.parts = parts;
    }

    protected void doPreview(DialogLogic.PreviewContext previewContext,
        final PreviewCallback callback) {
      // Prepare evaluate expression.
      StringBuilder builder = new StringBuilder();
      builder.append("(function() {\n"); //$NON-NLS-1$

      // Get global object.
      builder.append("var t = (function GetGlobal(){return this;})();\n"); //$NON-NLS-1$

      // Iterate over property chain and check all intermediate objects.
      // '+' is used in the mix with 'append' (not very effective) to keep code readable.
      for (int i = 0; i < parts.size() - 1; i++) {
        builder.append("if (\"" + parts.get(i) + "\" in t) { t = t." + //$NON-NLS-1$ //$NON-NLS-2$
            parts.get(i) + "; } else { return '" + //$NON-NLS-1$
            freePropertyCode + "'; }\n"); //$NON-NLS-1$
        builder.append("if (t instanceof Object == false) { throw \"Property '" + //$NON-NLS-1$
            parts.get(i) + "' contains non-object\"}\n"); //$NON-NLS-1$
      }

      // Check last expression component.
      String lastComponent = parts.get(parts.size() - 1);
      builder.append("if (\"" + lastComponent + "\" in t) { return '" + //$NON-NLS-1$
          occupiedPropertyCode + //$NON-NLS-1$
          "' + String(t." + lastComponent + "); } else { return '" + //$NON-NLS-1$ //$NON-NLS-2$
          freePropertyCode + "'; }\n"); //$NON-NLS-1$
      builder.append("})()"); //$NON-NLS-1$

      String expression = builder.toString();

      // The expression returns string value or throws an exception.
      // It uses occupiedPropertyCode and freePropertyCode strings as special return values.
      // All other return values are treated as error message.
      EvaluateCallback evaluateCallback = new EvaluateCallback() {
        // A message that shows when preview encountered an exception. Since preview
        // is optional, the exception is reported as warning.
        private final String warningMessage =
            Messages.LogicImpl_WARNING_SEEMS_A_PROBLEM;

        @Override
        public void success(JsVariable variable) {
          String stringValue = variable.getValue().getValueString();
          String value;
          String warning;
          if (freePropertyCode.equals(stringValue)) {
            value = Messages.LogicImpl_PROPERTY_FREE;
            warning = null;
          } else if (stringValue.startsWith(occupiedPropertyCode)) {
            String additionalString = stringValue.substring(occupiedPropertyCode.length());
            value = Messages.LogicImpl_PROPERTY_WILL_BE_OVERWRITTEN + additionalString;
            warning = null;
          } else {
            value = stringValue;
            warning = warningMessage;
          }
          callback.done(value, warning);
        }

        @Override
        public void failure(String errorMessage) {
          callback.done(Messages.LogicImpl_PROBLEM_ON_REMOTE + errorMessage, warningMessage);
        }
      };
      previewContext.extension.evaluateAsync(previewContext.globalEvaluateContext, expression,
          null, evaluateCallback, null);
    }

    protected void execute(DialogLogic.PreviewContext previewContext, final SetCallback callback) {
      // Prepare evaluate expression.
      StringBuilder builder = new StringBuilder();
      builder.append("(function() {\n"); //$NON-NLS-1$

      // Get global object.
      builder.append("var t = (function GetGlobal(){return this;})();\n"); //$NON-NLS-1$

      // Iterate over property chain and check all intermediate objects.
      for (int i = 0; i < parts.size() - 1; i++) {
        // '+' is used in the mix with 'append' (not very effective) to keep code readable.
        builder.append("if (\"" + parts.get(i) + "\" in t) { t = t." + //$NON-NLS-1$
            parts.get(i) + //$NON-NLS-1$
            "; } else { t = (t." + parts.get(i) + " = {}); }\n"); //$NON-NLS-1$ //$NON-NLS-2$
      }

      // Add a magic variable to expression that would be backed up by a handle.
      String paramJsName = "__pinPointedValue"; //$NON-NLS-1$
      String lastComponent = parts.get(parts.size() - 1);
      builder.append("t." + lastComponent + " = " + //$NON-NLS-1$ //$NON-NLS-2$
          paramJsName + ";\n"); //$NON-NLS-1$
      builder.append("})()"); //$NON-NLS-1$

      String expression = builder.toString();
      EvaluateCallback evaluateCallback = new EvaluateCallback() {
        @Override
        public void success(JsVariable variable) {
          callback.done(null);
        }

        @Override
        public void failure(String errorMessage) {
          callback.done(errorMessage);
        }
      };
      previewContext.extension.evaluateAsync(previewContext.globalEvaluateContext, expression,
          Collections.singletonMap(paramJsName, previewContext.jsObject.getRefId()),
          evaluateCallback, null);
    }

    protected String getWatchExpression() {
      StringBuilder builder = new StringBuilder();
      for (String s : parts) {
        if (builder.length() != 0) {
          builder.append("."); //$NON-NLS-1$
        }
        builder.append(s);
      }
      return builder.toString();
    }

    private static final String freePropertyCode = "free"; //$NON-NLS-1$
    private static final String occupiedPropertyCode = "occupied"; //$NON-NLS-1$
  }
}
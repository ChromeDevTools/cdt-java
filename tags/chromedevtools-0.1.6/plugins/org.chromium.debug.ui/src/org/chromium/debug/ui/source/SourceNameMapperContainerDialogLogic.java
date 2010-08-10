// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.source;

import static org.chromium.debug.ui.DialogUtils.createErrorOptional;
import static org.chromium.debug.ui.DialogUtils.createOptional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.chromium.debug.ui.DialogUtils.ExpressionProcessor;
import org.chromium.debug.ui.DialogUtils.Message;
import org.chromium.debug.ui.DialogUtils.MessagePriority;
import org.chromium.debug.ui.DialogUtils.OkButtonControl;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.Scope;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueProcessor;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.chromium.debug.ui.source.SourceNameMapperContainerDialog.ConfigureButtonAction;
import org.chromium.debug.ui.source.SourceNameMapperContainerDialog.ContainerStatusGroup;
import org.chromium.debug.ui.source.SourceNameMapperContainerDialog.Elements;
import org.chromium.debug.ui.source.SourceNameMapperContainerDialog.PresetFieldValues;
import org.chromium.debug.ui.source.SourceNameMapperContainerDialog.Result;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;

/**
 * A separated logic of {@link SourceNameMapperContainerDialog}. It describes how data flows
 * from input elements to the OK button with all necessary asserts. Basically it only uses
 * {@link SourceNameMapperContainerDialog.Elements} interface from the dialog.
 */
abstract class SourceNameMapperContainerDialogLogic {
  abstract Result getResult();
  abstract void updateAll();

  static SourceNameMapperContainerDialogLogic create(
      final Elements elements, final ISourceLookupDirector director,
      final PresetFieldValues initialParams) {
    final Updater updater = new Updater();
    Scope rootScope = updater.rootScope();

    final List<ValueSource<String>> warningSources = new ArrayList<ValueSource<String>>(2);

    // Represents value entered as prefix.
    final ValueSource<String> prefixEditor = new ValueSource<String>() {
      public String getValue() {
        return elements.getPrefixField().getText();
      }
      {
        if (initialParams != null) {
          elements.getPrefixField().setText(initialParams.getPrefix());
        }
        final ValueSource<String> updatableThis = this;
        ModifyListener listener = new ModifyListener() {
          public void modifyText(ModifyEvent e) {
            updater.reportChanged(updatableThis);
            updater.update();
          }
        };
        elements.getPrefixField().addModifyListener(listener);
      }
    };
    updater.addSource(rootScope, prefixEditor);

    // Represents prefix value after it has been validated.
    final ValueProcessor<Optional<String>> prefixValue = new ExpressionProcessor<String>(
        Collections.<ValueSource<? extends Optional<?>>>emptyList()) {
      @Override
      protected Optional<String> calculateNormal() {
        String prefix = prefixEditor.getValue();
        Optional<String> result;
        if (prefix == null || prefix.length() == 0) {
          return createErrorOptional(new Message(
              Messages.SourceNameMapperContainerDialog_ENTER_PREFIX,
              MessagePriority.BLOCKING_INFO));
        } else {
          return createOptional(prefix);
        }
      }
    };
    updater.addSource(rootScope, prefixValue);
    updater.addConsumer(rootScope, prefixValue);
    updater.addDependency(prefixValue, prefixEditor);

    // Represents possible warning about prefix value having no trailing slash.
    ValueProcessor<String> noSlashWarning = new ValueProcessor<String>() {
      public void update(Updater updater) {
        Optional<String> prefix = prefixValue.getValue();
        String result;
        if (prefix.isNormal() && !prefix.getNormal().endsWith("/")) { //$NON-NLS-1$
          result = Messages.SourceNameMapperContainerDialog_PREFIX_NORMALLY_ENDS;
        } else {
          result = null;
        }
        setCurrentValue(result);
        updater.reportChanged(this);
      }
    };
    updater.addSource(rootScope, noSlashWarning);
    updater.addConsumer(rootScope, noSlashWarning);
    updater.addDependency(noSlashWarning, prefixValue);
    warningSources.add(noSlashWarning);

    // Represents prefix rule example printer.
    ValueConsumer prefixExample = new ValueConsumer() {
      public void update(Updater updater) {
        Optional<String> prefix = prefixValue.getValue();
        String line1;
        String line2;
        if (prefix.isNormal()) {
          String sampleFileName = Messages.SourceNameMapperContainerDialog_SAMPLE_FILE_NAME;
          line1 = NLS.bind(Messages.SourceNameMapperContainerDialog_EXAMPLE_1,
              prefix.getNormal() + sampleFileName);
          line2 = NLS.bind(Messages.SourceNameMapperContainerDialog_EXAMPLE_2, sampleFileName);
        } else {
          line1 = ""; //$NON-NLS-1$
          line2 = ""; //$NON-NLS-1$
        }
        elements.getPrefixExampleLine1Label().setText(line1);
        elements.getPrefixExampleLine2Label().setText(line2);
      }
    };
    updater.addConsumer(rootScope, prefixExample);
    updater.addDependency(prefixExample, prefixValue);

    // Represents container type combo box.
    final ValueSource<ISourceContainerType> selectedTypeValue =
        new ValueSource<ISourceContainerType>() {
      public ISourceContainerType getValue() {
        return elements.getContainerTypeCombo().getSelected();
      }
      {
        if (initialParams != null) {
          ISourceContainerType type = initialParams.getContainer().getType();
          elements.getContainerTypeCombo().setSelected(type);
        }
        final ValueSource<ISourceContainerType> updatableThis = this;
        SelectionListener listener = new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            updater.reportChanged(updatableThis);
            updater.update();
          }
        };
        elements.getContainerTypeCombo().addSelectionListener(listener);
      }
    };
    updater.addSource(rootScope, selectedTypeValue);

    // Represents "Configure" button that acts like a container factory.
    final ValueProcessor<ISourceContainer> containerFactoryButtonValue =
        new ValueProcessor<ISourceContainer>() {
      private ConfigureButtonAction preparedAction = null;
      {
        if (initialParams != null) {
          setCurrentValue(initialParams.getContainer());
        }
        final ValueSource<ISourceContainer> valueSourceThis = this;
        elements.getConfigureButton().addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (preparedAction != null) {
              ISourceContainer value = preparedAction.run(elements.getShell());
              if (value != null) {
                setCurrentValue(value);
              }
              updater.reportChanged(valueSourceThis);
              updater.update();
              updateAction();
            }
          }
        });
      }
      public void update(Updater updater) {
        if (getValue() != null && !getValue().getType().equals(selectedTypeValue.getValue())) {
          setCurrentValue(null);
          updater.reportChanged(this);
        }
        updateAction();
      }
      private void updateAction() {
        preparedAction = SourceNameMapperContainerDialog.prepareConfigureAction(
            selectedTypeValue.getValue(), getValue(), director);
        elements.getConfigureButton().setEnabled(preparedAction != null);
      }
    };
    updater.addSource(rootScope, containerFactoryButtonValue);
    updater.addConsumer(rootScope, containerFactoryButtonValue);
    updater.addDependency(containerFactoryButtonValue, selectedTypeValue);

    // Represents printer that shows type and name of the created container.
    ValueConsumer showContainerTypeValue = new ValueConsumer() {
      public void update(Updater updater) {
        ISourceContainer container = containerFactoryButtonValue.getValue();
        String status;
        Image image;
        String name;
        boolean enabled;
        if (container == null) {
          status = Messages.SourceNameMapperContainerDialog_NOTHING_CONFIGURED;
          name = ""; //$NON-NLS-1$
          image = null;
          enabled = false;
        } else {
          status = Messages.SourceNameMapperContainerDialog_CONFIGURED_CONTAINER;
          ISourceContainerType type = container.getType();
          name = container.getName();
          image = DebugUITools.getSourceContainerImage(type.getId());
          enabled = true;
        }
        ContainerStatusGroup group = elements.getContainerStatusGroup();
        group.getStatusLabel().setText(status);
        group.getTypeImageLabel().setImage(image);
        group.getContainerNameLabel().setText(name);
        group.setEnabled(enabled);
        group.layout();
      }
    };
    updater.addConsumer(rootScope, showContainerTypeValue);
    updater.addDependency(showContainerTypeValue, containerFactoryButtonValue);

    // Represents expression that constructs dialog window result.
    final ValueProcessor<? extends Optional<Result>> resultValue =
        new ExpressionProcessor<Result>(
            Arrays.<ValueSource<? extends Optional<?>>>asList(prefixValue) ) {
          @Override
          protected Optional<Result> calculateNormal() {
            final String prefix = prefixValue.getValue().getNormal();
            final ISourceContainer container = containerFactoryButtonValue.getValue();
            if (container == null) {
              return createErrorOptional(
                  new Message(Messages.SourceNameMapperContainerDialog_CONFIGURE_TARGET_CONTAINER,
                      MessagePriority.BLOCKING_INFO));
            }
            Result result = new Result() {
              public ISourceContainer getResultContainer() {
                return container;
              }
              public String getResultPrefix() {
                return prefix;
              }
            };
            return createOptional(result);
          }
    };
    updater.addSource(rootScope, resultValue);
    updater.addConsumer(rootScope, resultValue);
    updater.addDependency(resultValue, prefixValue);
    updater.addDependency(resultValue, containerFactoryButtonValue);

    // Represents controller that updates state of OK button and error messages.
    OkButtonControl okButtonControl = new OkButtonControl(resultValue, warningSources, elements);
    updater.addConsumer(rootScope, okButtonControl);
    updater.addDependency(okButtonControl, okButtonControl.getDependencies());

    return new SourceNameMapperContainerDialogLogic() {
      @Override
      Result getResult() {
        Optional<Result> optional = resultValue.getValue();
        if (optional.isNormal()) {
          return optional.getNormal();
        } else {
          // Normally should not be reachable, because UI should have disabled OK button.
          return null;
        }
      }
      @Override
      void updateAll() {
        updater.updateAll();
      }
    };
  }
}

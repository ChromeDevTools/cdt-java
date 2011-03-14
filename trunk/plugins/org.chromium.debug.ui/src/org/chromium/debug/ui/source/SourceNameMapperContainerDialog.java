// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.chromium.debug.ui.DialogUtils.ComboWrapper;
import org.chromium.debug.ui.DialogUtils.OkButtonElements;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A dialog for adding and editing JavaScript source name mapper containers.
 */
public class SourceNameMapperContainerDialog extends TitleAreaDialog {
  private final ISourceLookupDirector director;
  private final PresetFieldValues initialParams;

  private Result result = null;
  private SourceNameMapperContainerDialogLogic logic = null;

  /**
   * An optional set of preset dialog field values. Useful in "edit" (not "add") mode of dialog.
   */
  public interface PresetFieldValues {
    String getPrefix();
    ISourceContainer getContainer();
  }

  public interface Result {
    String getResultPrefix();
    ISourceContainer getResultContainer();
  }

  public Result getResult() {
    return result;
  }

  public SourceNameMapperContainerDialog(Shell shell, ISourceLookupDirector director,
    PresetFieldValues initialParams) {
    super(shell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.director = director;
    this.initialParams = initialParams;
  }

  @Override
  protected Control createDialogArea(Composite ancestor) {
    getShell().setText(Messages.SourceNameMapperContainerDialog_DIALOG_TITLE);
    setTitle(Messages.SourceNameMapperContainerDialog_DIALOG_SUBTITLE);

    Composite parent = new Composite(ancestor, SWT.NULL);
    {
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 1;
      parent.setLayout(topLayout);
      parent.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    Label explanationOne = new Label(parent, 0);
    explanationOne.setText(
        Messages.SourceNameMapperContainerDialog_EXPLANATION_1);

    Group prefixGroup = new Group(parent, SWT.NONE);
    prefixGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    prefixGroup.setText(Messages.SourceNameMapperContainerDialog_PREFIX_GROUP);
    prefixGroup.setLayout(new GridLayout(1, false));
    final Text prefixEditor = new Text(prefixGroup, SWT.SINGLE | SWT.BORDER);
    prefixEditor.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    final Label prefixExampleLine1Label = new Label(prefixGroup, 0);
    prefixExampleLine1Label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    final Label prefixExampleLine2Label = new Label(prefixGroup, 0);
    prefixExampleLine2Label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label explanationTwo = new Label(parent, 0);
    explanationTwo.setText(Messages.SourceNameMapperContainerDialog_EXPLANATION_2);

    Group containerGroup = new Group(parent, SWT.NONE);
    containerGroup.setLayout(new GridLayout(1, false));
    containerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    containerGroup.setText(Messages.SourceNameMapperContainerDialog_CONTAINER_GROUP);

    Composite typeBlock = new Composite(containerGroup, SWT.NULL);
    typeBlock.setLayout(new GridLayout(3, false));
    typeBlock.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final List<ISourceContainerType> types =
        filterTypes(DebugPlugin.getDefault().getLaunchManager().getSourceContainerTypes());

    Collections.sort(types, TYPE_COMPARATOR_BY_NAME);

    String[] typeNameArray = new String[types.size()];
    for (int i = 0; i < typeNameArray.length; i++) {
      typeNameArray[i] = types.get(i).getName();
    }

    Label comboLabel = new Label(typeBlock, 0);
    comboLabel.setText(Messages.SourceNameMapperContainerDialog_TYPE_LABEL);

    Combo typesCombo = new Combo(typeBlock, SWT.READ_ONLY);
    typesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    typesCombo.setFont(parent.getFont());
    typesCombo.setItems(typeNameArray);
    if (typeNameArray.length > 0) {
      typesCombo.select(0);
    }
    final Button configureButton = new Button(typeBlock, SWT.PUSH);
    configureButton.setText(Messages.SourceNameMapperContainerDialog_CONFIGURE_BUTTON);

    final Composite statusBox = new Composite(containerGroup, SWT.NULL);
    statusBox.setLayout(new GridLayout(3, false));
    statusBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Label statusLabel = new Label(statusBox, 0);
    final Label containerTypeIconLabel = new Label(statusBox, 0);
    final Label containerNameLabel = new Label(statusBox, 0);

    Dialog.applyDialogFont(parent);

    // Implementing Elements interface
    final ComboWrapper<ISourceContainerType> comboWrapper =
        new ComboWrapper<ISourceContainerType>(typesCombo) {
      @Override
      public ISourceContainerType getSelected() {
        return types.get(getCombo().getSelectionIndex());
      }
      @Override
      public void setSelected(ISourceContainerType element) {
        int index = types.indexOf(element);
        if (index != -1) {
          getCombo().select(index);
        }
      }
    };

    final ContainerStatusGroup containerStatusGroup = new ContainerStatusGroup() {
      public Label getStatusLabel() {
        return statusLabel;
      }
      public Label getTypeImageLabel() {
        return containerTypeIconLabel;
      }
      public Label getContainerNameLabel() {
        return containerNameLabel;
      }
      public void layout() {
        statusBox.layout();
      }
      public void setEnabled(boolean enabled) {
        statusLabel.setEnabled(enabled);
        containerTypeIconLabel.setEnabled(enabled);
        containerNameLabel.setEnabled(enabled);
      }
    };

    Elements elements = new Elements() {
      public Text getPrefixField() {
        return prefixEditor;
      }
      public Label getPrefixExampleLine1Label() {
        return prefixExampleLine1Label;
      }
      public Label getPrefixExampleLine2Label() {
        return prefixExampleLine2Label;
      }
      public Button getConfigureButton() {
        return configureButton;
      }
      public ComboWrapper<ISourceContainerType> getContainerTypeCombo() {
        return comboWrapper;
      }
      public Shell getShell() {
        return SourceNameMapperContainerDialog.this.getShell();
      }
      public ContainerStatusGroup getContainerStatusGroup() {
        return containerStatusGroup;
      }
      public Button getOkButton() {
        return SourceNameMapperContainerDialog.this.getButton(IDialogConstants.OK_ID);
      }
      public void setMessage(String message, int type) {
        SourceNameMapperContainerDialog.this.setMessage(message, type);
      }
    };

    logic = SourceNameMapperContainerDialogLogic.create(elements, director, initialParams);

    return parent;
  }

  @Override
  public void create() {
    super.create();
    logic.updateAll();
  }

  @Override
  protected void okPressed() {
    result = logic.getResult();
    super.okPressed();
  }

  /**
   * A main interface to dialog elements, that are used from logic engine.
   */
  interface Elements extends OkButtonElements {
    Text getPrefixField();
    Label getPrefixExampleLine1Label();
    Label getPrefixExampleLine2Label();
    ComboWrapper<ISourceContainerType> getContainerTypeCombo();
    Button getConfigureButton();
    ContainerStatusGroup getContainerStatusGroup();

    Shell getShell();
  }

  interface ContainerStatusGroup {
    Label getStatusLabel();
    Label getTypeImageLabel();
    Label getContainerNameLabel();
    void layout();
    void setEnabled(boolean enabled);
  }

  interface ConfigureButtonAction {
    ISourceContainer run(Shell shell);
  }

  // Creates action implementation for a configure button or return null.
  static ConfigureButtonAction prepareConfigureAction(ISourceContainerType type,
      ISourceContainer alreadyCreatedContainer,
      final ISourceLookupDirector director) {
    if (type == null) {
      return null;
    }
    final ISourceContainerBrowser browser = DebugUITools.getSourceContainerBrowser(type.getId());
    if (browser == null) {
      return null;
    }
    abstract class ActionBase implements ConfigureButtonAction {
      public ISourceContainer run(Shell shell) {
        ISourceContainer[] containers = runImpl(shell);
        if (containers.length != 1) {
          return null;
        }
        return containers[0];
      }
      abstract ISourceContainer[] runImpl(Shell shell);
    }
    ISourceContainer[] containers;
    if (alreadyCreatedContainer != null && alreadyCreatedContainer.getType().equals(type)) {
      // Edit existing.
      final ISourceContainer[] alreadyCreatedContainerArray = { alreadyCreatedContainer };
      if (browser.canEditSourceContainers(director, alreadyCreatedContainerArray)) {
        return new ActionBase() {
          @Override ISourceContainer[] runImpl(Shell shell) {
            return browser.editSourceContainers(shell, director, alreadyCreatedContainerArray);
          }
        };
      }
    }
    // Add new.
    if (browser.canAddSourceContainers(director)) {
      return new ActionBase() {
        @Override ISourceContainer[] runImpl(Shell shell) {
          return browser.addSourceContainers(shell, director);
        }
      };
    }
    return null;
  }

  private static final Comparator<ISourceContainerType> TYPE_COMPARATOR_BY_NAME =
      new Comparator<ISourceContainerType>() {
    public int compare(ISourceContainerType o1, ISourceContainerType o2) {
      return o1.getName().compareTo(o2.getName());
    }
  };

  private List<ISourceContainerType> filterTypes(ISourceContainerType[] types){
    ArrayList<ISourceContainerType> result = new ArrayList<ISourceContainerType>();
    for (int i = 0; i< types.length; i++) {
      ISourceContainerType type = types[i];
      if (director.supportsSourceContainerType(type)) {
        ISourceContainerBrowser sourceContainerBrowser =
            DebugUITools.getSourceContainerBrowser(type.getId());
        if(sourceContainerBrowser != null &&
            sourceContainerBrowser.canAddSourceContainers(director)) {
          result.add(type);
        }
      }
    }
    return result;
  }
}

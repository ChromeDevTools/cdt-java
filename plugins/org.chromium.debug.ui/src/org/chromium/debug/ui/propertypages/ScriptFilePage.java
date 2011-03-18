// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.propertypages;

import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.ChromiumJavascriptDecorator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A ui for properties of JS file. Both Virtual project and a regular workspace
 * files (.js or .html) are supported.
 */
public class ScriptFilePage extends PropertyPage {

  @Override
  protected Control createContents(Composite parent) {

    IAdaptable adaptable = this.getElement();
    IFile file = (IFile) adaptable.getAdapter(IFile.class);

    List<? extends ScriptTargetMapping> mappingList =
        ChromiumDebugPlugin.getScriptTargetMapping(file);

    if (mappingList.size() == 1 && isVProjectFile(file, mappingList.get(0))) {
      return buildVProjectFileUi(mappingList.get(0), parent);
    } else {
      return buildWorkspaceFileUi(mappingList, parent);
    }
  }

  private boolean isVProjectFile(IFile file, ScriptTargetMapping mapping) {
    IProject project = file.getProject();
    if (project == null) {
      return false;
    }
    boolean hasNature;
    try {
      hasNature = project.hasNature(ChromiumDebugPluginUtil.JS_DEBUG_PROJECT_NATURE);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }
    // TODO(peter.rybin): here we can check that mapping refers to the same project.
    return hasNature;
  }

  private Control buildWorkspaceFileUi(final List<? extends ScriptTargetMapping> mappingList,
      Composite parent) {
    Composite main = new Composite(parent, SWT.NONE);
    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 1;
    main.setLayout(topLayout);
    main.setLayoutData(new GridData(GridData.FILL_BOTH));

    new Label(main, SWT.NONE).setText(Messages.ScriptFilePage_CURRENTLY_LINKED_TO_LABEL);

    String[] launchLabels = new String[mappingList.size()];
    for (int i = 0; i < launchLabels.length; i++) {
      DebugTargetImpl debugTarget = mappingList.get(i).getRunningTargetData().getDebugTarget();
      launchLabels[i] = debugTarget.getLaunch().getLaunchConfiguration().getName();
    }

    final Combo typesCombo = new Combo(main, SWT.READ_ONLY);
    typesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    typesCombo.setFont(parent.getFont());
    typesCombo.setItems(launchLabels);
    if (launchLabels.length > 0) {
      typesCombo.select(0);

      final ScriptProperties properties = buildScriptProperties(main);
      fillScriptProperties(properties, mappingList.get(0));

      typesCombo.addSelectionListener(new SelectionListener() {
        public void widgetDefaultSelected(SelectionEvent e) {
        }
        public void widgetSelected(SelectionEvent e) {
          int index = typesCombo.getSelectionIndex();
          fillScriptProperties(properties, mappingList.get(index));
        }
      });
    }

    return main;
  }

  private Control buildVProjectFileUi(ScriptTargetMapping mapping, Composite parent) {
    Composite main = new Composite(parent, SWT.NONE);
    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 1;
    main.setLayout(topLayout);
    main.setLayoutData(new GridData(GridData.FILL_BOTH));
    ScriptProperties properties = buildScriptProperties(main);

    fillScriptProperties(properties, mapping);

    return main;
  }

  private void fillScriptProperties(ScriptProperties properties, ScriptTargetMapping input) {
    IFile vprojectFile = input.getFile();
    String fileName =
        ChromiumJavascriptDecorator.getDecoratedText(vprojectFile.getName(), vprojectFile);
    properties.getLocalFileName().setText(fileName);

    properties.getScriptName().setText(input.getVmResource().getId().getEclipseSourceName());
  }

  private interface ScriptProperties {
    Text getScriptName();
    Text getLocalFileName();
  }

  private ScriptProperties buildScriptProperties(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout topLayout = new GridLayout();
    topLayout.numColumns = 2;
    composite.setLayout(topLayout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Text scriptName = createTextField(composite, Messages.ScriptFilePage_SCRIPT_NAME);
    final Text scriptFile = createTextField(composite, Messages.ScriptFilePage_NAME_IN_VPROJECT);

    return new ScriptProperties() {
      public Text getScriptName() {
        return scriptName;
      }
      public Text getLocalFileName() {
        return scriptFile;
      }
    };
  }

  private static Text createTextField(Composite parent, String title) {
    Label label = new Label(parent, SWT.NONE);
    label.setText(title);
    GridData gd;
    gd = new GridData();
    gd.verticalAlignment = SWT.TOP;
    label.setLayoutData(gd);

    Text valueText = new Text(parent, SWT.WRAP | SWT.READ_ONLY);
    gd = new GridData();
    gd.grabExcessHorizontalSpace = true;
    gd.horizontalAlignment = GridData.FILL;
    valueText.setLayoutData(gd);
    Display display = parent.getDisplay();
    valueText.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    return valueText;
  }
}

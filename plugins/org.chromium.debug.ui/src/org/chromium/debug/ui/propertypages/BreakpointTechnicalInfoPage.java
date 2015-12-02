// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.propertypages;

import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.model.ChromiumLineBreakpoint;
import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.WorkspaceBridge;
import org.chromium.debug.core.model.WorkspaceBridge.BreakpointHandler;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.sdk.Breakpoint;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * A JavaScript breakpoint technical info property page.
 * Shows how UI breakpoint projected into a particular debug target/launch.
 */
public class BreakpointTechnicalInfoPage extends PropertyPage {
  @Override
  protected Control createContents(Composite parent) {
    noDefaultAndApplyButton();

    ChromiumLineBreakpoint breakpoint = getBreakpoint();

    Composite mainComposite = JsLineBreakpointPage.createComposite(parent, 1, 1);
    createTechnicalInfoControls(mainComposite, breakpoint);
    setValid(true);
    return mainComposite;
  }

  private static class TargetInfo {
    final ConnectedTargetData connectedTargetData;
    final Breakpoint sdkBreakpoint;

    TargetInfo(ConnectedTargetData connectedTargetData, Breakpoint sdkBreakpoint) {
      this.connectedTargetData = connectedTargetData;
      this.sdkBreakpoint = sdkBreakpoint;
    }
  }

  private void createTechnicalInfoControls(final Composite parent,
      ChromiumLineBreakpoint breakpoint) {
    final List<TargetInfo> list = new ArrayList<TargetInfo>();
    for (ConnectedTargetData connected : DebugTargetImpl.getAllConnectedTargetDatas()) {
      WorkspaceBridge workspaceRelations = connected.getWorkspaceRelations();
      BreakpointHandler breakpointHandler = workspaceRelations.getBreakpointHandler();
      Breakpoint sdkBreakpoint = breakpointHandler.getSdkBreakpoint(breakpoint);
      list.add(new TargetInfo(connected, sdkBreakpoint));
    }

    new Label(parent, SWT.NONE).setText(Messages.BreakpointTechnicalInfoPage_CHOOSE_LAUNCH);

    String[] launchLabels = new String[list.size()];
    for (int i = 0; i < launchLabels.length; i++) {
      TargetInfo targetInfo = list.get(i);
      DebugTargetImpl debugTarget = targetInfo.connectedTargetData.getDebugTarget();
      launchLabels[i] = debugTarget.getLaunch().getLaunchConfiguration().getName();
    }

    final Combo typesCombo = new Combo(parent, SWT.READ_ONLY);
    typesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    typesCombo.setFont(parent.getFont());
    typesCombo.setItems(launchLabels);
    if (launchLabels.length == 0) {
      return;
    }

    typesCombo.select(0);

    // Padding.
    new Label(parent, SWT.NONE);

    class BreakpointProperties {
      final Text text;
      {
        Composite c = JsLineBreakpointPage.createComposite(parent, 2, 1);
        text = ScriptFilePage.createTextField(c, Messages.BreakpointTechnicalInfoPage_TARGET);
      }
      void fillIn(int pos) {
        TargetInfo info = list.get(pos);
        String value;
        if (info.sdkBreakpoint == null) {
          value = Messages.BreakpointTechnicalInfoPage_NOT_SET;
        } else {
          Breakpoint.Target target = info.sdkBreakpoint.getTarget();
          value = target.accept(ChromiumDebugPluginUtil.BREAKPOINT_TARGET_TO_STRING);
        }
        text.setText(value);
      }
    }

    final BreakpointProperties breakpointProperties = new BreakpointProperties();
    breakpointProperties.fillIn(0);

    typesCombo.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
      }
      public void widgetSelected(SelectionEvent e) {
        int index = typesCombo.getSelectionIndex();
        breakpointProperties.fillIn(index);
      }
    });
  }

  private ChromiumLineBreakpoint getBreakpoint() {
    IAdapterManager manager= Platform.getAdapterManager();
    IAdaptable adaptable = getElement();
    ChromiumLineBreakpoint adapted =
        (ChromiumLineBreakpoint) manager.getAdapter(adaptable, ChromiumLineBreakpoint.class);
    return adapted;
  }
}

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.launcher;

import java.net.MalformedURLException;
import java.net.URL;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.ui.PluginUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * The "Deprecation" tab for the Chromium JavaScript launch tab group. It contains
 * text explanation and a button that copies current configuration as WIP launch configuration.
 */
public class DevToolsProtocolDeprecationTab extends AbstractLaunchConfigurationTab {
  private ILaunchConfiguration launchConfiguration = null;
  private final ILaunchConfigurationType WIP_CONFIGURATION_TYPE =
      DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(WIP_TYPE_ID);

  @Override
  public String getName() {
    return Messages.DevToolsProtocolDeprecationTab_TITLE;
  }

  @Override
  public void createControl(Composite parent) {
    Composite composite = ChromiumRemoteTab.createDefaultComposite(parent);
    setControl(composite);

    FontMetrics fontMetrics = PluginUtil.getFontMetrics(composite, null);

    createVerticalSpan(composite, fontMetrics.getHeight());

    Text text = new Text(composite, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);

    text.setText(NLS.bind(Messages.DevToolsProtocolDeprecationTab_MAIN_TEXT, WIP_CONFIGURATION_TYPE.getName()));

    text.setBackground(parent.getBackground());

    {
      GridData gd = new GridData();
      gd.horizontalAlignment = GridData.FILL;
      gd.grabExcessHorizontalSpace = true;
      gd.widthHint = fontMetrics.getAverageCharWidth() * 44;
      text.setLayoutData(gd);
    }

    Button button = new Button(composite, SWT.PUSH);
    button.setText(Messages.DevToolsProtocolDeprecationTab_COPY_LAUNCH_CONFIGURATION);
    button.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        try {
          performCopying(launchConfiguration);
        } catch (CoreException e) {
          ChromiumDebugPlugin.log(new Exception("Failed to copy launch configuration", e)); //$NON-NLS-1$
        }
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
      }
    });

    createVerticalSpan(composite, fontMetrics.getHeight());

    Label seeAlso = new Label(composite, SWT.NONE);
    seeAlso.setText(Messages.DevToolsProtocolDeprecationTab_SEE_ALSO);

    createLink(composite, Messages.DevToolsProtocolDeprecationTab_OLD_PROTOCOL,
        "http://code.google.com/p/chromedevtools/wiki/ChromeDevToolsProtocol"); //$NON-NLS-1$
    createLink(composite, Messages.DevToolsProtocolDeprecationTab_NEW_PROTOCOL,
        "http://code.google.com/p/chromedevtools/wiki/WIP"); //$NON-NLS-1$
    createLink(composite, Messages.DevToolsProtocolDeprecationTab_PROJECT_SITE,
        "http://code.google.com/p/chromedevtools/"); //$NON-NLS-1$
  }

  private Label createVerticalSpan(Composite parent, int height) {
    Label spanLabel = new Label(parent, SWT.NONE);
    GridData gd = new GridData();
    gd.minimumHeight = height;
    spanLabel.setLayoutData(gd);
    return spanLabel;
  }

  private Link createLink(Composite parent, String text, String urlString) {
    final URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
    Link link = new Link(parent, SWT.NONE);
    link.setText(text);
    link.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
        try {
            IWebBrowser browser = support.getExternalBrowser();
            browser.openURL(url);
        } catch (PartInitException e) {
            ChromiumDebugPlugin.log(e);
        }
      }
    });
    return link;
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
  }

  @Override
  public void initializeFrom(ILaunchConfiguration configuration) {
    launchConfiguration = configuration;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
  }

  private ILaunchConfiguration performCopying(ILaunchConfiguration original) throws CoreException {
    IContainer container;
    IFile file = original.getFile();
    if (file == null) {
      container = null;
    } else {
      container = file.getParent();
    }

    ILaunchConfigurationWorkingCopy result =
        WIP_CONFIGURATION_TYPE.newInstance(container, original.getName() + Messages.DevToolsProtocolDeprecationTab_CONFIGURATION_NAME_SUFFIX);

    result.setAttributes(original.getAttributes());

    return result.doSave();
  }

  private static final String WIP_TYPE_ID = "org.chromium.debug.ui.LaunchType$Wip"; //$NON-NLS-1$

}

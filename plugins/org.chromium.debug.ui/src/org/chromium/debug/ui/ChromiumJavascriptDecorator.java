// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

/**
 * A decorator that removes the ".chromium" file extension
 * in the Project Explorer for the Debug Javascript project files.
 */
public class ChromiumJavascriptDecorator implements ILabelDecorator {

  public Image decorateImage(Image image, Object element) {
    return image;
  }

  public String decorateText(String text, Object element) {
    // (element instanceof IFile) is guaranteed by the enablement in plugin.xml
    return getDecoratedText(text, element);
  }

  /**
   * @param text the original label of the element
   * @param element must be an IFile instance
   * @return a decorated element label, or the original one if the label
   *         need not be decorated or there was a CoreException reading
   *         the element's project natures
   */
  public static String getDecoratedText(String text, Object element) {
    if (PluginUtil.isChromiumDebugFile((IFile) element)) {
      return PluginUtil.stripChromiumExtension(text, false);
    }
    return text;
  }

  public void addListener(ILabelProviderListener listener) {
  }

  public void dispose() {
  }

  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  public void removeListener(ILabelProviderListener listener) {
  }
}

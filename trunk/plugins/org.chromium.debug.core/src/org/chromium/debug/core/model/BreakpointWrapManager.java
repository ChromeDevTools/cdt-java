// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Stores all available breakpoint adapters registered in Eclipse extension registry.
 */
public class BreakpointWrapManager {
  private static final String BREAKPOINT_ADAPTER_ELEMENT_NAME = "breakpointAdapter";
  private static final String CLASS_PROPERTY = "class";

  private final List<JavaScriptBreakpointAdapter> adapterList;

  public BreakpointWrapManager() {
    IExtensionPoint extensionPoint = RegistryFactory.getRegistry().getExtensionPoint(
        JavaScriptBreakpointAdapter.EXTENSION_POINT_ID);
    IExtension[] extensions = extensionPoint.getExtensions();

    ArrayList<JavaScriptBreakpointAdapter> result =
        new ArrayList<JavaScriptBreakpointAdapter>(extensions.length);

    for (IExtension extension : extensions) {
      for (IConfigurationElement element : extension.getConfigurationElements()) {
        if (!BREAKPOINT_ADAPTER_ELEMENT_NAME.equals(element.getName())) {
          continue;
        }
        Object obj;
        try {
          obj = element.createExecutableExtension(CLASS_PROPERTY);
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
        JavaScriptBreakpointAdapter adapter = (JavaScriptBreakpointAdapter) obj;
        result.add(adapter);
      }
    }
    adapterList = result;
  }

  /**
   * Tries to wrap a breakpoint by finding an applicable adapter.
   * @return wrapped breakpoint or null
   */
  public WrappedBreakpoint wrap(IBreakpoint breakpoint) {
    for (JavaScriptBreakpointAdapter adapter : adapterList) {
      WrappedBreakpoint result = adapter.tryWrapBreakpoint(breakpoint);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * @return all breakpoint adapters registered in Eclipse
   */
  public Collection<? extends JavaScriptBreakpointAdapter> getAdapters() {
    return adapterList;
  }
}

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.wip.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.chromium.sdk.wip.WipBackend;

/**
 * A helper class that fetches all available implementations of {@link WipBackend} using Eclipse
 * framework.
 */
public class BackendRegistry {
  public static final BackendRegistry INSTANCE = new BackendRegistry();

  public List<? extends WipBackend> getBackends() {
    IExtensionPoint extensionPoint = RegistryFactory.getRegistry().getExtensionPoint(
        WipBackExtensionPoint.ID);
    IExtension[] extensions = extensionPoint.getExtensions();

    List<WipBackend> result = new ArrayList<WipBackend>(extensions.length);

    for (IExtension extension : extensions) {
      for (IConfigurationElement element : extension.getConfigurationElements()) {
        if (!WipBackExtensionPoint.ELEMENT_NAME.equals(element.getName())) {
          continue;
        }
        Object obj;
        try {
          obj = element.createExecutableExtension(WipBackExtensionPoint.CLASS_PROPERTY);
        } catch (CoreException e) {
          throw new RuntimeException(e);
        }
        WipBackend backend = (WipBackend) obj;
        result.add(backend);
      }
    }
    return result;
  }
}

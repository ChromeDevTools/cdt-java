// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;

/**
 * Provides a collection of source wrappers. They are predefined and immutable.
 */
public interface IPredefinedSourceWrapProvider {
  Collection<Entry> getWrappers();

  /**
   * Describes source wrapper.
   */
  abstract class Entry {
    private final String id;
    private final SourceWrapSupport.Wrapper wrapper;

    public Entry(String id, SourceWrapSupport.Wrapper wrapper) {
      this.id = id;
      this.wrapper = wrapper;
    }

    /**
     * @return a unique id of the source wrap entry (fully qualified name is recommended)
     */
    public String getId() {
      return id;
    }

    /**
     * @return an instance of source wrapper
     */
    public SourceWrapSupport.Wrapper getWrapper() {
      return wrapper;
    }

    /**
     * @return a human-readable wrapper description that can be used in UI
     */
    public abstract String getHumanDescription();
  }

  /**
   * Provides a standard mean of accessing instance of this interface. It uses
   * Eclipse/OSGI extension registry.
   */
  class Access {
    public static final String EXTENSION_POINT_ID =
        "org.chromium.debug.core.model_IPredefinedSourceWrapProvider";
    public static final String ELEMENT_NAME = "wrap-provider";
    public static final String CLASS_PROPERTY = "class";

    /**
     * @return all entries from all providers in for of id-to-entry map
     */
    public static Map<String, Entry> getEntries() {
      return ENTRY_MAP;
    }

    private static final Map<String, Entry> ENTRY_MAP;
    static {
      ENTRY_MAP = new HashMap<String, Entry>();
      for (IPredefinedSourceWrapProvider provider : getProviders()) {
        for (IPredefinedSourceWrapProvider.Entry entry : provider.getWrappers()) {
          ENTRY_MAP.put(entry.getId(), entry);
        }
      }
    }

    /**
   * Provides a standard mean of getting an instances of interface. It uses
   * Eclipse/OSGI extension registry.
     */
    private static Collection<IPredefinedSourceWrapProvider> getProviders() {
      List<IPredefinedSourceWrapProvider> result = new ArrayList<IPredefinedSourceWrapProvider>();
      IExtensionPoint extensionPoint = RegistryFactory.getRegistry().getExtensionPoint(
          EXTENSION_POINT_ID);
      IExtension[] extensions = extensionPoint.getExtensions();

      for (IExtension extension : extensions) {
        for (IConfigurationElement element : extension.getConfigurationElements()) {
          if (!ELEMENT_NAME.equals(element.getName())) {
            continue;
          }
          Object obj;
          try {
            obj = element.createExecutableExtension(CLASS_PROPERTY);
          } catch (CoreException e) {
            throw new RuntimeException(e);
          }
          IPredefinedSourceWrapProvider provider = (IPredefinedSourceWrapProvider) obj;
          result.add(provider);
        }
      }
      return result;
    }
  }
}

// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;

/**
 * Provides a format operation for JavaScript source text. Result is optimized for
 * future position translation operation.
 * <p>
 * A particular implementation of adapter should be registered as an extension.
 */
public interface JavaScriptFormatter {

  Result format(String sourceString);

  /**
   * Represents formatting result. It contains a formatted text and a mapping
   * between original and formatted versions.
   */
  interface Result {
    String getFormattedText();

    StringMappingData getInputTextData();
    StringMappingData getFormattedTextData();
  }

  /**
   * Provides a standard mean of getting a single implementation of this interface. It uses
   * Eclipse/OSGI extension registry.
   */
  class Access {
    public static final String EXTENSION_POINT_ID =
        "org.chromium.debug.core.model_JavaScriptFormatter";
    public static final String ELEMENT_NAME = "formatter";
    public static final String CLASS_PROPERTY = "class";

    /**
     * @return an instance of (any random) implementation of {@link JavaScriptFormatter} or null
     */
    public static JavaScriptFormatter getInstance() {
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
          return (JavaScriptFormatter) obj;
        }
      }
      return null;
    }
  }
}

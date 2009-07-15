// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

import org.chromium.sdk.JsArray;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.JsValue.Type;

/**
 * A converter of JsValues into human-readable strings used in various contexts.
 */
public class JsValueStringifier {

  /**
   * A configuration class
   */
  public static class Config {

    /**
     * The maximum length of the text to render. If the value length exceeds
     * the limit, an ellipsis will be used to truncate the value.
     * The default is 80 characters.
     */
    public int maxLength = 80;
  }

  private static final String ELLIPSIS = "..."; //$NON-NLS-1$

  private static final String UNKNOWN_VALUE = "<null>"; //$NON-NLS-1$

  private final Config config;

  /**
   * Use the default config values.
   */
  public JsValueStringifier() {
    this.config = new Config();
  }

  /**
   * Use the specified {@code config} data.
   * @param config to use when rendering values.
   */
  public JsValueStringifier(Config config) {
    this.config = config;
  }

  public String render(JsValue value) {
    if (value == null) {
      return UNKNOWN_VALUE;
    }
    StringBuilder output = new StringBuilder();
    renderInternal(value, config.maxLength, true, output);
    return output.toString();
  }

  /**
   * @param value to render
   * @param maxLength the maximum length of the {@code output}
   * @param descend whether to descend into the object contents
   * @param output to render into
   */
  private void renderInternal(JsValue value, int maxLength, boolean descend, StringBuilder output) {
    if (!descend) {
      renderPrimitive(value, maxLength, output);
      return;
    }
    Type type = value.getType();
    // TODO(apavlov): implement good stringification of other types?
    switch (type) {
      case TYPE_ARRAY:
        renderArray(value.asObject().asArray(), maxLength, output);
        break;
      case TYPE_OBJECT:
        renderObject(value.asObject(), maxLength, output);
        break;
      default:
        renderPrimitive(value, maxLength, output);
        break;
    }
  }

  private void renderPrimitive(JsValue value, int maxLength, StringBuilder output) {
    output.append(value.getValueString());
    truncate(output, maxLength, ELLIPSIS);
  }

  private void truncate(StringBuilder valueBuilder, int maxLength, String suffix) {
    int length = valueBuilder.length();
    if (length > maxLength) {
      valueBuilder.setLength(maxLength);
      valueBuilder.replace(
          maxLength - suffix.length(), maxLength,  suffix);
    }
  }

  private StringBuilder renderArray(JsArray value, int maxLength, StringBuilder output) {
    output.append('[');
    SortedMap<Integer, ? extends JsVariable> indexToElement = value.toSparseArray();
    boolean isFirst = true;
    int maxLengthWithoutLastBracket = maxLength - 1;
    StringBuilder elementBuilder = new StringBuilder();
    int entriesWritten = 0;
    for (Map.Entry<Integer, ? extends JsVariable> entry : indexToElement.entrySet()) {
      Integer index = entry.getKey();
      JsVariable var = entry.getValue();
      if (!isFirst) {
        output.append(',');
      } else {
        isFirst = false;
      }
      elementBuilder.setLength(0);
      elementBuilder.append(index).append('=');
      renderInternal(var.getValue(), maxLengthWithoutLastBracket /* essentially, no limit */,
          false, elementBuilder);
      if (output.length() + elementBuilder.length() >= maxLengthWithoutLastBracket) {
        // reached max length
        appendNMore(output, indexToElement.size() - entriesWritten);
        break;
      } else {
        output.append(elementBuilder.toString());
        entriesWritten++;
      }
    }
    return output.append(']');
  }

  private StringBuilder renderObject(JsObject value, int maxLength, StringBuilder output) {
    output.append('[');
    Collection<? extends JsVariable> properties = value.getProperties();
    boolean isFirst = true;
    int maxLengthWithoutLastBracket = maxLength - 1;
    StringBuilder elementBuilder = new StringBuilder();
    int entriesWritten = 0;
    for (JsVariable property : properties) {
      String name = property.getName();
      if (!isFirst) {
        output.append(',');
      } else {
        isFirst = false;
      }
      elementBuilder.setLength(0);
      elementBuilder.append(name).append('=');
      renderInternal(property.getValue(), maxLengthWithoutLastBracket /* essentially, no limit */,
          false, elementBuilder);
      if (output.length() + elementBuilder.length() >= maxLengthWithoutLastBracket) {
        // reached max length
        appendNMore(output, properties.size() - entriesWritten);
        break;
      } else {
        output.append(elementBuilder.toString());
        entriesWritten++;
      }
    }
    return output.append(']');
  }

  private StringBuilder appendNMore(StringBuilder output, int n) {
    return output.append(" +").append(n).append("..."); //$NON-NLS-1$ //$NON-NLS-2$
  }
}
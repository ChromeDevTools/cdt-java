// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An object that describes the numeric part of version.
 */
public class Version implements Comparable<Version> {
  private final List<Integer> components;
  private final String textComponent;

  /**
   * Constructs an immutable Version instance given the numeric components of version.
   */
  public Version(Integer ... components) {
    this(Arrays.asList(components), null);
  }
  /**
   * Constructs an immutable Version instance given the numeric components of version and optional
   * text component.
   */
  public Version(List<Integer> components, String textComponent) {
    this.components = Collections.unmodifiableList(new ArrayList<Integer>(components));
    this.textComponent = textComponent;
  }


  /**
   * @return numeric components of version in form of list of integers
   */
  public List<Integer> getComponents() {
    return components;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Version)) {
      return false;
    }
    Version that = (Version) obj;
    return this.components.equals(that.components);
  }

  @Override
  public int hashCode() {
    return components.hashCode();
  }

  public int compareTo(Version other) {
    for (int i = 0; i < this.components.size(); i++) {
      if (other.components.size() <= i) {
        // shorter version is less
        return +1;
      }
      int res = this.components.get(i).compareTo(other.components.get(i));
      if (res != 0) {
        return res;
      }
    }
    if (this.components.size() < other.components.size()) {
      return -1;
    } else {
      return 0;
    }
  }

  @Override
  public String toString() {
    if (textComponent == null) {
      return components.toString();
    } else {
      return components.toString() + textComponent;
    }
  }

  /**
   * Parses string as version as far as it is dot-delimited integer numbers.
   * @param text string representation of version; not null
   * @return new instance of version or null if text is not version.
   */
  public static Version parseString(String text) {
    int i = 0;
    List<Integer> components = new ArrayList<Integer>(4);
    while (i < text.length()) {
      int num = Character.digit(text.charAt(i), 10);
      if (num < 0) {
        break;
      }
      i++;
      while (i < text.length() && Character.digit(text.charAt(i), 10) >= 0) {
        num = num * 10 + Character.digit(text.charAt(i), 10);
        i++;
      }
      components.add(num);
      if (i < text.length() && text.charAt(i) == '.') {
        i++;
        continue;
      } else {
        break;
      }
    }
    String suffix;
    if (i < text.length()) {
      suffix = text.substring(i);
    } else {
      suffix = null;
    }
    if (components.isEmpty()) {
      return null;
    }
    return new Version(components, suffix);
  }
}

// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.json.simple.JSONObject;

/**
 * A named property reference.
 */
public class PropertyReference {
  private final int ref;

  private final String name;

  private final JSONObject valueObject;

  /**
   * @param refId the "ref" value of this property
   * @param propertyName the name of the property
   * @param valueObject a JSON descriptor of the property
   */
  public PropertyReference(int refId, String propertyName, JSONObject valueObject) {
    this.ref = refId;
    this.name = propertyName;
    this.valueObject = valueObject;
  }

  public int getRef() {
    return ref;
  }

  public String getName() {
    return name;
  }

  public JSONObject getValueObject() {
    return valueObject;
  }
}
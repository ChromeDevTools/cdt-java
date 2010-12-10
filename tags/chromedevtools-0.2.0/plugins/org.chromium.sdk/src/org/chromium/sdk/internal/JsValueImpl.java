// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.tools.v8.LoadableString;

/**
 * A base class that represents a JavaScript VM variable value (compound values
 * are represented by subclasses.)
 */
class JsValueImpl implements JsValue {

  /** The value data as reported by the JavaScript VM. */
  private final ValueMirror valueData;

  JsValueImpl(ValueMirror valueData) {
    this.valueData = valueData;
  }

  public Type getType() {
    return valueData.getType();
  }

  public String getValueString() {
    return valueData.toString();
  }

  public JsObjectImpl asObject() {
    return null;
  }

  public ValueMirror getMirror() {
    return this.valueData;
  }

  public boolean isTruncated() {
    LoadableString stringValue = this.valueData.getStringValue();
    return stringValue != null && stringValue.needsReload();
  }

  public void reloadHeavyValue(final ReloadBiggerCallback callback,
      SyncCallback syncCallback) {

    LoadableString stringValue = this.valueData.getStringValue();
    if (stringValue != null) {
      JavascriptVm.GenericCallback<Void> innerCallback = new JavascriptVm.GenericCallback<Void>() {
        public void success(Void value) {
          if (callback != null) {
            callback.done();
          }
        }
        public void failure(Exception e) {
        }
      };
      stringValue.reloadBigger(innerCallback, syncCallback);

    } else {
      if (syncCallback != null) {
        syncCallback.callbackDone(null);
      }
    }
  }

  @Override
  public String toString() {
    return String.format("[JsValue: type=%s,value=%s]", getType(), getValueString());
  }
}

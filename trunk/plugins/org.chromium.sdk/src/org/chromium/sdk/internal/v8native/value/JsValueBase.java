// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * A base class that represents a JavaScript VM variable value (compound values
 * are represented by subclasses.)
 */
abstract class JsValueBase implements JsValue {
  private final Type type;
  private final LoadableString loadableString;

  JsValueBase(ValueMirror valueData) {
    this.type = valueData.getType();
    this.loadableString = valueData.getStringValue();
  }

  @Override
  public Type getType() {
    return type;
  }

  protected LoadableString getLoadableString() {
    return loadableString;
  }


  @Override
  public boolean isTruncated() {
    LoadableString stringValue = loadableString;
    return stringValue != null && stringValue.needsReload();
  }

  @Override
  public RelayOk reloadHeavyValue(final ReloadBiggerCallback callback,
      SyncCallback syncCallback) {

    if (loadableString != null) {
      JavascriptVm.GenericCallback<Void> innerCallback = new JavascriptVm.GenericCallback<Void>() {
        @Override
        public void success(Void value) {
          if (callback != null) {
            callback.done();
          }
        }
        @Override public void failure(Exception e) {
        }
      };
      return loadableString.reloadBigger(innerCallback, syncCallback);

    } else {
      return RelaySyncCallback.finish(syncCallback);
    }
  }

  @Override public abstract String toString();

  static class Impl extends JsValueBase {
    Impl(ValueMirror valueData) {
      super(valueData);
    }

    @Override
    public JsObjectBase asObject() {
      return null;
    }

    @Override
    public String getValueString() {
      LoadableString s = getLoadableString();
      return s == null ? "" : s.getCurrentString();
    }

    @Override
    public String toString() {
      return String.format("[JsValue: type=%s,value=%s]", getType(), getValueString());
    }
  }
}

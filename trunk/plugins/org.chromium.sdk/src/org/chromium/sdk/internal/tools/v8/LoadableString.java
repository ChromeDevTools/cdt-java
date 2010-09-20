// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocol.data.ValueHandle;

/**
 * Represents a string which full value is not available at the moment
 * and may be loaded later on demand.
 */
public interface LoadableString {

  /**
   * A factory that creates a {@link LoadableString} from {@link ValueHandle}.
   * To provide a fully-functional {@link LoadableString} the factory should keep
   * a reference to a ValueLoader or some other of loading values.
   */
  interface Factory {
    LoadableString create(ValueHandle handle);

    Factory IMMUTABLE = new Factory() {
      public LoadableString create(ValueHandle handle) {
        return new Immutable(handle.text());
      }
    };
  }

  /**
   * @return full string or only its part depending on what is currently available
   */
  String getCurrentString();

  /**
   * @return whether string has been truncated and whether it makes sense to reload its value
   */
  boolean needsReload();

  /**
   * Asynchronously reloads string value from remote. A newly loaded string will be bigger,
   * but again not necessarily full.
   */
  void reloadBigger(JavascriptVm.GenericCallback<Void> callback, SyncCallback syncCallback);

  /**
   * A trivial implementation of {@link LoadableString} that never actually loads anything.
   */
  class Immutable implements LoadableString {
    private final String value;

    public Immutable(String value) {
      this.value = value;
    }
    public String getCurrentString() {
      return value;
    }
    public boolean needsReload() {
      return false;
    }
    public void reloadBigger(JavascriptVm.GenericCallback<Void> callback,
        SyncCallback syncCallback) {
      try {
        if (callback != null) {
          callback.success(null);
        }
      } finally {
        if (syncCallback != null) {
          syncCallback.callbackDone(null);
        }
      }
    }
  }
}

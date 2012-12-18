// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.EvaluateMessage;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

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
      @Override public LoadableString create(ValueHandle handle) {
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
  RelayOk reloadBigger(GenericCallback<Void> callback, SyncCallback syncCallback);

  EvaluateMessage.Value getProtocolDescription(InternalContext hostInternalContext);

  /**
   * A trivial implementation of {@link LoadableString} that never actually loads anything.
   */
  class Immutable implements LoadableString {
    private final String value;

    public Immutable(String value) {
      this.value = value;
    }
    @Override public String getCurrentString() {
      return value;
    }
    @Override public boolean needsReload() {
      return false;
    }
    @Override
    public RelayOk reloadBigger(GenericCallback<Void> callback,
        SyncCallback syncCallback) {
      if (callback != null) {
        callback.success(null);
      }
      return RelaySyncCallback.finish(syncCallback);
    }

    @Override
    public EvaluateMessage.Value getProtocolDescription(InternalContext hostInternalContext) {
      return EvaluateMessage.Value.createForValue(value);
    }
  }
}

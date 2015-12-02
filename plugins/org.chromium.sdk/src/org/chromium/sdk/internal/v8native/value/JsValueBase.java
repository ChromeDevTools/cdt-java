// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.protocol.output.EvaluateMessage;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * A base class that represents a JavaScript VM variable value (compound values
 * are represented by subclasses.)
 */
public abstract class JsValueBase implements JsValue {
  private final Type type;
  private final LoadableString loadableString;

  JsValueBase(ValueMirror valueData) {
    this.type = valueData.getType();
    this.loadableString = valueData.getStringValue();
  }

  private JsValueBase(Type type, LoadableString loadableString) {
    this.type = type;
    this.loadableString = loadableString;
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
      GenericCallback<Void> innerCallback = new GenericCallback<Void>() {
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

  public abstract EvaluateMessage.Value getJsonParam(InternalContext hostInternalContext);

  public static class Impl extends JsValueBase {
    Impl(ValueMirror valueData) {
      super(valueData);
    }

    public Impl(Type type, String string) {
      super(type, new LoadableString.Immutable(string));
    }

    @Override
    public JsObject asObject() {
      return null;
    }

    @Override
    public String getValueString() {
      LoadableString s = getLoadableString();
      return s == null ? "" : s.getCurrentString();
    }

    @Override
    public EvaluateMessage.Value getJsonParam(InternalContext hostInternalContext) {
      EvaluateMessage.Value.Type protocolType;
      switch (getType()) {
      case TYPE_NULL:
        return EvaluateMessage.Value.createForType(EvaluateMessage.Value.Type.NULL);
      case TYPE_UNDEFINED:
        return EvaluateMessage.Value.createForType(EvaluateMessage.Value.Type.UNDEFINED);
      case TYPE_BOOLEAN:
        protocolType = EvaluateMessage.Value.Type.BOOLEAN;
        break;
      case TYPE_NUMBER:
        protocolType = EvaluateMessage.Value.Type.NUMBER;
        break;
      case TYPE_STRING:
        return getLoadableString().getProtocolDescription(hostInternalContext);
      default:
        throw new RuntimeException("Unsupported type " + getType());
      }
      return EvaluateMessage.Value.createForStringDescription(protocolType, getValueString());
    }

    @Override
    public String toString() {
      return String.format("[JsValue: type=%s,value=%s]", getType(), getValueString());
    }
  }

  public static JsValueBase cast(JsValue value) {
    if (false == value instanceof JsValueBase) {
      throw new IllegalArgumentException("Incorrect argument type " + value.getClass());
    }
    return (JsValueBase) value;
  }
}

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsValue.Type;
import org.chromium.sdk.internal.v8native.V8Helper;
import org.chromium.sdk.internal.v8native.protocol.input.data.FunctionValueHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.ObjectValueHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.RefWithDisplayData;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.value.LoadableString.Factory;

/**
 * A representation of a datum (value) in the remote JavaScript VM. The class must be immutable.
 * When additional (or more recent) data arrives, new instance should be put into
 * {@link ValueLoader} map.
 */
public abstract class ValueMirror {
  /**
   * Merges to {@link ValueMirror}s into one. Since {@link ValueMirror} is immutable
   * this is the right way to gather data.
   * @return possibly new {@link ValueMirror}, or any of the old ones, making a preference for
   *      'base' parameter.
   */
  static ValueMirror merge(ValueMirror base, ValueMirror alternative) {
    if (base.hasProperties()) {
      if (alternative.hasProperties()) {
        // Fall through.
      } else {
        return base;
      }
    } else {
      if (alternative.hasProperties()) {
        return alternative;
      } else {
        // Fall through.
      }
    }
    int lenDiff = base.getStringLength() - alternative.getStringLength();
    if (lenDiff < 0) {
      return alternative;
    } else {
      return base;
    }
  }

  /**
   * Constructs a {@link ValueMirror} given a V8 debugger object specification if it's possible.
   */
  public static ValueMirror create(final RefWithDisplayData refWithDisplayData,
      Factory loadableStringFactory) {
    Long ref = refWithDisplayData.ref();
    final Type type = V8Helper.calculateType(refWithDisplayData.type(),
        refWithDisplayData.className(), false);

    return new ValueMirror(ref) {
      @Override
      public Type getType() {
        return type;
      }

      @Override
      public String getClassName() {
        return refWithDisplayData.className();
      }

      @Override
      public LoadableString getStringValue() {
        // try another format
        Object valueObj = refWithDisplayData.value();
        String valueStr;
        if (valueObj == null) {
          valueStr = refWithDisplayData.type(); // e.g. "undefined"
        } else {
          valueStr = valueObj.toString();
        }
        return new LoadableString.Immutable(valueStr);
      }

      @Override
      public boolean hasProperties() {
        return false;
      }

      @Override
      public SubpropertiesMirror getProperties() {
        return null;
      }
    };
  }


  /**
   * Constructs a ValueMirror given a V8 debugger object specification.
   * @param valueHandle containing the object specification from the V8 debugger
   */
  public static ValueMirror create(final ValueHandle valueHandle, final Factory factory) {
    Long ref = valueHandle.handle();

    final Type type = V8Helper.calculateType(valueHandle.type(), valueHandle.className(), true);

    return new ValueMirror(ref) {
      @Override
      public Type getType() {
        return type;
      }

      @Override
      public LoadableString getStringValue() {
        return V8Helper.createLoadableString(valueHandle, factory);
      }

      @Override
      public SubpropertiesMirror getProperties() {
        ObjectValueHandle objectValueHandle = valueHandle.asObject();
        if (objectValueHandle == null) {
          return SubpropertiesMirror.EMPTY;
        }
        int refId = (int) valueHandle.handle();
        SubpropertiesMirror subpropertiesMirror;
        if (type == Type.TYPE_FUNCTION) {
          FunctionValueHandle functionValueHandle = objectValueHandle.asFunction();
          subpropertiesMirror = new SubpropertiesMirror.FunctionValueBased(functionValueHandle);
        } else {
          subpropertiesMirror = new SubpropertiesMirror.ObjectValueBased(objectValueHandle);
        }
        return subpropertiesMirror;
      }

      @Override
      public boolean hasProperties() {
        return true;
      }

      @Override
      public String getClassName() {
        return valueHandle.className();
      }
    };
  }

  public static ValueMirror create(Long ref, final Type type,
      final String className, final LoadableString loadableString,
      final SubpropertiesMirror subpropertiesMirror) {
    return new ValueMirror(ref) {
      @Override
      public JsValue.Type getType() {
        return type;
      }

      @Override
      public String getClassName() {
        return className;
      }

      @Override
      public LoadableString getStringValue() {
        return loadableString;
      }

      @Override
      public SubpropertiesMirror getProperties() {
        return subpropertiesMirror;
      }

      @Override
      public boolean hasProperties() {
        return getProperties() != null;
      }
    };
  }

  private final Long ref;

  protected ValueMirror(Long ref) {
    assert ref != null;
    this.ref = ref;
  }

  public abstract JsValue.Type getType();

  public abstract SubpropertiesMirror getProperties();

  public Long getRef() {
    return ref;
  }

  public abstract LoadableString getStringValue();

  public abstract String getClassName();

  public abstract boolean hasProperties();

  int getStringLength() {
    LoadableString loadableString = getStringValue();
    if (loadableString == null) {
      return 0;
    }
    return loadableString.getCurrentString().length();
  }
}

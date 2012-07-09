package org.chromium.sdk.internal.v8native.value;

import org.chromium.sdk.internal.v8native.protocol.input.data.RefWithDisplayData;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeRef;

public abstract class DataWithRef {
  public abstract long ref();

  /** @return data or null */
  public abstract RefWithDisplayData getWithDisplayData();

  public static DataWithRef fromSomeRef(final SomeRef someRef) {
    return new DataWithRef() {
      @Override
      public RefWithDisplayData getWithDisplayData() {
        return someRef.asWithDisplayData();
      }
      @Override
      public long ref() {
        return someRef.ref();
      }
    };
  }
  public static DataWithRef fromLong(final long ref) {
    return new DataWithRef() {
      @Override
      public RefWithDisplayData getWithDisplayData() {
        return null;
      }
      @Override
      public long ref() {
        return ref;
      }
    };
  }
}

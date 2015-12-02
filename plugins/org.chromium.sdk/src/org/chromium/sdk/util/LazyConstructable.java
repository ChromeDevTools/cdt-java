// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.util;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds a value yet-to-be-constructed. It gets constructed when method {@link #get()} is
 * called the first time. User must provide {@link LazyConstructable.Factory} that
 * actually constructs the value. Once the value is constructed, the factory is released
 * and may be collected by Java GC.
 * <p>Threads: the class is thread-safe; how it's implementation is lock-free, so a
 * factory may be called several times from several concurrent threads. However
 * method {@link #get()} is guaranteed to always return the same value from to whatever
 * thread.
 */
public class LazyConstructable<T> {
  public interface Factory<T> {
    T construct();
  }

  public static <T> LazyConstructable<T> create(Factory<T> factory) {
    return new LazyConstructable<T>(factory);
  }

  private final AtomicReference<Result<T>> resultRef;

  public LazyConstructable(Factory<T> factory) {
    this.resultRef = new AtomicReference<Result<T>>(new FutureResult(factory));
  }

  /**
   * Constructs a value when called first time and returns it to all subsequent calls.
   */
  public T get() {
    return resultRef.get().get();
  }

  private static abstract class Result<T> {
    abstract T get();
  }

  private class FutureResult extends Result<T> {
    private final Factory<T> factory;

    private FutureResult(Factory<T> factory) {
      this.factory = factory;
    }

    @Override
    T get() {
      T newResult = factory.construct();
      resultRef.compareAndSet(this, new ReadyResult<T>(newResult));
      return resultRef.get().get();
    }
  }

  private static class ReadyResult<T> extends Result<T> {
    private final T result;

    public ReadyResult(T result) {
      this.result = result;
    }

    @Override
    T get() {
      return result;
    }
  }
}

// Copyright 2009 Google Inc. All Rights Reserved.

package org.chromium.sdk.internal.tools.v8.request;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A singleton that keeps track of the "seq" values for them to be unique across
 * the plugin lifecycle.
 */
public class SeqGenerator {

  private static SeqGenerator INSTANCE = new SeqGenerator();

  private final AtomicInteger count = new AtomicInteger(1);

  public static SeqGenerator getInstance() {
    return INSTANCE;
  }

  public int next() {
    return count.getAndIncrement();
  }

  private SeqGenerator() {
    // not instantiable outside
  }
}

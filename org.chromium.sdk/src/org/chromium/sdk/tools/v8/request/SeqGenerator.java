// Copyright 2009 Google Inc. All Rights Reserved.

package org.chromium.debug.core.tools.v8;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keeps track of the "seq" values for them to be unique across the plugin
 * lifecycle.
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

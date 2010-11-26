// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

/**
 * Signals that a deadlock was about to happen.
 * <p>
 * Method may wait for some callback to receive a result from some worker
 * thread. If this method happened to be called from another callback
 * being run by the same thread this will block the thread forever because
 * method is never going to get result. Such situation may raise this
 * exception.
 * <p>
 * Currently this exception is never actually thrown so it gets more of
 * symbolic sense. Nevertheless it's still important to keep its declaration
 * because it helps to track which ones are blocking and which are not.
 * To do this, one should temporarily modify this class and make exception checked
 * (make it extend {@code java.lang.Exception}). This will enforce the proper
 * declaration of all blocking methods.
 * <p>Here are the simple rules: you never normally catch this exception, but
 * add throws declaration wherever needed; if your callback method needs to
 * throw it you are doing something wrong (i.e. calling blocking method from
 * a callback) and risk running into a deadlock; wherever you are
 * sure you are on a safe ground (not called from callback) and there is no
 * need in further tracking this exception, make a symbolic try/catch and
 * explain why you think it's safe, in a catch block:
 * <pre>
 *   try {
 *     makeSureAllScriptsAreLoaded();
 *   } catch (MethodIsBlockingException e) {
 *     // I'm being called from my own thread, so it's ok if method blocks,
 *     // I can wait
 *     throw new RuntimeException(e); // never executed
 *   }
 * </pre>
 * <p>By default, {@code MethodIsBlockingException} is unchecked exception,
 * so you may completely ignore it.
 */
public class MethodIsBlockingException extends RuntimeException {
  // We never actually instantiate this exception, because it's symbolic.
  private MethodIsBlockingException() {}
}

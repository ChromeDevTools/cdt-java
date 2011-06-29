package org.chromium.sdk.internal.v8native;

import org.chromium.sdk.internal.v8native.ScriptManager;
import org.chromium.sdk.internal.v8native.V8ContextFilter;


public class ScriptImplTestGate {

  public static ScriptManager create(V8ContextFilter contextFilter) {
    return new ScriptManager(contextFilter, null);
  }

}

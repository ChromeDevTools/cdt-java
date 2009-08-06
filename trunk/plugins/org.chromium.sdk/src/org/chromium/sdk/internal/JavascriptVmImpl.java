package org.chromium.sdk.internal;



import org.chromium.sdk.Breakpoint;
import org.chromium.sdk.JavascriptVm;

public abstract class JavascriptVmImpl implements JavascriptVm {

  protected JavascriptVmImpl() {
  }

  public void getScripts(final ScriptsCallback callback) {
    getDebugContext().loadAllScripts(callback);
  }

  public void setBreakpoint(Breakpoint.Type type, String target, int line,
      int position, boolean enabled, String condition, int ignoreCount,
      BreakpointCallback callback) {
    getDebugContext().getV8Handler().getV8CommandProcessor().getBreakpointProcessor()
        .setBreakpoint(type, target, line, position, enabled, condition, ignoreCount, callback);  
  }

  public abstract DebugContextImpl getDebugContext();
}

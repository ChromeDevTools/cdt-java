// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Defines pause on exceptions state. Can be set to stop on all exceptions, uncaught exceptions or no exceptions.
 */
public class SetPauseOnExceptionsParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setPauseOnExceptions";

  /**
   @param state Pause on exceptions mode.
   */
  public SetPauseOnExceptionsParams(State state) {
    this.put("state", state);
  }

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  /**
   Pause on exceptions mode.
   */
  public enum State {
    NONE("none"),
    UNCAUGHT("uncaught"),
    ALL("all"),
    ;
    private final String protocolValue;

    State(String protocolValue) {
      this.protocolValue = protocolValue;
    }

    @Override public String toString() {
      return protocolValue;
    }
  }
}

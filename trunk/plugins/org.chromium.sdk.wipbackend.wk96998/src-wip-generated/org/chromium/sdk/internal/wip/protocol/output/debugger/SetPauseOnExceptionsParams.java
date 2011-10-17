// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.output.debugger;

/**
Defines pause on exceptions state. Can be set to stop on all exceptions, uncaught exceptions or no exceptions. Initial pause on exceptions state is <code>none</code>.
 */
public class SetPauseOnExceptionsParams extends org.chromium.sdk.internal.wip.protocol.output.WipParams {
  /**
   @param state Pause on exceptions mode.
   */
  public SetPauseOnExceptionsParams(State state) {
    this.put("state", state);
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.DEBUGGER + ".setPauseOnExceptions";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  /**
   Pause on exceptions mode.
   */
  public enum State implements org.json.simple.JSONAware{
    NONE("none"),
    UNCAUGHT("uncaught"),
    ALL("all"),
    ;
    private final String protocolValue;

    State(String protocolValue) {
      this.protocolValue = protocolValue;
    }

    @Override public String toJSONString() {
      return '"' + protocolValue + '"';
    }
  }
}

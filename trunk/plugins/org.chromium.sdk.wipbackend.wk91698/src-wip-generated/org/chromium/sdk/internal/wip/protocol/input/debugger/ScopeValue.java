// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Debugger call frame. Array of call frames form call stack.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ScopeValue {
  /**
   Scope type.
   */
  Type type();

  /**
   Object representing the scope.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue object();

  /**
   Scope type.
   */
  public enum Type {
    GLOBAL,
    LOCAL,
    WITH,
    CLOSURE,
    CATCH,
  }
}

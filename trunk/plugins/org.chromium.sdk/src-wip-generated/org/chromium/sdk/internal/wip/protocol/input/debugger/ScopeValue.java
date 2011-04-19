// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@84080

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
   <code>this</code> object for local scope.
   */
  @org.chromium.sdk.internal.protocolparser.JsonField(jsonLiteralName="this")
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue getThis();

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

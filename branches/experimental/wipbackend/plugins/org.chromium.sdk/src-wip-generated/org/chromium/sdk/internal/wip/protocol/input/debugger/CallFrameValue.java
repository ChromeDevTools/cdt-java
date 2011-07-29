// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@86959

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 Debugger call frame. Array of call frames form call stack.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CallFrameValue {
  /**
   Call frame identifier.
   */
  String id();

  /**
   Name of the function called on this frame.
   */
  String functionName();

  /**
   Location in the source code.
   */
  org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue location();

  /**
   Scope chain for given call frame.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.ScopeValue> scopeChain();

  /**
   <code>this</code> object for this call frame.
   */
  @org.chromium.sdk.internal.protocolparser.JsonField(jsonLiteralName="this")
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue getThis();

}

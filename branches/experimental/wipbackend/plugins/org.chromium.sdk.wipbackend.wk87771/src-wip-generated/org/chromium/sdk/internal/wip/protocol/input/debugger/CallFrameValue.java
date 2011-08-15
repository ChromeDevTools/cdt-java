// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@92377

package org.chromium.sdk.internal.wip.protocol.input.debugger;

/**
 JavaScript call frame. Array of call frames form the call stack.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CallFrameValue {
  /**
   Call frame identifier. This identifier is only valid while the virtual machine is paused.
   */
  String id();

  /**
   Name of the JavaScript function called on this call frame.
   */
  String functionName();

  /**
   Location in the source code.
   */
  org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue location();

  /**
   Scope chain for this call frame.
   */
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.debugger.ScopeValue> scopeChain();

  /**
   <code>this</code> object for this call frame.
   */
  @org.chromium.sdk.internal.protocolparser.JsonField(jsonLiteralName="this")
  org.chromium.sdk.internal.wip.protocol.input.runtime.RemoteObjectValue getThis();

}

// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Description of an isolated world.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ExecutionContextDescriptionValue {
  /**
   Unique id of the execution context. It can be used to specify in which execution context script evaluation should be performed.
   */
  long/*See org.chromium.sdk.internal.wip.protocol.common.runtime.ExecutionContextIdTypedef*/ id();

  /**
   True if this is a context where inpspected web page scripts run. False if it is a content script isolated context.
   */
  boolean isPageContext();

  /**
   Human readable name describing given context.
   */
  String name();

  /**
   Id of the owning frame.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.FrameIdTypedef*/ frameId();

}

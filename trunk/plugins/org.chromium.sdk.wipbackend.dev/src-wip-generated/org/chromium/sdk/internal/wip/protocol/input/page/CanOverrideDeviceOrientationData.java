// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Check the backend if Web Inspector can override the device orientation.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface CanOverrideDeviceOrientationData {
  /**
   If true, <code>setDeviceOrientationOverride</code> can safely be invoked on the agent.
   */
  boolean result();

}

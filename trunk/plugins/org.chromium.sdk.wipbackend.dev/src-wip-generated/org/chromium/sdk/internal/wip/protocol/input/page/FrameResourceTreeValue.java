// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@108993

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Information about the Frame hierarchy along with their cached resources.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface FrameResourceTreeValue {
  /**
   Frame information for this tree item.
   */
  org.chromium.sdk.internal.wip.protocol.input.page.FrameValue frame();

  /**
   Child frames.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  java.util.List<org.chromium.sdk.internal.wip.protocol.input.page.FrameResourceTreeValue> childFrames();

  /**
   Information about frame resources.
   */
  java.util.List<Resources> resources();

  @org.chromium.sdk.internal.protocolparser.JsonType
  public interface Resources {
    /**
     Resource URL.
     */
    String url();

    /**
     Type of this resource.
     */
    org.chromium.sdk.internal.wip.protocol.input.page.ResourceTypeEnum type();

    /**
     Resource mimeType as determined by the browser.
     */
    String mimeType();

    /**
     True if the resource failed to load.
     */
    @org.chromium.sdk.internal.protocolparser.JsonOptionalField
    Boolean failed();

    /**
     True if the resource was canceled during loading.
     */
    @org.chromium.sdk.internal.protocolparser.JsonOptionalField
    Boolean canceled();

  }
}

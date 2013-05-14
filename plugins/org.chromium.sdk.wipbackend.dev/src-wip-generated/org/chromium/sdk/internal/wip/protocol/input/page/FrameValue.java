// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@150309 with change #14672031

package org.chromium.sdk.internal.wip.protocol.input.page;

/**
 Information about the Frame on the page.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface FrameValue {
  /**
   Frame unique identifier.
   */
  String id();

  /**
   Parent frame identifier.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String parentId();

  /**
   Identifier of the loader associated with this frame.
   */
  String/*See org.chromium.sdk.internal.wip.protocol.common.network.LoaderIdTypedef*/ loaderId();

  /**
   Frame's name as specified in the tag.
   */
  @org.chromium.sdk.internal.protocolparser.JsonOptionalField
  String name();

  /**
   Frame document's URL.
   */
  String url();

  /**
   Frame document's security origin.
   */
  String securityOrigin();

  /**
   Frame document's mimeType as determined by the browser.
   */
  String mimeType();

}

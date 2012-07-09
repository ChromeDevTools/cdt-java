// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@116768

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Issued when new isolated context is created.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface IsolatedContextCreatedEventData {
  /**
   A newly created isolated contex.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.ExecutionContextDescriptionValue context();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.runtime.IsolatedContextCreatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.runtime.IsolatedContextCreatedEventData>("Runtime.isolatedContextCreated", org.chromium.sdk.internal.wip.protocol.input.runtime.IsolatedContextCreatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.runtime.IsolatedContextCreatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseRuntimeIsolatedContextCreatedEventData(obj);
    }
  };
}

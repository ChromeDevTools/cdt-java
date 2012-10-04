// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@130398

package org.chromium.sdk.internal.wip.protocol.input.runtime;

/**
 Issued when new execution context is created.
 */
@org.chromium.sdk.internal.protocolparser.JsonType
public interface ExecutionContextCreatedEventData {
  /**
   A newly created execution contex.
   */
  org.chromium.sdk.internal.wip.protocol.input.runtime.ExecutionContextDescriptionValue context();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.runtime.ExecutionContextCreatedEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.runtime.ExecutionContextCreatedEventData>("Runtime.executionContextCreated", org.chromium.sdk.internal.wip.protocol.input.runtime.ExecutionContextCreatedEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.runtime.ExecutionContextCreatedEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseRuntimeExecutionContextCreatedEventData(obj);
    }
  };
}

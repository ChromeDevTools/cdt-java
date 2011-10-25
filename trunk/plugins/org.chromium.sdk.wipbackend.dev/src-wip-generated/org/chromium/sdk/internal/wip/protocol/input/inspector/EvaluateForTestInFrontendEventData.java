// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@98328

package org.chromium.sdk.internal.wip.protocol.input.inspector;

@org.chromium.sdk.internal.protocolparser.JsonType
public interface EvaluateForTestInFrontendEventData {
  long testCallId();

  String script();

  public static final org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.EvaluateForTestInFrontendEventData> TYPE
      = new org.chromium.sdk.internal.wip.protocol.input.WipEventType<org.chromium.sdk.internal.wip.protocol.input.inspector.EvaluateForTestInFrontendEventData>("Inspector.evaluateForTestInFrontend", org.chromium.sdk.internal.wip.protocol.input.inspector.EvaluateForTestInFrontendEventData.class) {
    @Override public org.chromium.sdk.internal.wip.protocol.input.inspector.EvaluateForTestInFrontendEventData parse(org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
      return parser.parseInspectorEvaluateForTestInFrontendEventData(obj);
    }
  };
}

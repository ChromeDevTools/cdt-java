// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@113013

package org.chromium.sdk.internal.wip.protocol.output.page;

/**
Checks whether <code>setDeviceMetricsOverride</code> can be invoked.
 */
public class CanOverrideDeviceMetricsParams extends org.chromium.sdk.internal.wip.protocol.output.WipParamsWithResponse<org.chromium.sdk.internal.wip.protocol.input.page.CanOverrideDeviceMetricsData> {
  public CanOverrideDeviceMetricsParams() {
  }

  public static final String METHOD_NAME = org.chromium.sdk.internal.wip.protocol.BasicConstants.Domain.PAGE + ".canOverrideDeviceMetrics";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.chromium.sdk.internal.wip.protocol.input.page.CanOverrideDeviceMetricsData parseResponse(org.chromium.sdk.internal.wip.protocol.input.WipCommandResponse.Data data, org.chromium.sdk.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException {
    return parser.parsePageCanOverrideDeviceMetricsData(data.getUnderlyingObject());
  }

}

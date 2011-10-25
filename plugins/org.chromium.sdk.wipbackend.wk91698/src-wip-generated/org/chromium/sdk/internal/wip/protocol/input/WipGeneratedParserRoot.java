// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/91698/trunk/Source/WebCore/inspector/Inspector.json@91673

package org.chromium.sdk.internal.wip.protocol.input;

@org.chromium.sdk.internal.protocolparser.JsonParserRoot
public interface WipGeneratedParserRoot {
  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.BreakpointResolvedEventData parseDebuggerBreakpointResolvedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.DebuggerWasDisabledEventData parseDebuggerDebuggerWasDisabledEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.DebuggerWasEnabledEventData parseDebuggerDebuggerWasEnabledEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.EvaluateOnCallFrameData parseDebuggerEvaluateOnCallFrameData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.GetScriptSourceData parseDebuggerGetScriptSourceData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.PausedEventData parseDebuggerPausedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.ResumedEventData parseDebuggerResumedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptFailedToParseEventData parseDebuggerScriptFailedToParseEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.ScriptParsedEventData parseDebuggerScriptParsedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData parseDebuggerSetBreakpointByUrlData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointData parseDebuggerSetBreakpointData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.SetScriptSourceData parseDebuggerSetScriptSourceData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.BringToFrontEventData parseInspectorBringToFrontEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.DidCreateWorkerEventData parseInspectorDidCreateWorkerEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.DidDestroyWorkerEventData parseInspectorDidDestroyWorkerEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.DisconnectFromBackendEventData parseInspectorDisconnectFromBackendEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.EvaluateForTestInFrontendEventData parseInspectorEvaluateForTestInFrontendEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.FrontendReusedEventData parseInspectorFrontendReusedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.InspectEventData parseInspectorInspectEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.ResetEventData parseInspectorResetEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.ShowPanelEventData parseInspectorShowPanelEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.inspector.StartUserInitiatedDebuggingEventData parseInspectorStartUserInitiatedDebuggingEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.DomContentEventFiredEventData parsePageDomContentEventFiredEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.FrameDetachedEventData parsePageFrameDetachedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.FrameNavigatedEventData parsePageFrameNavigatedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.GetCookiesData parsePageGetCookiesData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.GetResourceContentData parsePageGetResourceContentData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.GetResourceTreeData parsePageGetResourceTreeData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.LoadEventFiredEventData parsePageLoadEventFiredEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourcesData parsePageSearchInResourcesData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.runtime.CallFunctionOnData parseRuntimeCallFunctionOnData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData parseRuntimeEvaluateData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.runtime.GetPropertiesData parseRuntimeGetPropertiesData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

}

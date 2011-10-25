// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/!svn/bc/96998/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.chromium.sdk.internal.wip.protocol.input;

@org.chromium.sdk.internal.protocolparser.JsonParserRoot
public interface WipGeneratedParserRoot {
  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.console.EnableData parseConsoleEnableData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.console.MessageAddedEventData parseConsoleMessageAddedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.console.MessageRepeatCountUpdatedEventData parseConsoleMessageRepeatCountUpdatedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.console.MessagesClearedEventData parseConsoleMessagesClearedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

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
  org.chromium.sdk.internal.wip.protocol.input.debugger.SearchInContentData parseDebuggerSearchInContentData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointByUrlData parseDebuggerSetBreakpointByUrlData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.SetBreakpointData parseDebuggerSetBreakpointData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.debugger.SetScriptSourceData parseDebuggerSetScriptSourceData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.AttributeModifiedEventData parseDOMAttributeModifiedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.AttributeRemovedEventData parseDOMAttributeRemovedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.CharacterDataModifiedEventData parseDOMCharacterDataModifiedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeCountUpdatedEventData parseDOMChildNodeCountUpdatedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeInsertedEventData parseDOMChildNodeInsertedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.ChildNodeRemovedEventData parseDOMChildNodeRemovedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.DocumentUpdatedEventData parseDOMDocumentUpdatedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.GetAttributesData parseDOMGetAttributesData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.GetDocumentData parseDOMGetDocumentData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.GetEventListenersForNodeData parseDOMGetEventListenersForNodeData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.GetOuterHTMLData parseDOMGetOuterHTMLData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.InlineStyleInvalidatedEventData parseDOMInlineStyleInvalidatedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.MoveToData parseDOMMoveToData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.PushNodeByPathToFrontendData parseDOMPushNodeByPathToFrontendData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.QuerySelectorAllData parseDOMQuerySelectorAllData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.QuerySelectorData parseDOMQuerySelectorData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.RequestNodeData parseDOMRequestNodeData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.ResolveNodeData parseDOMResolveNodeData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.SearchResultsEventData parseDOMSearchResultsEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.SetChildNodesEventData parseDOMSetChildNodesEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.SetNodeNameData parseDOMSetNodeNameData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.dom.SetOuterHTMLData parseDOMSetOuterHTMLData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

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
  org.chromium.sdk.internal.wip.protocol.input.network.DataReceivedEventData parseNetworkDataReceivedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.GetResponseBodyData parseNetworkGetResponseBodyData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.LoadingFailedEventData parseNetworkLoadingFailedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.LoadingFinishedEventData parseNetworkLoadingFinishedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.RequestServedFromCacheEventData parseNetworkRequestServedFromCacheEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.RequestServedFromMemoryCacheEventData parseNetworkRequestServedFromMemoryCacheEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.RequestWillBeSentEventData parseNetworkRequestWillBeSentEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.ResponseReceivedEventData parseNetworkResponseReceivedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.WebSocketClosedEventData parseNetworkWebSocketClosedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.WebSocketCreatedEventData parseNetworkWebSocketCreatedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.WebSocketHandshakeResponseReceivedEventData parseNetworkWebSocketHandshakeResponseReceivedEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.network.WebSocketWillSendHandshakeRequestEventData parseNetworkWebSocketWillSendHandshakeRequestEventData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

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
  org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourceData parsePageSearchInResourceData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.page.SearchInResourcesData parsePageSearchInResourcesData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.runtime.CallFunctionOnData parseRuntimeCallFunctionOnData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.runtime.EvaluateData parseRuntimeEvaluateData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

  @org.chromium.sdk.internal.protocolparser.JsonParseMethod
  org.chromium.sdk.internal.wip.protocol.input.runtime.GetPropertiesData parseRuntimeGetPropertiesData(org.json.simple.JSONObject obj) throws org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;

}

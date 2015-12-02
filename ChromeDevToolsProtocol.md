The protocol is **deprecated** and its support is going to be **finished**. See [below](#Deprecation_And_Removal.md).

Google Chrome/Chromium browser is using [WebKit Remote Debugging Protocol](WebKitProtocol.md) instead, see [the main site](http://developers.google.com/chrome-developer-tools/docs/remote-debugging) for details.

# Introduction #

The existing [V8 Debugger Protocol](http://code.google.com/p/v8/wiki/DebuggerProtocol) is not sufficient to provide Google Chrome, Chromium, or any other browser that supports the protocol, with the capability of remote debugging via TCP/IP sockets. The V8 Debugger protocol covers only JavaScript debugging operations, and only within a single V8 virtual machine (VM). In reality, there can be one or more separate V8 VMs inside a Google Chrome instance, residing in different renderer processes. Also, retrieving URLs loaded in the browser tabs, inspecting or modifying the DOM tree are not covered by JavaScript operations.

Because of these restrictions, the ChromeDevTools protocol has been created to enable the exchange of additional information between a remote debugger and the browser instance being debugged. The ChromeDevTools protocol can be used as a transport for other debugging-related protocols, including the existing V8 Debugger Protocol.
The proposed protocol can be used as a transport for other debugging-related protocols, including the existing V8 Debugger Protocol.

A single subsystem that uses ChromeDevTools protocol as a transport for its own debugging-related protocol is called a "Tool". A V8 VM Debugger, a DOM debugger, or a Developer Tools Service are examples of a Tool.

### How to start Google Chrome/Chromium ###
```
chrome --remote-debugging-port=<port>
```

### How to attach from [Debugger](EclipseDebugger.md) ###
A launch configuration named "Chromium JavaScript" should be created (see [tutorial](DebuggerTutorial#Connect.md)).

### How to attach from SDK ###
Use the following create method:
```
org.chromium.sdk.BrowserFactory.getInstance().create(...)
```

### Protocol Name ###
Protocol is called 'ChromeDevTools Protocol'. The name is pretty overloaded though. You can be sure that you are using this protocol if you are starting Google Chrome/Chromium with the following parameter:
`--remote-debugging-port=<port>`

Compare with [WebKit Remote Debugging Protocol](WebKitProtocol.md).

## Protocol Message Structure ##

For simplicity of parsing, and higher human readability, the generic protocol message structure follows that of an HTTP request/response message (barring the Starting line):

```
Header1
...
HeaderN
<empty line>
Content
```
where _<empty line>_ represents a single pair of the \r\n (CR LF) characters.

A single header format is:
```
<HeaderName>:<HeaderValue>CRLF
```

**Request/Response message headers**
  * _Content-Length_
  * _Tool_
  * _Destination_

_Content-Length_ (required) - the total length of the Сontent field in bytes.

_Tool_ (required) - a string specifying the Tool that will handle the message.

_Destination_ (optional) - a string that identifies the concrete application-specific host object in whose context the message should be handled. For a V8 debugger it can be an identifier of a Google Chrome tab running JavaScript code, and for Developer Tools Service it can be absent altogether because the content pertains to the global Google Chrome context rather than some specific object contained in the browser (e.g. the content can contain a request for the supported version of the Developer Tools protocol.) "0" (zero) is an invalid host object identifier, reserved for specific protocol purposes.

Note: Tools may add other headers that they will extract while processing messages.

**Content**

The optional _Content_ field value is unique to each command as well as the request/response message. For example, it can contain the V8 Debugger Protocol request/response JSON messages or some plain-text command for a simpler tool. The content must use the UTF8 charset. If this field is absent, the _Content-Length_ header value must be 0.

## Establishing the Debugger Connection ##

A remote debugger that supports the ChromeDevTools protocol, connects to a server socket opened by Google Chrome.
A remote debugger sends a handshake message consisting of 23 ASCII characters,
```
ChromeDevToolsHandshakeCRLF
```
Google Chrome replies with the same 23-byte message.

Once the connection is established, the remote debugger might want to query the list of tabs using the **list\_tabs** command.

# Supported Tools #

Currently, all the supported Tools use JSON as the Content format.

## DevToolsService ##

This tool provides a remote debugger with information about the inspectable environment.

**Common DevToolsService JSON fields:**
  * command (required) - the command to perform {string}.
  * result (required) - the operation result (in the response message) {integer}.
  * data (optional) - auxiliary information associated with the command (both in the request and response messages) {string}.

**Available commands:**
  * ping - keeps the connection alive under certain circumstances, or determines if the application is currently capable of handling request messages.
  * version - determines the ChromeDevTools Protocol version supported by the debugged application.
  * list\_tabs - lists all the debuggable tabs together with their respective URLs opened.

The following **result** codes are possible:
  * OK (0) - the operation completed successfully.
  * UNKNOWN\_COMMAND (1) - the specified command is not found in the **Available commands** list.

**The request/response examples:**
| **Command** | **Request content** | **Response "data" field** |
|:------------|:--------------------|:--------------------------|
| ping        | {"command":"ping"}  | ` {"command":"ping", "result":0, "data":"ok"} ` |
| version     | {"command":"version"} | ` {"command":"version", "result":0, "data":version_id} ` |
| list\_tabs  | {"command":"list\_tabs"} | ` {"command":"list_tabs", "result":0, "data":[[tab1_id,"tab1_url"], [tab2_id,"tab2_url"]]} ` |


## V8Debugger ##

This tool enables communication between a remote debugger and the V8 debuggers running inside a Google Chrome instance.

**Common V8Debugger JSON fields:**
  * command (required) - the command to perform, or an event type {string}.
  * result (required in a response) - the operation result {integer}.
  * data (required or not, depends on the message type) - auxiliary information associated with the command (both in the request and response messages) {string}.

**Available commands:**
  * attach - requests attachment to a V8 debugger running in the tab specified in the Destination header.
  * detach - requests detachment from a V8 debugger running in the tab specified in the Destination header.
  * debugger\_command - sends a V8 debugger protocol command to a V8 debugger running in the tab specified in the Destination header.
  * evaluate\_javascript - evaluates JavaScript in the context of a V8 VM associated with the tab specified in the Destination header. No result is returned to the client (use debugger\_command "evaluate" instead). When sent after a V8 debugger command while not on a breakpoint, results in the immediate processing of the command (rather than when a breakpoint is hit.)
  * navigated - an event that notifies the remote debugger of the fact that the tab URL has changed. The "result" value is always 0 (OK).
  * closed - an event that notifies the remote debugger of the fact that the tab has been closed. The "result" value is always 0 (OK).

The following **result** codes are possible:
  * OK (0) - the operation completed successfully (it is a tool-level result code, not a command-level one. For example, the result may be 0 even though the underlying debugger\_command request failed. In this case OK means that a valid response has been received from the V8 debugger.)
  * ILLEGAL\_TAB\_STATE (1) - the tab specified in the "Destination" header is in an inappropriate state (i.e. it is attached for an "attach" command or not attached for a "detach" command.)
  * UNKNOWN\_TAB (2) - the tab specified in the "Destination" header does not exist (it may have been reported in the "list\_tabs" response but closed since then.)
  * DEBUGGER\_ERROR (3) - a generic error occurred while performing the specified operation.
  * UNKNOWN\_COMMAND (4) - the specified command is not found in the **Available commands** list.

**The request/response examples:**

| **Command** | **Request Data** | **Response Data** |
|:------------|:-----------------|:------------------|
| attach      | <ul><li>Destination: tab_id</li><li>Content: {"command":"attach"}</li></ul> | <ul><li>Destination: tab_id</li><li>Content: {"command":"attach", "result":result_code}</li></ul> |
| detach      | <ul><li>Destination: tab_id</li><li>Content: {"command":"detach"}</li></ul> | <ul><li>Destination: tab_id</li><li>Content: {"command":"detach", "result":result_code}</li></ul> |
| debugger\_command | <ul><li>Destination: tab_id</li><li>Content: {"command":"debugger_command", "data":debugger_json}</li></ul> | <ul><li>Destination: tab_id</li><li>Content: {"command":"debugger_command", "result":result_code, "data":debugger_response_json}</li></ul> |
| evaluate\_javascript | <ul><li>Destination: tab_id</li><li>Content: {"command":"evaluate_javascript", "data":"some_javascript"}</li></ul> | none              |
| navigated   | none (event)     | <ul><li>Destination: tab_id</li><li>Content: {"command":"navigated", "result":0, "data":"new_url"}</li></ul> |
| closed      | none (event)     | <ul><li>Destination: tab_id</li><li>Content: {"command":"closed", "result":0}</li></ul> |

<p></p>

# Deprecation And Removal #
The new debug protocol ["WebKit Remote Debug Protocol"](WekKitProtocol.md) is being developed to succeed ChromeDevTools protocol. ChromeDevTools protocol should be considered _deprecated_. Google Chrome/Chromium browser discontinued its support in 17.0.950.`*` version (between developer builds 111534 and 111559).

The [SDK](ChromeDevToolsSdk.md) and EclipseDebugger are going to simultaneously support both protocols behind the single interface starting from [0.3.0 version](Release_0_3_0.md).
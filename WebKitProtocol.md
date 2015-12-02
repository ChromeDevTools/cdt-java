## Introduction ##
"WebKit Remote Debugging Protocol" is a Google Chrome/Chromium debug protocol. You will find its description at http://developers.google.com/chrome-developer-tools/docs/remote-debugging.

This protocol is natively used by in-browser Developer Tools. Historically it succeeds ChromeDevToolsProtocol.

Here its Java client implementation is called [WIP](WIP.md) (in source code and in docs).

### How to start Google Chrome/Chromium ###
```
chrome --remote-debugging-port=<port>
```

### How to attach from [Debugger](EclipseDebugger.md) ###
A launch configuration named "WebKit Protocol" should be created (see [tutorial](DebuggerTutorial#Connect.md)).

### How to attach from SDK ###
Use the following create method:
```
org.chromium.sdk.wip.WipBrowserFactory.INSTANCE.createBrowser(...)
```

## WIP Backends ##
Note that the project contains several alternative protocol implementations for the different versions. They are deployed as "WIP Backends" (see [main article](WipBackends.md)).

### Protocol Name ###
Protocol is called 'WebKit Remote Debuggin Protocol'. You can be sure that you are using this protocol, if you are starting Google Chrome/Chromium with the following parameter:
`--remote-debugging-port=<port>`

Compare with [ChromeDevTools Protocol](ChromeDevToolsProtocol#Protocol_Name.md).


## References ##
Main page: http://developers.google.com/chrome-developer-tools/docs/remote-debugging.

Blog posts:
  * [Remote debugging with Chrome Developer Tools](http://blog.chromium.org/2011/05/remote-debugging-with-chrome-developer.html), The Chromium Blog, May 9th, 2011,
  * [WebKit Remote Debugging](http://www.webkit.org/blog/1620/webkit-remote-debugging/), Surfin' Safari, May 9th, 2011.
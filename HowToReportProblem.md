# How to Report A Problem #
If you got a problem with the project, don't hesitate to report it. The chances are we might be unaware of the issue since there are quite a number of protocol/platform versions.

## Where? ##
What seems a certain bug should go straight to [http:../issues Issues]. More questionable issues could be reported and discussed in the [dedicated group](http://groups.google.com/group/chromedevtools-dev) (you will need to join the group first).

## What? ##
As usual, we need some information to reproduce the problem. The most valuable item is an **exception stacktrace**. Note that Eclipse may only display something like “Internal error” and nothing more. In this case the corresponding stacktrace could be found manually either
  * in the “Error Log” view (preinstalled in most Eclipse configurations): Window | Show View | Other → General → Error Log, or
  * in the Eclipse log file `.metadata/.log` found below your workspace directory.

Other helpful data points you can provide are:
  * ChromeDevTools version.
  * Protocol used: [old Chrome protocol](ChromeDevToolsProtocol.md), [WebKit Remote Debugging Protocol (WIP)](WIP.md), Standalone connection.
  * Name and version of your debuggee: Google Chrome, Chromium, Node.JS, etc.

## How to Investigate Myself? ##
You may want to look into what's going wrong yourself. We shouldn't mention how valuable it could be with certain sorts of bugs (for example platform-specific). If you are ok with Java and Eclipse, debugging should be easy. There are some steps to start a debugger summarized [here](HowToDebug.md).
# Google Chrome Developer Tools for Java #

## <font color='red'>The project has been discontinued</font> ##

This project provides debugging tools for developers writing applications that run on Google Chrome or its open-source version [Chromium](http://www.chromium.org/Home) or any V8-based application. The tools enable you to debug JavaScript inside these browsers over the TCP/IP protocol. If you are looking for information on the Google Chrome browser built-in Developer Tools, please have a look at this [official primer](http://code.google.com/chrome/devtools).

This project comprises:
  * A [ChromeDevTools SDK](ChromeDevToolsSdk.md) that provides a Java API that enables a debugging application to communicate with a Google Chrome browser from the localhost using the TCP/IP-based [Google Chrome Developer Tools Protocol](ChromeDevToolsProtocol.md) or [WebKit Remote Debugging Protocol](WIP.md).
  * An [Eclipse debugger](EclipseDebugger.md) that uses the SDK. This debugger enables you to debug JavaScript running inside Google Chrome tabs from the Eclipse IDE.

As the SDK and Eclipse debugger are written in Java, this project is of most benefit to developers who use Java tools for debugging web applications inside Google Chrome. You can use the SDK to write your own debugger that uses the protocol to communicate with Google Chrome.

## What's new ##
Version 0.3.8 has been released.  [What's new](http://code.google.com/p/chromedevtools/wiki/Release_0_3_8)? [How to install](http://code.google.com/p/chromedevtools/wiki/HowToInstall)?

## Using the Eclipse Debugger ##

Prerequisites:
  * Eclipse version 3.5 or later (minimal configuration is Eclipse Platform).
  * Java Vm version 1.6 or later.
  * Google Chrome (or Chromium) version `3.*.*.*` or later (the latest version can be retrieved from the [Dev Channel](http://www.google.com/chrome/eula.html?extra=devchannel)). See [version compatibility table](VersionCompatibility.md) for details.

Installation:
  * [Download](http://code.google.com/p/chromedevtools/wiki/HowToInstall) Debugger or [build it from sources](http://code.google.com/p/chromedevtools/wiki/HowToBuild).

How to Debug:
  1. Close all windows of Google Chrome or Chromium browsers. (Debug command-line option doesn't work for secondary browser processes).
  1. Start the Google Chrome (or Chromium) browser as: <br><code>chrome --remote-debugging-port=9222</code> (or some other <i>debugger port</i> of your choice.)<br>Make sure your firewall blocks incoming connections to the chosen port from other machines. Open the URL you want to debug.<br>
<ol><li>Start Eclipse and create a new debug configuration (under Run | Debug Configurations...) from the "WebKit Protocol" configuration type (you might need to switch to the Debug perspective first):</li></ol>

<img src='http://chromedevtools.googlecode.com/svn/wiki/rel036-debug-configurations-remote.svg' />
<br />
(Note that failing to see these configuration types is a very likely sign of an incorrect Java VM version).<br>
<br>
Then specify the <i>debugger port</i> you have chosen, in the "Remote" tab. Also you need to select a proper WebKit protocol version in <i>Wip backend</i> field.<br>
<ol><li>Start debugging. When prompted, select the tab to debug and click the "OK" button.<br>
</li><li>In a short while, you will see the browser script sources in a new project.</li></ol>

See a <a href='http://code.google.com/p/chromedevtools/wiki/DebuggerTutorial'>Tutorial</a> for more details. If you found a bug or some other problem, please, <a href='HowToReportProblem.md'>report to us</a>.
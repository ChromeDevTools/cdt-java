Google Chrome Developer Tools for Java
======================================

The project has been discontinued
---------------------------------

This project provides debugging tools for developers writing applications that run on Google Chrome or its open-source version [Chromium] or any V8-based application. The tools enable you to debug JavaScript inside these browsers over the TCP/IP protocol. If you are looking for information on the Google Chrome browser built-in Developer Tools, please have a look at this [official primer].

This project comprises: \* A ChromeDevTools SDK that provides a Java API that enables a debugging application to communicate with a Google Chrome browser from the localhost using the TCP/IP-based Google Chrome Developer Tools Protocol or WebKit Remote Debugging Protocol. \* An Eclipse debugger that uses the SDK. This debugger enables you to debug JavaScript running inside Google Chrome tabs from the Eclipse IDE.

As the SDK and Eclipse debugger are written in Java, this project is of most benefit to developers who use Java tools for debugging web applications inside Google Chrome. You can use the SDK to write your own debugger that uses the protocol to communicate with Google Chrome.

What’s new
----------

Version 0.3.8 has been released. [What’s new]? [How to install]?

Using the Eclipse Debugger
--------------------------

#### Prerequisites: 
* Eclipse version 3.5 or later (minimal configuration is Eclipse Platform). 
* Java Vm version 1.6 or later. 
* Google Chrome (or Chromium) See version compatibility table for details.

#### Installation: 
* [Download][How to install] Debugger or [build it from sources].

#### How to Debug: 

1. Close all windows of Google Chrome or Chromium browsers. (Debug command-line option doesn’t work for secondary browser processes). 
1. Start the Google Chrome (or Chromium) browser as: `chrome --remote-debugging-port=9222` (or some other *debugger port* of your choice.) Make sure your firewall blocks incoming connections to the chosen port from other machines. Open the URL you want to debug.
1. Start Eclipse and create a new debug configuration (under Run | Debug Configurations…) from the “WebKit Protocol” configuration type (you might need to switch to the Debug perspective first):

![image](https://cloud.githubusercontent.com/assets/39191/13662405/07f824f0-e64f-11e5-9d3c-56b219255d8d.png)

(Note that failing to see these configuration types is a very likely sign of an incorrect Java VM version).
Then specify the *debugger port* you have chosen, in the “Remote” tab. Also you need to select a proper WebKit protocol version in *Wip backend* field.

1.  Start debugging. When prompted, select the tab to debug and click the “OK” button.
2.  In a short while, you will see the browser script sources in a new project.

See https://github.com/ChromeDevTools/cdt-java/wiki/Debugger-Tutorial for more details. If you found a bug or some other problem, please, report to us.

  [Chromium]: http://www.chromium.org/Home
  [official primer]: https://devtools.chrome.com
  [What’s new]: http://code.google.com/p/chromedevtools/wiki/Release_0_3_8
  [How to install]: http://code.google.com/p/chromedevtools/wiki/HowToInstall
  [Dev Channel]: https://www.google.com/chrome/browser/desktop/index.html?extra=devchannel
  [build it from sources]: https://code.google.com/archive/p/chromedevtools/wikis/HowToBuild.wiki

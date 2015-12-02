# Google Chrome Developer Tools for Java. Release 0.3.4. #

This release succeeds the [release 0.3.2](Release_0_3_2.md).

## Overview ##
This is mostly a bugfix release. It covers issues 56-60. It also introduces several minor changes to [Debugger](EclipseDebugger.md) and SDK. It also have some [known issues](#Known_Issues.md).

## Old Debug Configuration Converter ##
Since Google Chrome/Chromium 17.0.950.`*` old debug protocol support is discontinued (see [details](ChromeDevToolsProtocol#Deprecation_And_Removal.md)). [WebKit Remote Debugging Protocol](WIP.md) should be used instead. When [connecting](DebuggerTutorial#Connect.md) from Debugger `WebKit Protocol` launch configuration type should be used.

Old `Chromium JavaScript` launch configurations will now have a "Deprecation" tab warning that the old protocol now goes. It also has a button "Copy Launch Configuration" that should help transfer old configurations to the new type.

## Property Descriptor Support ##
JavaScript property descriptors now got a clear support in the project. Currently it is only supported by WebKit protocol.

In Debugger, object properties with getters now have their values displayed. Small auxiliary command was added to a property context menu: "Watch Property Descriptor". Since Eclise UI can't seem to have a proper display for both property value and property descriptor, this action allows to watch property descriptor on request. It merely creates for you a new expression in "Expressions" view.

In SDK a new interface [org.chromium.sdk.JsObjectProperty](http://chromedevtools.googlecode.com/svn//trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/JsObjectProperty.html) was added.

## Javadocs ##
See here for [version 0.3.4](http://chromedevtools.googlecode.com/svn/!svn/bc/938/trunk/plugins/org.chromium.sdk/javadocs/index.html).

## WIP Backends ##
As WebKit Remote Debugging Protocol matures, we are gradually switching from WebKit revision numbers to protocol official version names. See table below.

#### Backend Set Version 0.1.6 ####
| id | Protocol version | Chrome version | New features |
|:---|:-----------------|:---------------|:-------------|
| WK@97678 | 0.1              | 16.0.912.`*`   |              |
| WK@102439 | n/a              | 17.0.963.3+    | function position, property descriptor |
| Protocol 1.0 | 1.0              | 18.`*`.`*`.`*` | function position, property descriptor |

## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.5 minimum, Eclipse 3.6 is also tested.                    |
| **Java:**                   | J2SE-1.6 or newer.                                                   |

Optional:
| **JSDT version:**<br>(feature <i>JavaScript Development Tools</i>) <table><thead><th> 1.2.0 or newer<br>(corresponds to Eclipse 3.6).</th></thead><tbody></tbody></table>

<h1>Known Issues</h1>
Debugger won't start with WebKit Protocol with Backend "Protocol 1.0" or "WK@102439" if "Show debug network communication console" is disabled. Issue <a href='http://code.google.com/p/chromedevtools/issues/detail?id=65'>#65</a>. Quick work-around: enable "Show debug network communication console".
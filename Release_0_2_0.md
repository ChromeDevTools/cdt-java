# Google Chrome Developer Tools for Java. Release 0.2.0. #

As usual, this release contains two parts: an independent Java [SDK](ChromeDevToolsSdk.md) library that implements [Chrome/V8 debug protocol](ChromeDevToolsProtocol.md) client, and an Eclipse [“Debugger”](EclipseDebugger.md) feature that allows debugging of Chrome/V8 JavaScript programs from inside Eclipse.

This release succeeds the [release 0.1.6](Release_0_1_6.md).


## Overview ##
In this release, we have added several long-waited features. The SDK API has a number of technical changes, so we have decided to bump up the minor version to reflect the compatibility breakage.

## Load Full Value ##
Normally, the [Debugger](EclipseDebugger.md) truncates string values longer than 80 characters; you can tell it by a typical cut-off in the end of string: `... (length: <actual length>)`. This release allows the full string value to be loaded on demand. In the _Variables_ or _Expressions_ view, select a truncated string value and choose _“Load Full Value”_ from its context menu. Note that the implementation is somewhat cautious: it will reload the string with a new length limit of 65536 chars. If that’s not enough for you, simply repeat the action once or twice.

However, Eclipse may not be very usable with a string of an enormous length. If only a specific part of the string is needed, you can try to manually add a helper expression. For example, type in `“value.substring(2978 - 100, 2978 + 100)”` to see a small string chunk around an interesting point.

## Break On Exception ##
A JavaScript VM may now be instructed to stop on each exception thrown. All exceptions fall into 2 groups:
  * _caught_ exceptions, which are to be caught by a `catch` clause somewhere in the script, and
  * _uncaught_ exceptions, which terminate script execution altogether and return the control to the calling application.

Both types of exceptions are controlled from the _"Break on JavaScript Exception"_ item in the launch context menu. (a _"launch"_, a.k.a. a _"debug session"_, is each of the top-level elements in the _Debug_ view).

## Basic JSDT Integration ##
The previous release expanded out of the VirtualProject premises to let you work with regular workspace source files directly. However, the default JavaScript editor from the JSDT project could not really be used: all breakpoints set in the editor made no effect on the running VM. Now we fix this problem. The breakpoints added from the JavaScript editor should work fine. Still, you may notice that they are slightly different from this in [VirtualProject](VirtualProject.md)s; this shouldn't make any trouble though.

Please note that this integration is deployed as a separate Eclipse feature: _"Chromium JavaScript Debugger Bridge to JSDT"_. This feature comes together with the others, but you need to manually select it when installing/upgrading. A separate feature is needed to keep core Debugger deployable independent of a JSDT installation. See version dependency table below.

## Temporarily Format Source ##
A new experimental feature that adjoins the JSDT integration works with JavaScript scripts that lack formatting. In order to format your regular work files you may run the JSDT formatter manually, and the new feature is to be used with scripts from a VirtualProject. Open the context menu on a script under VirtualProject and choose the _V8 Debugging -> Temporarily Format Source_ action.
This will call the JSDT formatter with default settings and put the result into a new script next to the original one. This script is called "temporary", as it will be gone once the debug session is finished. Before that happens, you can step over it or set breakpoints as usual.

This feature is still experimental and may not always work properly. In particular, the formatter may fail on some scripts. Currently, there is a fall-back: a secondary, extremely simplistic formatter that may produce an output of extremely poor quality, which can no longer be a valid JavaScript.

## Miscellaneous Small Changes ##
#### Virtual Project Icon ####
Since this release, the [VirtualProject](VirtualProject.md) folder is visually distinguishable by a small [Chromium](http://www.chromium.org) logo next to its icon.

#### Skip All Breakpoints ####
The standard "Skip All Breakpoints" mode (see the corresponding button in the _Breakpoints_ view) is now supported by the [Debugger](EclipseDebugger.md). It disables breakpoints directly in VM, so all breaks get skipped without calling back the debugger.

## Protocol Change ##
Since 7.0.511.4 ([revision #58796](https://code.google.com/p/chromedevtools/source/detail?r=#58796)), Chromium/Google Chrome has changed its debug protocol with regard to the handling of non-ASCII characters. All previous versions of Debugger/SDK will not work with the new protocol if there are any non-ASCII symbols in scripts or variable values -- the session will hang or fail abruptly. This release has all necessary changes and should work with both old and new revisions of the protocol.

This release is recommended for use with any version of Chromium/Google Chrome.

## SDK Changes ##
SDK changes reflect the new features described above. Some of the most noticeable changes are:
  * source position API refactored,
  * live editing interfaces made regular,
  * connection logging API switched to bytes from characters.

## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.4 minimum, Eclipse 3.5 and Eclipse 3.6 are also tested.   |
| **Java:**                   | J2SE-1.6 or newer.                                                   |

Optional:
| **JSDT version:**<br>(feature <i>JavaScript Development Tools</i>) <table><thead><th> 1.2.0 or newer<br>(corresponds to Eclipse 3.6).</th></thead><tbody>
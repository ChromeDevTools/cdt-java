# Google Chrome Developer Tools for Java. Release 0.3.8. #

This release succeeds the [release 0.3.6](Release_0_3_6.md).

## Overview ##
This release is a minor one. It makes a change to the Debugger UI and also fixes bugs [74](http://code.google.com/p/chromedevtools/issues/detail?id=74)
and
[76](http://code.google.com/p/chromedevtools/issues/detail?id=76).

## Break on Exception ##
The debugger feature that instructs JavaScript VM to pause on exception throwing event has got a new form. Previously, it was controlled from the context menu on LaunchElement. Since this release, a more traditional Eclipse approach is implemented.

To enable breaking on exception, the user should add a _JS Exception_ breakpoint: `Run|Add V8/Chrome JavaScript Exception Breakpoint` menu item. This breakpoint has regular enable/disable semantics, additionally overridden by the `Skip All Breakpoints` control and, what's most useful, its state is persisted by the workspace. This means that once you have enabled break on exception, this will hold for the following debug sessions.

Even though the UI allows you to create multiple breakpoints of this kind, you should hardly need more than one.

## Slight Changes to Launch Configuration ##
You might notice that the `Breakpoint sync on launch` control has disappeared from the launch configuration dialog. This is due to such synchronization only making sense for V8 Standalone configurations (where this control is left untouched).

## WIP Backends ##
[WIP](WIP.md) backends haven't been changed in this release.

#### Backend Set Version 0.1.10 ####
| id | Chrome version | New features |
|:---|:---------------|:-------------|
| Protocol 1.0 | 18.`*`.`*`.`*` |              |
| WK@118685 | 21.0.1180.`*`  | function scope, primitive value |
| WK@120709 |22.0.1188.`*`   | function scope, primitive value, restart frame |

## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.5 minimum, Eclipse 3.6 and 4.2 were also tested.          |
| **Java:**                   | J2SE-1.6 or newer.                                                   |

Optional:
| **JSDT version:**<br>(feature <i>JavaScript Development Tools</i>) <table><thead><th> 1.2.0 or later<br>(corresponds to Eclipse 3.6).</th></thead><tbody>
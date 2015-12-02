# Google Chrome Developer Tools for Java. Release 0.3.2. #

This release succeeds the [release 0.3.0](Release_0_3_0.md).

## Overview ##
This is a minor bugfix release. It covers issues 44, 53, 54.

It also introduces new WIP backend for Google Chrome 17. It uses more recent WebSocket protocol (HyBi-17). It also implements missing feature "Open Function" (see [0.1.6 release notes](Release_0_1_6.md)).

## WIP Backends ##

The list of available WIP backend versions. They get installed with the "ChromeDevTools SDK WIP Backends" feature.

#### Version 0.1.4 ####
| id | Chrome version | Chromium dev build |
|:---|:---------------|:-------------------|
| WK@93101 | 15.0.855.`*`, 15.0.874.`*` | 97053, 99889       |
| WK@97678 | 16.0.912.`*`   | 106036             |
| WK@102439 | 17.0.963.3+    | n/a                |

## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.5 minimum, Eclipse 3.6 is also tested.                    |
| **Java:**                   | J2SE-1.6 or newer.                                                   |

Optional:
| **JSDT version:**<br>(feature <i>JavaScript Development Tools</i>) <table><thead><th> 1.2.0 or newer<br>(corresponds to Eclipse 3.6).</th></thead><tbody>
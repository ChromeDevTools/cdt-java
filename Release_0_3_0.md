# Google Chrome Developer Tools for Java. Release 0.3.0. #

This release succeeds the [release 0.2.2](Release_0_2_2.md).

## Overview ##
The primary topics covered by this release are the support of emerging [WebKit Remote Debugging Protocol](WIP.md) in Chrome and simplified 'auto-detect' source look-up implementation. SDK interfaces have been changed moderately but incompatibly, so the minor version number was bumped up. On the update site, a new feature called "ChromeDevTools SDK WIP Backends" is added and is recommended for install.

## Debugger ##
### Auto-Detect Source Look-Up Mode ###
Since several releases ago, Debugger supports the source look-up -- using developer source files from the workspace rather than temporary auto-created files under a VirtualProject. However the set-up procedure was quite involved. It was a price for the accurateness of the 2-way source matching.

This release introduces a new, much more user-friendly look-up mode called 'Auto-detect' (the old one is now called 'Exact match'). It employs fuzzier algorithms and deals only with short file names.
You can enable it from the Debug Configurations dialog.

1. In the "Remote" tab choose "Auto-detect" mode.

![https://chromedevtools.googlecode.com/svn/wiki/rel030-debug-configurations-remote.png](https://chromedevtools.googlecode.com/svn/wiki/rel030-debug-configurations-remote.png)

2. In the "Source" tab, add one or more folders holding your working files.

If all of your files in the project are called 'foo.js', this probably won't work correctly. However, for most projects there shouldn't be any problems.

Anyway, should you occasionally have a few files with the same name in different directories, there is a fall-back: you can configure Debugger to take a directory name into consideration for this particular file.

1. Open file properties (Alt+Enter).

![https://chromedevtools.googlecode.com/svn/wiki/rel030-script-properties.png](https://chromedevtools.googlecode.com/svn/wiki/rel030-script-properties.png)

2. Use "Less" and "More" buttons to add or remove file path components. By default, only the file name is used, but you may add its parent directory or a longer path. All selected file path components will be used for matching this file.

### Experimental WebKit Remote Debugging Protocol (WIP) ###
This release adds an [experimental support](WIP.md) for a new debug protocol used in Google Chrome/Chromium. This protocol should eventually supersede the currently used ["DevTools" protocol](ChromeDevToolsProtocol.md). The subproject got a local codename [WIP](WIP.md).

From the user point of view switching to the new protocol should be virtually transparent. The most obvious change is ref-id's in Variables/Expressions views: now they have a different format and are completely random, so that the same object may appear with different ids in the same view.

In order to try out the new protocol, make sure you have installed the "ChromeDevTools SDK WIP Backends" feature. This reveals how WIP implementation is currently deployed. There are several (incompatible) versions of protocol that we currently support, and you can choose between them: the older protocol version corresponds to the current stable release of Chrome (13), but it misses some features; the newer protocol version comes with all features, but the browser is in yet-not-stable development state. There is a separate feature "ChromeDevTools SDK WIP Backends" that installs the current collection of backends;  updating this feature should install newer WIP backends in the future as the protocol evolves.

As of 0.3.0, you need to choose the correct backend manually. See the [table](#WIP_Backends.md) below.

  1. Create a debug configuration of the "WebKit Protocol" type.
  1. In the "Remote" tab select the proper backend version in the drop-down list.

### Exceptions ###
The Debugger now better supports exceptions. If you stopped on an exception (see `Break on JavaScript Exceptions` in LaunchElement) or got an exception as a result of an expression evaluation, now you have the access to the exception value: you can expand its properties, or pin-point an object, or do whatever is available from the "Variables" view.

### Object From 'with' Statement ###
Similar to the exceptions, if you paused inside the `with` statement, you can see its argument in the "Variables" view and get the full access to it. Note that if your `with`-argument is a primitive value, JavaScript VM may be not fully accurate and return a wrapper-object instead of the original value.

### Separate Folders For Unnamed Scripts ###
All scripts currently loaded in the VM are shown under a VirtualProject. To keep things more clear, scripts without a name (e.g. those eval'd) are put in a separate folder.

### JSDT Integration Feature Notes ###
"Chromium JavaScript Debugger Bridge to JSDT" feature became less important. It used to bind Debugger with JSDT native breakpoints. However Eclipse allows to create ChromeDevTools breakpoints right in the JSDT editor (in any editor, in fact); see `Run | Breakpoint Types`. Therefore, this binding code was dropped. The feature, however, is still useful if you want to use the JSDT formatter in `V8 Debugging | Temporarily Format Source` action.

## SDK ##
The main change in SDK is the support of WebKit Remote Debugging Protocol that is locally called [WIP](WIP.md).

See also a new SdkTutorial and generated [javadocs](http://chromedevtools.googlecode.com/svn/!svn/bc/936/trunk/plugins/org.chromium.sdk/javadocs/index.html).

Other changes are moderate interfaces redesign, support for clearing internal property caches (see [issue#44](http://code.google.com/p/chromedevtools/issues/detail?id=44)) and a faster JSON parser that is implemented without Java reflection.

This SDK version does not support any new features that [WebKit Remote Debugging Protocol](WIP.md) offers. However this support is planned for the subsequent releases.

## WIP Backends ##

The list of available WIP backend versions. They get installed with the "ChromeDevTools SDK WIP Backends" feature.

#### Version 0.1.2 ####
| id | Chrome version | Chromium dev build |
|:---|:---------------|:-------------------|
| WK@91698 | 14.0.835.`*`   |                    |
| WK@93101 | 15.0.855.`*`, 15.0.874.`*` | 97053, 99889       |
| WK@97678 | 16.0.912.`*`   | 106036             |

#### Version 0.1.0 (obsolete) ####
| id | Chrome version | Debugger limitations | SDK limitations |
|:---|:---------------|:---------------------|:----------------|
| WK@87771 | 13.0.782.112 (stable) | Auto-detect mode is missing,<br>live edit preview is missing. <table><thead><th> 'RegExp' breakpoint targets not supported,<br>live edit preview is missing. </th></thead><tbody>
<tr><td> WK@91698 </td><td> 14.0.835.<code>*</code> (beta) </td><td>                      </td><td>                 </td></tr>
<tr><td> WK@93101 </td><td> 15.0.855.<code>*</code><br>dev build#97053 </td><td>                      </td><td>                 </td></tr></tbody></table>

<h2>System Requirements</h2>
<table><thead><th> <b>Eclipse configuration:</b>  </th><th>Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). </th></thead><tbody>
<tr><td> <b>Eclipse version:</b>        </td><td>  Eclipse 3.5 minimum, Eclipse 3.6 is also tested.                    </td></tr>
<tr><td> <b>Java:</b>                   </td><td> J2SE-1.6 or newer.                                                   </td></tr></tbody></table>

Optional:<br>
<table><thead><th> <b>JSDT version:</b><br>(feature <i>JavaScript Development Tools</i>) </th><th> 1.2.0 or newer<br>(corresponds to Eclipse 3.6).</th></thead><tbody>
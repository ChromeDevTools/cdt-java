# Google Chrome Developer Tools. Release 0.1.6. #
As usual this release contains 2 parts: independent Java [SDK](ChromeDevToolsSdk.md) library that implements [Chrome/V8 debug protocol](ChromeDevToolsProtocol.md) client and Eclipse [“Debugger”](EclipseDebugger.md) feature that allows debugging Chrome/V8 JavaScript programs from inside Eclipse. Naturally, Debugger uses SDK. In this document we cover only the changes in Debugger. SDK changes should be self-explanatory in the source code and are not mentioned here.


## Overview ##
For this release we continued our efforts to develop our debugger and to better integrate it with Eclipse IDE ecosystem. One of our main goals was to link our debugger with source files in Eclipse workspace. However at the moment debugger only works with system Text Editor, and is not really integrated with [JSDT](http://wiki.eclipse.org/index.php/ATF/JSDT).

## Workspace Source Files ##
When you connect to a remote JavaScript VM, debugger automatically downloads all scripts and shows them under temporary virtual project. However you typically not only want to debug a code, but in parallel to fix or develop it. You may have all your script source files on the local machine and even have them open in Eclipse under some project. In this case it is especially useful to have debugger use that (working) copy of your scripts instead of scripts in temporary project; so that you edit the same files that you debug.

You can configure this starting from the current release. First let’s see how V8 identifies those remote scripts. While debugging a script in the virtual project open “Properties” (context menu on file in “Project Explorer” or with Alt+Enter in an editor).

![http://chromedevtools.googlecode.com/svn/wiki/rel016-script-properties.png](http://chromedevtools.googlecode.com/svn/wiki/rel016-script-properties.png)

Here we see that VM identifies your script as URL ` http://server:8080/misc/utils.js ` (from this URL you seem to be running a local web server; there may be other configurations). Depending on your working files layout, URLs like this should be mapped either as `http://server:8080/misc/** => <your misc folder>/**` or as `http://server:8080/** => <your project folder>/**`

You can configure this in “Source” tab of your debug launch configuration.

![http://chromedevtools.googlecode.com/svn/wiki/rel016-debug-configurations-source.png](http://chromedevtools.googlecode.com/svn/wiki/rel016-debug-configurations-source.png)

Add a new source container with identifier mapping and make sure it is above the “Default” container (here “Default” container represents the virtual project that holds downloaded scripts). In the dialog window, choose “JavaScript Source Name Mapper” from the list. This container converts URL (or any id) into a file name and looks it up in another container, here called “target container”.

![http://chromedevtools.googlecode.com/svn/wiki/rel016-source-name-mapper.png](http://chromedevtools.googlecode.com/svn/wiki/rel016-source-name-mapper.png)

Depending on your configuration, enter the common URL prefix (e.g. `http://server:8080/`) and specify the target container (choose type and configure it). Note that you may need to restart your debug launch after this.

## Multiple Debug Sessions Case ##
Eclipse allows simultaneous debug launches. For example in your browser, you may open the web page in 2 tabs and start debugging both tabs. This comprises an interesting scenario if you have configured sources for working files. A single working file may be associated with the script in several VMs at the same time. E.g. when you set a breakpoint in your file, both VMs react. In some cases UI has to explicitly support multiple debug launches.

## Breakpoints in Workspace ##
Now you can set a breakpoint in your working files. Unlike files in virtual projects, the working files are persistent and so are their breakpoints. If you restart your V8 VM or reload page in Chromium, Debugger will restore your breakpoints upon connecting to it. Note that in an exotic case, the VM you are connecting to may have its own set of breakpoints if it has some history before this debug session. You may specify what should happen in this case in the launch configuration (under “Breakpoints sync on launch”):

![http://chromedevtools.googlecode.com/svn/wiki/rel016-debug-configurations-remote.png](http://chromedevtools.googlecode.com/svn/wiki/rel016-debug-configurations-remote.png)

Additionally, you may synchronize the breakpoints at any time you want from the context menu of your launch (in “Debug” view): “Synchronize JavaScript Breakpoints”.

Another important point is that with a working file and its counterpart in the virtual project you have 2 copies/representations of your script. While you can set a breakpoint on any copy of the script file, Debugger will always prefer only one representation (according to priority setting in Source tab of the launch configuration) when it needs to highlight the current line of code. This means that debugger may stop on a breakpoint, but the breakpoint itself won’t be visible, because it was set in the other copy of the script file.

This release comes with a significant limitation, which allows dealing with working file breakpoints only from system Text Editor. In particular it means that should you open a file in JSDT editor (“JavaScript Editor”), you won’t be able to operate with breakpoints (more precisely, JSDT has its own breakpoints, that are not supported yet). This problem should be fixed in future releases.

## Variables View Features ##
There are just some minor changes here.
  1. With array values now you can see non-index properties. E.g. you may create an array and add property “foo” to it. Now such properties become visible.
  1. If your value has type “Function” now you can navigate to this function from the context menu item “Open Function”.

## Live Editing ##
This is a highly experimental feature that allows you to edit your script while it is being executed by V8. After you have edited your script you may try to push the changes back to VM and see whether it has succeeded. In editor context menu choose “V8 Debugging” -> “Push Source Changes to VM”. An additional menu item “Preview and Push Source Changes to VM…” gives you a more verbose UI that should help understand what is happening.

Obviously, there are some limitations to this feature. First, the principal limitations: JavaScript allows only a few kinds of declarations that you can edit, e.g. you can’t edit a script and add a new field to a type because there is no place where fields are actually declared. Second, this feature is still under development so there are many things that may not be implemented yet.

The best use of live editing is to change function algorithms by editing function body:

Before:
```
  …
  manager.addCase(name, id, function() { throw “This is impossible!”; } );
  …
```
After:
```
  …
  manager.addCase(id, name, undefined);
  …
```

## JSDT Integration Status ##
As we are making steps towards linking with source files in a workspace, we believe that it should imply integration with JSDT. However we are not there yet and the integration is left for future releases. As of 0.1.6 there are several incompatibilities with JSDT, most notably you won’t be able to work with breakpoints from inside JSDT source editor.

## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.4 minimum, Eclipse 3.5 and Eclipse 3.6 are tested.        |
| **Java:**                   | J2SE-1.5 or newer.                                                   |
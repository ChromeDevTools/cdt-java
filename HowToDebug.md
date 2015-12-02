# How to Debug #

(For tutorial on how to use ChromeDevTools [Debugger](EclipseDebugger.md) to debug JavaScript see the [dedicated page](DebuggerTutorial.md). This page is about debugging Debugger itself.)

Since the project is written in Java, it must be relatively easy to debug it from a Java IDE.

You may want to look inside the running code if you are wondering how it works or if you see a problem and wish to contribute to the project by investigating.

See also HowToBuild, HowToReportProblem.

## Debugging EclipseDebugger ##
Our Debugger is an Eclipse plugin (technically a set of plugins), so you have to start the entire Eclipse (here called _debuggee_) under a Java debugger. Fortunately Eclipse framework is quite prepared for this. What you need is just another Eclipse instance (here called _debugger_) with installed `Plugin Development Environment` feature (Eclipse Classic has it preinstalled).

_Debuggee_ must have usual `Chromium JavaScript Remote Debugger` feature plus `ChromeDevTools Project Source Code` feature (one prepared specifically for such occasions). (We assume you are _installing_ debugger, not [building it from sources](HowToBuild.md), otherwise you don't need such instructions.)

Start _debugger_. First, some steps to prepare it:

  1. In `Preferences` under `Plug-in Development` set up new `Target Platform`: press `Add`, `Start with empty`, `Add Installation` and enter a path to your _debuggee_ installation directory. To double-check, it should find some plugins while in the same dialog window.
  1. Open `Plug-in Development` perspective. On the left choose `Plug-ins` tab. In the tab choose all `org.chromium.*` plugins (or better all plugins) and from context menu make `Add To Java Search` on them.
  1. Go to `Java` perspective. In `Package Explorer` tab in triangle menu choose `Filters` and deselect `External plug-ins project`.
  1. Open `Debug Configurations` dialog and create new `Eclipse Application` launch debug configuration.
  1. It's ready to start now.

All the classes should be under `External Plug-in Libraries/External Plug-ins` in `Package Explorer`. They are also available from the regular `Open Type (Ctrl+Shift+T)` dialog window.

## Where to Start Debugging ##
Finding a place for the first breakpoint in an unfamiliar codebase may be tricky. If you have a stacktrace (HowToReportProblem tells how to fetch one from an error message), everything should be quite straightforward. In other cases, there is probably no general instruction. We welcome you to ask in [discussion group](http://groups.google.com/group/chromedevtools-dev) if clueless.
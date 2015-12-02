# Related Tools #

This project implements JavaScript [debugger](EclipseDebugger.md) for Eclipse platform. It relies on Eclipse ecosystem for other capabilities, in particular source file editor. While the standard Eclipse Platform configuration can be enough for virtually all [Debugger features](EclipseDebuggerFeatures.md) to work, additional third-party software could improve JavaScript developer experience.

The following list mentions some software packages with notes about its compatibility with Debugger.

## Base Eclipse Configuration ##
Debugger requires "Eclipse Platform" configuration. In this configuration there are only abstract IDE capabilities, and no particular language/SDK is supported.

Notes:
  * A generic project with .js and .html source files can be created and successfully used with Debugger.
  * `Run | Debug Configuration` menu item is available from `Debug` perspective.
  * A standard Text Editor should be manually chosen when opening .html file (`Open With | Text Editor` in context menu).
  * The Text Editor doesn't create breakpoints, but a breakpoint can always be created from `Run | Toggle Breakpoint` menu.

## Possible Topics (to be done later) ##
  * JSDT
  * Aptana
  * ZendStudio
  * Spket IDE
  * VJET
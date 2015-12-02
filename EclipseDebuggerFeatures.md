Features of EclipseDebugger. See also DebuggerTutorial.



# Basic Capabilities #
  * Breakpoints<br>(It is important you set the right breakpoint type "Chrome/V8 breakpoints" in <code>Run | Breakpoint Types</code> menu).</li></ul>

<ul><li>Step in/over/out<br>
</li><li>Stack context when suspended<br>
</li><li>Mouse on-hover identifier evaluation<br>
</li><li>Text selection evaluation<br>
</li><li>Script sources in workspace files and downloaded from server<br>
</li><li>Exception events reported into the Error Log</li></ul>


<h1>Highlights #

## Debugging on Files That You Are Developing/Source Mapping ##
By default debugger downloads all scripts and puts them under a correspoindg VirtualProject.
However you typically not only want to debug a code, but in parallel to fix or develop it. You may have all your script source files on the local machine and even have them open in Eclipse under some project. In this case it is especially useful to have debugger use that (working) copy of your scripts instead of scripts in temporary project; so that you edit the same files that you debug.

See [how to set up debugger](FeatureDebugOnRealFiles.md) to do so.


## Breakpoints ##
### Provisional Breakpoints ###
Breakpoint can be set before script is loaded or before debugger is connected. Just set a breakpoint on a source in your regular project. This works when source mapping is configured for your debug launch.

### Break On Exception ###
A JavaScript VM may be instructed to stop on each exception thrown. All exceptions fall into 2 groups:
  * _caught_ exceptions, which are to be caught by a `catch` clause somewhere in the script, and
  * _uncaught_ exceptions, which terminate script execution altogether and return the control to the calling application.

Both types of exceptions are controlled by _JS Exception_ breakpoint. You can create one with `Run|Add V8/Chrome JavaScript Exception Breakpoint` menu command.

### Selecting Breakpoint Type ###
The Eclipse IDE can host several different debuggers. Such configurations often cause ambiguity when setting a breakpoint: which debugger should own the new breakpoint? Eclipse provides a UI for this case and it is now supported by ChromeDevTools. You may explicitly choose a type of breakpoint you are about to set in the menu `Run->Breakpoint Types` (choose "Chrome/V8 breakpoints" in a submenu.)

## Load Full Value ##
Normally, the [Debugger](EclipseDebugger.md) truncates string values longer than 80 characters; you can tell it by a typical cut-off in the end of string: `... (length: <actual length>)`. However the full value can be loaded on demand. In the _Variables_ or _Expressions_ view, select a truncated string value and choose _“Load Full Value”_ from its context menu. Note that the implementation is somewhat cautious: it will reload the string with a new length limit of 65536 chars. If that’s not enough for you, simply repeat the action once or twice.

Note that manually entering expressions is sometimes more usable, than downloading a full value: `“value.substring(2978 - 100, 2978 + 100)”`

## Live Editing ##
When your scripts are already running, you can try to edit them and push changes into VM. Edit your script, save it, then choose `V8 Debugging->Push Source Changes to VM`. Additionally you can use `Preview and Push Source Changes to VM...` menu item that gives you some insight how the change is going to be applied.

## ToString ##
In some cases exploring variables can get much easier with their values represented as strings. A single glance at `“[1, 2, 3, 4, 5, 6, 7, 8]”` seems to be a much faster way to learn that the value is an array holding 8 numbers than expanding a tree node in the UI to see its children.

![http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-tostring.png](http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-tostring.png)

For all primitive values, JavaScript pre-defines their string representations. You can define a string representation for your objects by implementing the “toString()” method on their prototypes, much like in Java (see the picture above).


## Pin-Point Value ##
Sometimes it is important to grip a particular object and keep it for several debugger steps. For example, you may watch the `getManager()` expression, but do you know for sure if it returns the same object every time, or the objects returned are all different? Select a value in the Variables or Expressions view and _pin-point_ it, which means **save it as a property of the global object**. You can do this from the context menu: “Pin-Point Value...”:

![http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-pinpoint-dialog.png](http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-pinpoint-dialog.png)

This will save your value in the global object, and you can access it at any time as `my_debug_save.manager1` (a dialog window also lets you add similar watch expressions.)
Some time later you can see how this object has changed its data or compare it with what `getManager()` returns now: `my_debug_save.manager1 === getManager()`.

## Temporarily Format Source ##
You can have your file from VirtualProject formatted. This will only remain effective within the current debug session.
Open the context menu on a script under VirtualProject and choose the `V8 Debugging->Temporarily Format Source` action.
The JSDT formatter will be used and JSDT integration feature should be installed for this.

This feature is still experimental and may not always work properly. In particular, the formatter may fail on some scripts. Currently, there is a fall-back: a secondary, very simplistic formatter that may produce an output of extremely poor quality, which can no longer be a valid JavaScript.
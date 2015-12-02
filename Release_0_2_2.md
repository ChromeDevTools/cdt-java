# Google Chrome Developer Tools for Java. Release 0.2.2. #

This release succeeds the [release 0.2.0](Release_0_2_0.md).

## Overview ##
This release introduces several new Debugger features. The SDK changes back these features up accordingly.

## String Representation ##
In some cases exploring variables can get much easier with their values represented as strings. A single glance at `“[1, 2, 3, 4, 5, 6, 7, 8]”` seems to be a much faster way to learn that the value is an array holding 8 numbers than expanding a tree node in the UI to see its children. This release introduces the string representation of variable values in Debugger.

![http://chromedevtools.googlecode.com/svn/wiki/rel022-string-representation.png](http://chromedevtools.googlecode.com/svn/wiki/rel022-string-representation.png)

For all primitive values, JavaScript pre-defines their string representations. You can define a string representation for your objects by implementing the “toString()” method on their prototypes, much like in Java (see the picture above).

## Host/Port ##
Users have long asked us to allow connections to any remote host instead of `localhost`, which used to be the only option. Even though both Chrome and V8 deny non-local connections for security reasons, this has been made possible on the Debugger side:

![http://chromedevtools.googlecode.com/svn/wiki/rel022-host-port.png](http://chromedevtools.googlecode.com/svn/wiki/rel022-host-port.png)

To make this feature useful, you can set up a small proxy on a remote host, next to the debuggee, and establish a connection to this proxy, so that Chrome/V8 will be accessed from a local process. Some versions of the Unix `nc` (netcat) utility can be used to set up single-connection proxy as easily as:
```
nc -l <proxy_port> -c “nc localhost <port>”
```

## Pin-Point Value ##
Sometimes it is important to grip a particular object and keep it for several debugger steps. For example, you may watch the `getManager()` expression, but do you know for sure if it returns the same object every time, or the objects returned are all different? Now you can select a value in the Variables or Expressions view and _pin-point_ it, which means **save it as a property of the global object**. You can do this from the context menu: “Pin-Point Value...”:

![http://chromedevtools.googlecode.com/svn/wiki/rel022-pinpoint-dialog.png](http://chromedevtools.googlecode.com/svn/wiki/rel022-pinpoint-dialog.png)

This will save your value in the global object, and you can access it at any time as `my_debug_save.manager1` (a dialog window also lets you add similar watch expressions.)
Some time later you can see how this object has changed its data or compare it with what `getManager()` returns now: `my_debug_save.manager1 === getManager()`.

## Breakpoint Types ##
The Eclipse IDE can host several different debuggers. Such configurations often cause ambiguity when setting a breakpoint: which debugger should own the new breakpoint? Eclipse provides a UI for this case and it is now supported by ChromeDevTools. You may explicitly choose a type of breakpoint you are about to set in the menu `Run->Breakpoint Types` (choose "Chrome/V8 breakpoints" in a submenu.)


## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.5 minimum, Eclipse 3.6 is also tested.                    |
| **Java:**                   | J2SE-1.6 or newer.                                                   |

Optional:
| **JSDT version:**<br>(feature <i>JavaScript Development Tools</i>) <table><thead><th> 1.2.0 or newer<br>(corresponds to Eclipse 3.6).</th></thead><tbody>
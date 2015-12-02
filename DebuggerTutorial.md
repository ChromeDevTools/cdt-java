# Install #
First you have to install Eclipse platform from [eclipse.org](http://eclipse.org). Eclipse comes in many different configurations, the Debugger requires “Eclipse platform” or richer. You also could give a try to “Eclipse IDE for JavaScript Web Developers” (JSDT) configuration that provides JavaScript IDE, though it has its limitations.

Then you have to install [Debugger](EclipseDebugger.md) into Eclipse.

Start Eclipse and open menu `Help | Install New Software...`.

Add update site http://chromedevtools.googlecode.com/svn/update/dev/.

You should find “Google Chrome Developer Tools” category in the space below. Under this category choose “Chromium JavaScript Remote Debugger” and optionally debugger bridge to JSDT (only if you have JSDT installed).

Finish installation and restart Eclipse as prompted.

See also HowToInstall.

# Connect #
In Eclipse create a new debug configuration (under `Run | Debug Configurations...`) (you might need to switch to the Debug perspective first).
Several configuration types are supported for different protocols.

![http://chromedevtools.googlecode.com/svn/wiki/rel034-debug-configurations-remote.png](http://chromedevtools.googlecode.com/svn/wiki/rel034-debug-configurations-remote.png)

(Note that failing to see these configuration types is very likely a sign of incorrect Java VM version).

### WebKit Protocol ###
This is default Google Chrome/Chromium debug protocol: [WebKit Remote Debugging Protocol](WIP.md).
You should start browser with the following command-line:
```
chrome --remote-debugging-port=<debug port>
```

### Standalone V8 VM ###
This is for the native V8 debug protocol. There is no standard way to enable it, as V8 is only a library and doesn't control it. A particular application is [responsible for this](http://code.google.com/p/v8/wiki/AddDebuggerSupport).

### Chromium JavaScript ###
This is what historically was the first Google Chrome/Chromium [debug protocol](ChromeDevToolsProtocol.md). It is being phasing out so you probably won't need it.

Once you chose a configuration type, all other settings should be pretty much the same.

However note that for "WebKit Protocol" you will have to manually choose a backend (see [main article](WipBackends.md)).

Then specify the `debugger port` you have chosen, in the "Remote" tab.

Press “Debug”. If the browser has multiple tabs opened, a corresponding tab chooser dialog will appear.  Unless something went wrong, debugger should attach soon and a new object called [launch](LaunchElement.md) should appear in _Debug_ view (make sure you are in _Debug_ perspective by now).

Note that you can also connect to other V8-based applications via “Standalone V8 VM” launch type, provided they have [debug support](http://code.google.com/p/v8/wiki/AddDebuggerSupport) embedded.

# First Steps #
After debugger attached you should find a VirtualProject (in the Project Explorer view). That's where all running **scripts** are.

Set a **breakpoint** on the function you are interested in. Note that it might be important to set correct **breakpoint type**, especially when some other debugger or language support is installed in the same Eclipse. Choose `Chrome/V8 breakpoints` in `Run | Breakpoint Types` menu item in this case.

Make the function with breakpoint run (for example provoke it by clicking something on the webpage) – program should stop there and debugger will put a cursor on the breakpoint line. You also can stop the program by pressing “Suspend” button in _Debug_ view.

Explore current **call stack**. Check local variables or try some expressions. If you have an object, you can expand it in _Variables_ or _Expressions_ view and see its properties. Its proto object is shown under  `__proto__` property. If the object has “toString” method, you can see the **object string representation** in the same view.

If you encounter some value of function type (e.g. some method or a callback), you can navigate to its source: choose `Open Function` in the context menu. At the same time if you want to see the function you paused in as an object, create new "arguments.callee" expression.

If you want to keep an eye on a particular object, you can **pin-point** under some global object property.

Whenever you see a bug in your program you can edit it and also you can try a **live editing** – once you fixed the bug save changes and in the editor context menu try `V8Debugging | Push Source Changes to VM`. This will put your changes right in the running program. V8 cannot do a magic, but in many simple (or not so simple) cases it should work and you won't have to reload your page. The debugger doesn't have to be paused for this to work.

See also [full feature list](EclipseDebuggerFeatures.md).

## Setting up your sources ##

You may expect Debugger to work with your local working files rather than scripts in the VirtualProject. You can do so as described [here](FeatureDebugOnRealFiles.md).

# How to Spy On How It's Working #
What you most easily can do is to overlook our network communication. In launch configuration enable “Show debugger network communication console” (see picture above).
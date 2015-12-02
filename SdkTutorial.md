# ChromeDevTools [SDK](ChromeDevToolsSdk.md) Tutorial #



## Overview ##

SDK is a standalone Java library. It comes as a set of .jar files or as an Eclipse plugins. As of [0.3.0 version](Release_0_3_0.md) it supports ['DevTools' protocol](ChromeDevToolsProtocol.md), native V8 protocol and [WebKit Remote Debugging Protocol](WIP.md). They all are implemented with the common API and differs only in attach interfaces.

WebKit Remote Debugging Protocol support is a new subproject called here “WIP” (stands historically for WebInspector Protocol). Several versions of WIP implementation are deployed simultaneously in modules called Wip Backends. Each of them corresponds to a particular version of WebKit Remote Debugging Protocol (see details in [release notes](Release_0_3_0.md)). As of 0.3.0 it is user's responsibility to choose the correct backend.

### Javadocs ###
Generated documentation for the last release is available
[here](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/index.html).

## Getting started ##
  1. **Eclipse framework.** Install "ChromeDevTools SDK" and "ChromeDevTools SDK WIP Backends" features or manually install all org.chromium.sdk`*` packages.
  1. **Non-Eclipse framework.** Get all .jar files of SDK. Put them in your classpath. Get WIP backend jars and put **one** of them in your classpath. (You cannot put all backend jars in classpath, because they have conflicting class names. You can workaround it with several classloaders though.)

## Attaching to JavaScript VM ##
Attachment is the part where SDK API is protocol-dependent.

#### 1. DevTools Protocol. ####
To connect by '[DevTools](ChromeDevToolsProtocol.md)' protocol, try the following code:
```
Browser browser = BrowserFactory.getInstance().create(new InetSocketAddress(“localhost”, 9222), null);
TabFetcher tabFetcher = browser.createTabFetcher();
List<? extends TabConnector> tabList = tabFetcher.getTabs();
BrowserTab tab = tabList.get(<choose your tab>).attach(listener);  // Choose your tab somehow.
JavascriptVm javascriptVm = tab;
tabFetcher.dismiss(); // TabFecther is needed to accurately manage single shared connection to Browser.
```

#### 2. WebKit Remote Debugging Protocol ####
To connect by [WebKit Remote Debugging Protocol](WIP.md) use WIP [package](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/wip/package-summary.html):
```
WipBrowser browser = WipBrowserFactory.INSTANCE.createBrowser(
    new InetSocketAddress(“localhost”, 9222), null);
WipBackend backend;
#ifdef ECLIPSE
backend = BackendRegistry.INSTANCE.getBackends().get(<choose your backend>); 
#else
WipBackendFactory backendFactory = new WipBackendFactory(); // Same class name in each backend .jar 
backend = backendFactory.create();
#endif
WipBrowserTab tab = browser.getTabs(backend).get(<choose your tab>).attach(listener);
JavascriptVm javascriptVm = tab.getJavascriptVm()
```

#### 3. Standalone JavaScript VM ####
Standalone connection uses native [V8 protocol](http://code.google.com/p/v8/wiki/DebuggerProtocol) that is implemented by DebuggerAgent from V8 library.
```
StandaloneVm standaloneVm =
    BrowserFactory.getInstance().createStandalone(new InetSocketAddress(“localhost”, 9222), null);
standaloneVm.attach(listener);
JavascriptVm javascriptVm =  standaloneVm;
```


## First steps ##
The API is pretty much straightforward. Once you got [JavascriptVM](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/JavascriptVm.html) object, you can make various requests. For example you can try to set a breakpoint:
```
javascriptVm.setBreakpoint(new Breakpoint.Target.ScriptName(“index.html”),
    5, // line
    1, // column
    true, null, null, null); // other parameters.
```
Hopefully some time later the VM will pause on this breakpoint. Your program should learn about it from the DebugEventListener.

## DebugEventListener ##
What is called simply 'listener' in the previous code snippet is a central part of SDK API: [DebugEventListener](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/DebugEventListener.html). That's how your program learns about new events in JavaScript VM. Once you attached, SDK will call your listener on each event in asynchronous manner.

This asynchronism makes it difficult to simply play around with SDK, because your program becomes essentially multi-threaded. However, for the scripting purposes you can safely ignore most of the events except for 'suspended':
```
class SimpleListener implements DebugEventListener {
  DebugContext savedDebugContext;
  final Semaphore semaphore = new Semaphore(0); 
  @Override public void suspended(DebugContext debugContext) {
    savedDebugContext = debugContext;
    semaphore.release();
  }
  @Override public void scriptLoaded(Script script) {
    // ignore
  }
  ...
}
SimpleListener listener = new SimpleListener();
```
in the main script:
```
// Let's patiently wait for our breakpoint to hit.
boolean res = listener.semaphore.tryAcquire(5, TimeUnit.HOURS);
if (!res) {
  throw new RuntimeException(“Bad luck”);
}

DebugContext debugContext = listener.savedDebugContext;

// Let's get stacktrace.
List<? extends CallFrame> stackTrace = debugContext.getCallFrames();

// Now let's iterate over local variables...
```

## SDK Thread Model ##
SDK is multi-threaded library. It means that you can call its methods from different threads (with some reasonable limitations of course).

Also it means that SDK itself will call you asynchronously from its own thread. Each JavascriptVm allocates a dedicated thread called Dispatch Thread. That's where you listener will be called from. The good news is that you can rely on the fact that all events are coming consequently from one thread. The bad news is that you should be careful and return from the callbacks promptly. In particular you cannot call certain blocking methods of API, because it's a certain deadlock (you cannot block the road and waiting for another car to come at the same time).

All callbacks are called from either the Dispatch Thread or from your own thread (typically in some corner cases).

## Blocking and non-blocking method. ##
There are methods in API that work completely locally. For example `JavascriptVm.getVersion()` will simply return a field. Other methods create requests, send them over wire to JavaScript VM and wait for the response that should (hopefully) come.
This latter kind of methods could be implemented in 2 styles. In SDK you will find example of both styles:

1. **Blocking (Synchronous).** The method conveniently returns result to caller. For example [JsObject.getProperties()](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/JsObject.html#getProperties()) works like this. What may be inconvenient is that you have  to wait for a result, which means that your thread is blocked for the time of entire operation. So several simultaneous operations you will need several threads. Plus you cannot call this methods from SDK callbacks and listeners, because you would stop the Dispatch Thread forever.

2. **Asynchronous (Non-blocking).** The method doesn't return result. Instead it starts work in some way (for example sends the corresponding request) and returns control to you. You have to provide a callback that will be notified some time in the future when result is there. This is more flexible comparing to Blocking approach, but is obviously less useful. E.g. see [JavascriptVm.listBreakpoints](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/JavascriptVm.html#listBreakpoints(org.chromium.sdk.JavascriptVm.ListBreakpointsCallback,%20org.chromium.sdk.SyncCallback)).

SDK combines both approaches optimized for the typical use cases: some methods are blocking, some asynchronous. Some methods come in 2 styles, like in [JsEvaluateContext](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/JsEvaluateContext.html).

Some methods are blocking, but they still prefer to pass a result to callback. The possible reasons for this are:
  1. callback can accept both error and result, while (in Java) method can return only 1 type;
  1. SDK lets callback examine the result, holding all possible changes for the time of method work.

## Recurring helper types: SyncCallback, RelayOk etc ##
There are certain patterns how methods are written in SDK API. All blocking methods throw [MethodIsBlockingException](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/util/MethodIsBlockingException.html). All asynchronous methods return [RelayOk](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/RelayOk.html) and accept [SyncCallback](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/SyncCallback.html) parameter. In the first approach these types could be safely ignored.

They are here to help writing more safe program. Java is a pretty safe language, however dealocks (and infinite loops) still remain an actual problem. There are 2 typical cases where you can get a deadlock with SDK: calling a blocking method from a callback and waiting for a callback that nobody's going to call.

### SyncCallback ###
All asynchronous methods accept 2 callbacks. One is "logical", second one is "secure". It resembles try/finally structure, but for callbacks. The simple but vital actions should go into SyncCallback. It is guaranteed to be called one time when asynchronous operation finished either normally or with a failure. SyncCallback suits well for semaphore releasing and for resource deallocations.
"Logical" callback is called first. It processes data, passes it into other callbacks and does other complex things. It's ok if it fails and strictly speaking SDK may also fail to call it (if something went wrong).

There is a default implementation of SyncCallback that allows other thread to wait until callback is invoked: [CallbackSemaphore](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/CallbackSemaphore.html).
See how you can call asynchronous method `setBreakpoint` and wait until it finishes.
```
class LogicCallback implements BreakpointCallback {
  Breakpoint result = null;
  @Override public void success(Breakpoint breakpoint) {
    result = breakpoint;
  }
  @Override public void failure(String errorMessage) {
    System.out.println(“Problem: “ + errorMessage);
  }
};
LogicCallback callback = new LogicCallback();
SyncCallback syncCallback = new CallbackSemaphore();
RelayOk relayOk = javascriptVm.setBreakpoint(new Breakpoint.Target.ScriptName(“index.html”),
    5, 1, true, “a > 5”, callback, syncCallback);
// Wait when for operation to finish.
syncCallback.acquireDefault(relayOk);
Breakpoint createdBreakpoint = callback.result;
```

### RelayOk ###
This a 'symbolic' type. It helps to track how asynchronous methods 'promises' to call SyncCallback.
Initially all asynchronous methods returned `void`. However the following incorrect code could easily be written with `void` return type:
```
if (paramList.isEmpty()) {
  return;  // Hey, someone is waiting on SyncCallback, you promised to call it sooner or later.
} 
sendRequest(new Request(paramList), syncCallback);
```

Also note in the snippet about SyncCallback how method acquireDefault doesn't let you forget to call asynchronous method first because of RelayOk symbolic value.

### MethodIsBlockingException ###
This symbolic exception annotates all blocking methods in SDK. Tracking blocking methods is important, because you mustn't call them from callbacks. By default Java doesn't pay an attention to this exception, because it is unchecked (i.e. `extends RuntimeException`). But if you manage to make it checked (edit sources or create a new class in IDE that override one from the jar file), the IDE and compiler would make you track this symbolic exception in your code. This way you would see where you actually call a blocking method even indirectly.

Hint. You don't have to annotate all your program. At any point where you want to stop tracking a call chain, simply put a symbolic try/catch (this is cheap):
```
try {
  prepareAllProperties();
} catch (MethodIsBlockingException e) {
  // It's ok to call blocking code from here, because ...
}  
```

## Extensions ##
Some features of SDK are optional. Depending on backend version or JavaScript VM version they may or may not be available.

As of 0.3.0 there are following API extensions:
  * ['regexp' breakpoint target type](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/BreakpointTypeExtension.ScriptRegExpSupport.html) (works everywhere except old V8 versions)
  * ['function' breakpoint target type](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/BreakpointTypeExtension.FunctionSupport.html) (doesn't work in WIP)
  * [breakpoint 'ignore count' property](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/IgnoreCountBreakpointExtension.html) (doesn't work in WIP)
  * [evaluate with 'target mapping' parameter](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/org/chromium/sdk/wip/EvaluateToMappingExtension.html) (WIP only)

All extensions are designed:
  * to be fully separate from the standard API;
  * as objects that are available as early as possible; for example IgnoreCountBreakpointExtension is available from JavascriptVM interface; this way you can prepare to 'ignore count' not being available (e.g. disable UI) before any breakpoint is actually created.
# Overview #
ChromeDevTools SDK is a library that enables developers to create ChromeDevToolsProtocol-aware applications (EclipseDebugger is an example of such an application.) The library exposes an API and its default implementation for interaction with the Google Chrome (Chromium) browser.

In addition SDK supports following protocols:
  * [V8 debugger protocol](http://code.google.com/p/v8/wiki/DebuggerProtocol) to attach to any V8-based applications,
  * [WebKit Remote Debugging Protocol](WIP.md) (starting from [0.3.0 release](Release_0_3_0.md)), subproject called 'WIP'.

## Documentation ##
  * [Javadocs](http://chromedevtools.googlecode.com/svn/trunk/plugins/org.chromium.sdk/javadocs/index.html) for the latest release.
  * SdkTutorial.


## WIP Backends ##
WebKit Remote Debugging Protocol support (WIP) is deployed in form of separate modules called backends. Each backend corresponds to a particular version of the protocol (see a table in [release notes](Release_0_3_0.md)). Note that all backends share the same class names, so you can put only one backend in the classpath at the same time. This is not a problem when using Eclipse plugin dynamic loading and you can do the same manipulations with the Java classloaders if needed.

# Binary #
## Standalone library ##
The library is available as a set of .jar files from Downloads section ("ChromeDevTools SDK library as tar archive"). WIP backend jars are put in a separate directory. Note that backend jars cannot put simultaneously in one classpath.
## In Eclipse ##
From UI you can install "ChromeDevTools SDK" feature plus "ChromeDevTools SDK WIP Backends" feature. You also can manually get the following plugins:
  * org.chromium.sdk
  * org.chromium.sdk.wip.eclipse
  * org.chromium.sdk.wipbackend.`*` -- backend plugins

# Sources #
Sources could be found in project SVN under the path:
```
  trunk  /  plugins/org.chromium.sdk/src*
  trunk  /  plugins/org.chromium.sdk.wip.eclipse/src
  trunk  /  plugins/org.chromium.sdk.wipbackend.*/src*
```
(`trunk` can be replaced with `tags/...` for more stable code state.
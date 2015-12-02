# What is it? #
A 'launch' is Eclipse UI element that corresponds to a debug session (or a running program). When you attach to Chrome the launch object appears. Then you terminate it and it eventually disappears (gets removed).

You can find it in 'Debug' view in 'Debug' perspective:

![http://chromedevtools.googlecode.com/svn/wiki/rel022-launch.png](http://chromedevtools.googlecode.com/svn/wiki/rel022-launch.png)

On the picture above the launch is a top-level object labeled "chrome 9222 `[`Chromium JavaScript]". First part "chrome 9222" is a name of the corresponding launch configuration (see menu `Run | Debug Configurations...`).

A launch has nested elements (one is _target_, the other one is _thread_). In ChromeDevTools each launch corresponds to a VirtualProject.

There may be several launches simultaneously if you debug several things at once.

# Why? #
ChromeDevTools adds several menu elements to the launch context menu. It's the only place where you could:
  * enable "Break on JavaScript Exceptions",
  * invoke "Synchronize JavaScript Breakpoints".
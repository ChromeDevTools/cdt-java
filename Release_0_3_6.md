# Google Chrome Developer Tools for Java. Release 0.3.6. #

This release succeeds the [release 0.3.4](Release_0_3_4.md).

## Overview ##

This release brings some new debugger features. It also fixes issues [62](http://code.google.com/p/chromedevtools/issues/detail?id=62)
(AKA [65](http://code.google.com/p/chromedevtools/issues/detail?id=65)),
[64](http://code.google.com/p/chromedevtools/issues/detail?id=64)
and
[66](http://code.google.com/p/chromedevtools/issues/detail?id=66).

## Launch Configuration UI ##
A small UI change is made to the launch configuration dialog. The configuration gets a new tab “Script Mapping”, that adopts the “Source look-up method” control from the overcrowded “Remote” tab; the tab also has a new Source Wrapping control (see [below](#Source_Wrapping_Support.md)).

## Function Scope and Primitve Values ##
JavaScript language is known to support _closures_. A closure is a nested function that “sees” local variables of a parent function. Typically, a _reference_ to the closure will be kept and called later, after the parent function has finished. This means that the closure must keep those variables somehow. Together these variables are called a closure/function's _scope_.

Being able to inspect this scope may be valuable when you have a reference to a function and want to know what it does without actually calling it. Of course you can always navigate to the function's text, but the text itself might be not enough. For example, you function could be defined like in the following snippet:
```
var limit = a + b;
return function(x) {
    return x <= limit;
};
```
The behavior is trivial, but what the function actually does depends on the data: what the `limit` value is.

Now you can inspect this in Variables/Expressions view by expanding the function's properties and opening the pseudo-property <function scope>. That's where the value of `limit` is displayed together with all other _bound_ variables of the function. (Currently, this doesn't work for functions generated using the _Function.prototype.bind_ method).

_Note:_ this is supported in Chrome starting from WK118685 backend, and via Standalone V8 protocol starting since version 3.10.7.

Another small enhancement is a visible “primitive value” property. In JavaScript simple values (number, string, and boolean) can be wrapped as objects. For example  `Object(-1)`  or  `Object("crazy")`  returns no number or string. Those are objects that wrap original primitive value in an invisible internal property. With this release you can see the property explicitly in Variables/Expressions view.

_Note:_ this currently works only for Standalone V8 protocol since V8 3.10.5.

## Restart Frame/Drop To Frame ##

The distinctive behavior of live editing is that it will rewind a function, if the latter is halfway through by the moment its text is changed. Naturally, sometimes such a rewind can be useful by itself.  This is called 'restarting a frame'. Eclipse has a standard UI button to the right of Step In/Over/Out buttons:

![http://chromedevtools.googlecode.com/svn/wiki/rel036-step-icons.svg](http://chromedevtools.googlecode.com/svn/wiki/rel036-step-icons.svg)

In this release this button is finally wired up.

In Eclipse its name is “Drop to frame” which better reflects the fact that you can rewind not only the topmost frame, but also any frame down the stack. If the frame is not the topmost one, all frames above it are discarded.

Note that the 'rewind' does not reset the entire VM back to its previous state. It simply allows you to re-execute the same statements from the beginning. Whatever external changes have been done, their effects remain. For example, if your function increments some global variable, that action will not be undone.

_Note:_ this works in Chrome in backend WK120709 and Standalone V8 since 3.12.0.

## Source Wrapping Support ##
Some JavaScript frameworks employ a technique of wrapping user script source with some prefix and suffix. This way it puts your script in some additional framework-provided context. In such frameworks as Node.JS, you can easily notice it if you compare the script under a VirtualProject with your original file.

Unfortunately, this breaks [live editing](EclipseDebuggerFeatures#Live_Editing.md) feature, when edits are done to [your original (working) files](FeatureDebugOnRealFiles.md). Live edit will simply push a new version to the VM without reconstructing the prefix and suffix, effectively ruining the established structure.

This release makes an attempt to mitigate this problem. A small subsystem will recognize and support certain predefined prefixes and suffixes. Currently, only Node.JS framework is chosen for the purpose of an experiment and demonstration (however, an Eclipse extension point is ready for third parties).

Source wrapping support can be enabled from the launch configuration dialog.

![http://chromedevtools.googlecode.com/svn/wiki/rel036-debug-configurations-mapping.svg](http://chromedevtools.googlecode.com/svn/wiki/rel036-debug-configurations-mapping.svg)

To better see how it works for Node.JS scripts, you can perform a live editing push with a preview dialog (`context menu | V8 Debugging | Preview and Push Source Changes to VM...`)

## SDK ##
SDK API gets a few extensions backing the new features (see [javadoc for 0.3.6](http://chromedevtools.googlecode.com/svn/tags/chromedevtools-0.3.6/plugins/org.chromium.sdk/javadocs/index.html)).

## WIP Backends ##
As WebKit Remote Debugging Protocol evolves beyond [1.0 version](https://developers.google.com/chrome-developer-tools/docs/protocol/1.0/index), it gains new features that are supported by 2 new [backends](WipBackends.md) for Chrome 21 and early Chrome 22.. See table below.

#### Backend Set Version 0.1.8 ####
| id | Chrome version | New features |
|:---|:---------------|:-------------|
| Protocol 1.0 | 18.`*`.`*`.`*` |              |
| WK@118685 | 21.0.1180.`*`  | function scope, primitive value |
| WK@120709 |22.0.1188.`*`   | function scope, primitive value, restart frame |

## System Requirements ##
| **Eclipse configuration:**  |Eclipse Platform or any richer configuration (e.g. Eclipse Java IDE). |
|:----------------------------|:---------------------------------------------------------------------|
| **Eclipse version:**        |  Eclipse 3.5 minimum, Eclipse 3.6 and 4.2 is also tested.            |
| **Java:**                   | J2SE-1.6 or newer.                                                   |

Optional:
| **JSDT version:**<br>(feature <i>JavaScript Development Tools</i>) <table><thead><th> 1.2.0 or newer<br>(corresponds to Eclipse 3.6).</th></thead><tbody>
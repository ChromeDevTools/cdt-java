## What is it? ##

A "virtual project" is an Eclipse project created by the [Debugger](EclipseDebugger.md) for every debug session ([launch](LaunchElement.md)) and deleted once the debug session is over. Virtual projects can be found among other regular projects. Since [release 0.2.0](Release_0_2_0.md), the virtual project icon is decorated by a small [Chromium](http://www.chromium.org) logo. A virtual project serves as a container for JavaScript scripts downloaded from a remote VM. Those scripts are never saved on disk and are forgotten when the virtual project is deleted.

## Why do we need it? ##

For languages like C++ or Java, all source files reside in a developer's working directory. The JavaScript case is quite different: the entire source scripts collection (which includes scripts generated on the fly) can be found only inside a running JavaScript virtual machine (VM). That's why a JavaScript debugger has to download the scripts from a VM into Eclipse first. A virtual project is the way the Debugger presents them.

## Virtual project vs. Workspace local files ##

A virtual project is useful because it contains all up-to-date sources, so you don't have to care about getting them from elsewhere. It also contains sources that are generated on the fly, and is irreplaceable in this aspect.

Unfortunately, a virtual project is not very useful for a JavaScript developer who may want to _edit_ files, not only debug them. (Actually, scripts from virtual projects _are_ editable, which is used for the _live editing_ feature, but all changes are lost once the VM stops). In a more common setting, a developer will link a debug session to the workspace source files -- see [feature description](FeatureDebugOnRealFiles.md).

Note that a virtual project exists for any debug session, regardless of whether it has the workspace source files configured or not.
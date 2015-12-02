# Introduction #

This feature is important element of Debugger-IDE integration. By default Debugger works with source files it gets from remote VM -- a files under a temporary VirtualProject. (This means you never have to worry about getting sources for _debugging_).

However you also could be using Eclipse for _developing_ your JavaScript program. In this case your sources are in file system and it absolutely makes sense to have debugger use them rather than downloaded copies: you code, you set a breakpoint, you see a problem, you fix it -- all in the same editor. An important detail: you can set a breakpoint on a file before script has been loaded and even before you attached to Chrome.

Technically speaking, you will need to establish source mapping in both directions: [Debugger](EclipseDebugger.md) must know how a script in VM (already presented in the VirtualProject) is mapped to some local file and vice verse -- how a local file with breakpoint corresponds to a remote script. And it involves the directory structure both on remote and in local project.

Debugger has 2 approaches to deal with directory structures that are known as _source look-up methods_. You can select a method from "Remote" tab of launch configuration.

![https://chromedevtools.googlecode.com/svn/wiki/rel034-debug-configurations-remote.png](https://chromedevtools.googlecode.com/svn/wiki/rel034-debug-configurations-remote.png)

## Auto-detect method ##
Here the source look-up is based on file short names. This is a bit fuzzy because it may fail on files with the same name in different directories and because Debugger takes a liberty to pick a short name from whatever remote VM sends to it (could be any string) and makes some other assumptions.

Configuring:
  * select "Auto-detect" method in "Remote" tab of launch configuration,
  * in the "Source" tab, add one or more folders holding your working files.

If all of your files in the project are called 'foo.js', this probably won't work correctly. However, for most projects there shouldn't be any problems.

Anyway, should you occasionally have a few files with the same name in different directories, there is a fall-back: you can configure Debugger to take a directory name into consideration for this particular file.

1. Open file properties (Alt+Enter) on your local file.

![https://chromedevtools.googlecode.com/svn/wiki/rel030-script-properties.png](https://chromedevtools.googlecode.com/svn/wiki/rel030-script-properties.png)

2. Use "Less" and "More" buttons to add or remove file path components. By default, only the file name is used, but you may add its parent directory or a longer path. All selected file path components will be used for matching this file.

## Exact match method ##
This method accurately works with the full file path, so you would need to provide more details about your directory structure than in "Auto-detect" method.

First let’s see how V8 identifies those remote scripts. While debugging a script in the virtual project open “Properties” (context menu on file in “Project Explorer” or with Alt+Enter in an editor).

![http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-script-properties.png](http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-script-properties.png)

Here we see that VM identifies your script as URL ` http://server:8080/misc/utils.js ` (from this URL you seem to be running a local web server; there may be other configurations; in particular scripts may be from your disk, with `file://...` URLs). Depending on your working files layout, URLs like this should be mapped either as `http://server:8080/misc/** => <your misc folder>/**` or as `http://server:8080/** => <your project folder>/**`

You can configure this in "Edit Source Lookup..." on already running [launch](LaunchElement.md) or in “Source” tab of your debug launch configuration for future runs:

![http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-debug-configurations-source.png](http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-debug-configurations-source.png)

Add a new source container with identifier mapping and make sure it is above the “Default” container (here “Default” container represents the virtual project that holds downloaded scripts). In the dialog window, choose “JavaScript Source Name Mapper” from the list. This container converts URL (or any id) into a file name and looks it up in another container, here called “target container”.

![http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-source-name-mapper.png](http://chromedevtools.googlecode.com/svn/wiki/EclipseDebuggerFeatures-source-name-mapper.png)

Depending on your configuration, enter the common URL prefix (e.g. `http://server:8080/`) and specify the target container (choose type and configure it).

Also make sure that you selected "Exact match" method in "Remote" tab of launch configuration,

See also a [screen-cast video](http://www.youtube.com/watch?v=GVxFFw7lkYg).

# Known issues #
For files in VirtualProject the dedicated editor was registered. Obviously this is not the case for files in your project. You can be using JSDT JavaScript editor or system Text Editor or any other. This brings up the following issues.
  * The editor may not fully support breakpoint (like system Text Editor). You still can set them via `Run | Toggle Breakpoint`.
  * There might be several types of breakpoints registered in Eclipse. In this case you have to hint the system which type you want: choose `Chrome/V8 breakpoints` in {{{Run | Breakpoint Types}} menu.
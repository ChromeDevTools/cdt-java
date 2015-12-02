# How to Build Debugger From Sources #

You will need developer Eclipse (with JDT and PDE) and you will need target Eclipse (those 2 may be a single instance of Eclipse though).

The directory `/plugins` from `/trunk` or from branches contains several Eclipse projects. Check them out and import into Eclipse.

Eclipse should build all projects. Run or Debug them as "Eclipse Application".

# How to Build SDK alone #

SDK is a Java library. Its sources are hosted inside `org.chromium.sdk` pluging. The easies way to build them is to use Eclipse.

## Building in Eclipse ##
Check out plug-in project `org.chromium.sdk` from SVN. Let Eclipse build it (usually it happens automatically). Start external tool "build\_sdk\_jar" (should appear in `Run | External Tools` menu once you checked out the project).

A folder named `build-output` should appear under the project folder (you may need to manually refresh project folder to see it). Several .jar files should appear in the directory. These files comprise the SDK library.

## Building without Eclipse ##
Checkout `plugins/org.chromium.sdk` directory from the SVN (from trunk or a tagged version).

Build Java sources (using JDK 6 or later). Source directories:
  * src
  * src-dynamic-impl/bridge
  * src-dynamc-impl/parser
  * src-wip
  * src-wip-generated
libraries:
  * lib/json\_simple/json\_simple-1.1.jar

# Build with parser's static implementation #
In the project JSON messages get parsed by a dedicated parser. The parser has a domain-specific interfaces, that are normally implemented on the fly (using Java reflection). Alternatively a static (faster) implementation can be generated.

TBD.

# Build with [WIP](WIP.md) backends #
TBD.

# Building release #
Checkout sources from trunk or a branch -- should be a directory containing `plugins`, `features` and `builder` subdirectories.
```
cd builder
```
Update `build.properties` file (Eclipse directory, JDK directory). Output directory is `../../staging`

## Build main plugins ##
(Everything except WIP backends)
```
ant buildMain
```
Result is in `../../staging/mainResult`

## Build WIP backend plugins ##
`../../staging/mainResult` must contain main plugins.
```
ant buildBackends
```
Result is in `../../staging/backendsResult`

## Combining plugins ##
Input must be in `../../staging/mainResult` and `../../staging/backendsResult`.
```
ant repack
```
Result is in `../../staging/result/eclipse`

## Building archive files ##
```
ant buildLibs
```
Result is in `../../staging/result/libs`

## Building Javadocs ##
```
ant sdkJavadocs
```
Result is in `../../staging/result/sdkJavadocs`

# Release Checklist #
  * Validate MethodIsBlockingException consistency.
  * Bump up [version](Versioning.md) to even _"release"_ number.
  * Update license year numbers.
  * Build plugins and libraries.
  * Upload build to update repository.
  * Upload archives to Downloads.
  * Build and upload Javadocs (html and css needs `svn:mime-type` property).
  * Prepare SVN tag for release.
  * Write up Release notes.
  * Send a message to mail group.
  * Update first page.
  * Bump up [version](Versioning.md) to odd _"development"_ number.
  * Go over still open issues.
  * Revise feature descriptions on wiki.
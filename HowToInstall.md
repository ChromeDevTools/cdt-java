# Java Version #
Minimum required version of Java VM is 1.6. Note that Eclipse checks this and will almost _silently_ block Debugger plugins should the version appear wrong.

# Eclipse Debugger #

  * **Update site** is at http://chromedevtools.googlecode.com/svn/update/dev/. Note that Eclipse may take a chance updating other plugins at this occurrence. If this takes too long, it is recommended to temporary disable other update sites (use "Available Software Sites" preferences page).

  * **Update site in zip archive** is available on "Downloads" page.

Install "Chromium JavaScript Remote Debugger" and "ChromeDevTools SDK WIP Backends" features. If you have JSDT installed in your Eclipse, additional feature "Chromium JavaScript Debugger Bridge to JSDT" is also recommended.

# SDK library alone #

SDK is available from "Downloads" section as "ChromeDevTools SDK library". This is a tar archive of several jar-files. They have no dependencies on Eclipse.

WIP backends are separately downloadable as "ChromeDevTools SDK WIP backends".

# For Contributors #
You can check out sources and build the project yourself. See HowToBuild and [Contribution](Contribution.md) instructions.
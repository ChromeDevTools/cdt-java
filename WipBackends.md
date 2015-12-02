# Introduction #
WIP Backends is a design artifact of [WebKit Remote Debugging Protocol](WIP.md) client implementation. It allows to have several different protocol implementations behind the single SDK interface. Technically WIP backend is a sub-library that should be used together with the main SDK library. There are several of them.

This is how Java SDK tolerates the fact that different versions of Google Chrome/Chromium simultaneously available may have incompatible protocols.

WIP Backend is currently a user-visible detail. This means that it's a user responsibility to correctly select a proper WIP Backend before attaching from debugger. Each backend is presented in [Debugger](EclipseDebugger.md) UI in `Debug Configurations` dialog with a small declaration of compatible browser versions.

The exact set of backends change from release to release. That's why they are normally listed in release notes.

WIP Backends are deployed as a separate Eclipse feature "ChromeDevTools SDK WIP Backends".
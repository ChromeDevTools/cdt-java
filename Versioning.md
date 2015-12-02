# Background #

As the ChromeDevTools project evolves, it is necessary to consistently keep track of each component version.

Since the physical components are OSGi bundles developed using the Eclipse IDE, they will use the [following](http://wiki.eclipse.org/index.php/Version_Numbering) versioning scheme endorsed by the Eclipse developers.

# Component Versioning #

## ChromeDevToolsProtocol ##

The protocol version consists of two segments:
```
major.minor
```

  * The "major" segment will change when the protocol is broken (e.g. the message format or existing command specifications are changed.)
  * The "minor" segment will change in all other cases (e.g. new commands are added or unimplemented features are implemented.)

## OSGi bundles ##

According to the referenced scheme, an OSGi bundle version identifier format is as following:
```
major.minor.service.qualifier
```

A timestamp (YYYYMMDDHHmm) will be used as the "qualifier" segment for the plugins and features.

### Before-1.0 versioning scheme ###

Major version number "0" indicates that SDK/Debugger haven't become stable enough. This milestone will be marked as "1.0" version. Until this happens, the temporary ("0.`*`") version scheme is used for all plugins and features.

  * The "major" segment is 0.
  * The "minor" segment will change when the SDK API is broken or there are major user-facing improvements.
  * The "service" segment will change per release. The number is always even.
  * The "qualifier" segment is automatically generated during every build.

### Planned after-1.0 scheme ###

#### ChromeDevToolsSdk ####
  * The "major" segment will change when the SDK API is broken.
  * The "minor" segment will change when there are major improvements (not bugfixes) to the underlying implementation.
  * The "service" segment will change when there are bugfixes.
  * The "qualifier" segment is automatically generated during every build.

#### EclipseDebugger Plugins ####
  * The "major" segment will always be the same as that of the SDK version used.
  * The "minor" segment will change when there are major user-facing improvements.
  * The "service" segment will change when there are bugfixes.
  * The "qualifier" segment is automatically generated during every build.

#### EclipseDebugger and ChromeDevToolsSdk Features ####
The feature versioning is consistent with these [Feature Versioning Guidelines](http://wiki.eclipse.org/index.php/Version_Numbering#Versioning_features).
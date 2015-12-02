This is an article about [WIP](WIP.md) support development.

## Development process ##
As WebKit Remote Debugging Protocol keeps evolving and different versions of Chromium should be supported, client implementation is forked into several WipBackends.

See also DesignOverview.

### Changes from V8 native protocol ###
SDK is meant to provide a uniform interface to all existing or future protocols. However some small differences in behavior or features are possible. This is a preview of the list of WIP implementation differences.
  * JavaScript VM is not necessarily V8. Theoretically, other JavaScript engine could be used in debuggee. This may affect the feature-set. E.g. live editing might be disabled.
  * No "ref-id" property for values. Ref-id can be used to visually distinguish two objects with similar properties. This may be not available on WIP back-end.
  * Permanent values, that outlive DebugContext (i.e. a single paused state).
  * Additional protocol domains might be supported in the future, including access to DOM tree with CSS.
// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@106811

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Represents function call argument. Either remote object id <code>objectId</code> or primitive <code>value</code> or neither of (for undefined) them should be specified.
 */
public class CallArgumentParam extends org.json.simple.JSONObject {
  /**
   @param hasValue whether 'value' actually contains value (possibly null)
   @param value Primitive value.
   @param objectIdOpt Remote object handle.
   */
  public CallArgumentParam(boolean hasValue, Object value, String/*See org.chromium.sdk.internal.wip.protocol.common.runtime.RemoteObjectIdTypedef*/ objectIdOpt) {
    if (hasValue) {
      this.put("value", value);
    }
    if (objectIdOpt != null) {
      this.put("objectId", objectIdOpt);
    }
  }

}

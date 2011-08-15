// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@91754

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Represents function call argument. Either remote object id <code>objectId</code> or primitive <code>value</code> or neither of (for undefined) them should be specified.
 */
public class CallArgumentParam extends org.json.simple.JSONObject {
  /**
   @param valueOpt Primitive value.
   @param objectIdOpt Remote object handle.
   */
  public CallArgumentParam(Object valueOpt, org.chromium.sdk.internal.wip.protocol.output.runtime.RemoteObjectParam objectIdOpt) {
    if (valueOpt != null) {
      this.put("value", valueOpt);
    }
    if (objectIdOpt != null) {
      this.put("objectId", objectIdOpt);
    }
  }

}

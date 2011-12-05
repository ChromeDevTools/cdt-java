// Generated source.
// Generator: org.chromium.sdk.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@101756

package org.chromium.sdk.internal.wip.protocol.output.runtime;

/**
Mirror object referencing original JavaScript object.
 */
public class RemoteObjectParam extends org.json.simple.JSONObject {
  /**
   @param type Object type.
   @param subtypeOpt Object subtype hint. Specified for <code>object</code> type values only.
   @param classNameOpt Object class (constructor) name. Specified for <code>object</code> type values only.
   @param valueOpt Remote object value (in case of primitive values or JSON values if it was requested).
   @param descriptionOpt String representation of the object.
   @param objectIdOpt Unique object identifier (for non-primitive values).
   @param functionLocationOpt Function location within owning script.
   */
  public RemoteObjectParam(Type type, SubtypeOpt subtypeOpt, String classNameOpt, Object valueOpt, String descriptionOpt, String/*See org.chromium.sdk.internal.wip.protocol.output.runtime.RemoteObjectIdTypedef*/ objectIdOpt, org.chromium.sdk.internal.wip.protocol.output.debugger.LocationParam functionLocationOpt) {
    this.put("type", type);
    if (subtypeOpt != null) {
      this.put("subtype", subtypeOpt);
    }
    if (classNameOpt != null) {
      this.put("className", classNameOpt);
    }
    if (valueOpt != null) {
      this.put("value", valueOpt);
    }
    if (descriptionOpt != null) {
      this.put("description", descriptionOpt);
    }
    if (objectIdOpt != null) {
      this.put("objectId", objectIdOpt);
    }
    if (functionLocationOpt != null) {
      this.put("functionLocation", functionLocationOpt);
    }
  }

  /**
   Object type.
   */
  public enum Type implements org.json.simple.JSONAware{
    OBJECT("object"),
    FUNCTION("function"),
    UNDEFINED("undefined"),
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ;
    private final String protocolValue;

    Type(String protocolValue) {
      this.protocolValue = protocolValue;
    }

    @Override public String toJSONString() {
      return '"' + protocolValue + '"';
    }
  }
  /**
   Object subtype hint. Specified for <code>object</code> type values only.
   */
  public enum SubtypeOpt implements org.json.simple.JSONAware{
    ARRAY("array"),
    NULL("null"),
    NODE("node"),
    REGEXP("regexp"),
    DATE("date"),
    ;
    private final String protocolValue;

    SubtypeOpt(String protocolValue) {
      this.protocolValue = protocolValue;
    }

    @Override public String toJSONString() {
      return '"' + protocolValue + '"';
    }
  }
}

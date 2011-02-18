package org.chromium.sdk.internal.protocol;

import org.chromium.sdk.internal.protocol.data.ScriptWithId;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ScriptCollectedBody extends JsonSubtype<EventNotificationBody> {
  ScriptWithId script();
}

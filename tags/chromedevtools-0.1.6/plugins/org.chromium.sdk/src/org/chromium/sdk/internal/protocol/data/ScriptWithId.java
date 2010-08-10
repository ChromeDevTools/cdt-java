package org.chromium.sdk.internal.protocol.data;

import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ScriptWithId {
  long id();
}

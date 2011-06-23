// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocol;

import org.chromium.sdk.internal.protocol.liveedit.LiveEditResult;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonType;

@JsonType
public interface ChangeLiveBody extends JsonSubtype<CommandResponseBody>  {
  @JsonField(jsonLiteralName="change_log")
  Object getChangeLog();

  @JsonNullable
  @JsonField(jsonLiteralName="result")
  LiveEditResult getResultDescription();

  @JsonOptionalField
  Boolean stepin_recommended();
}

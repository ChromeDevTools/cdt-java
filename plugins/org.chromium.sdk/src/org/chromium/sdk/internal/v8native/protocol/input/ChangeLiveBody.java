// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.protocol.input;

import org.chromium.sdk.internal.liveeditprotocol.LiveEditResult;
import org.chromium.sdk.internal.protocolparser.EnumValueCondition;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionCustom;
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

  @JsonType
  interface CompileErrorDetails extends JsonSubtype<FailedCommandResponse.ErrorDetails> {
    @JsonOverrideField
    @JsonSubtypeConditionCustom(condition=TypeCondition.class)
    FailedCommandResponse.ErrorDetails.Type type();

    String syntaxErrorMessage();

    @JsonOptionalField
    PositionRange position();

    @JsonType
    interface PositionRange {
      Position start();
      Position end();
    }

    @JsonType
    interface Position {
      // Offset text character sequence.
      long position();

      long line();
      long column();
    }


    class TypeCondition extends EnumValueCondition<FailedCommandResponse.ErrorDetails.Type> {
      public TypeCondition() {
        super(FailedCommandResponse.ErrorDetails.Type.LIVEEDIT_COMPILE_ERROR);
      }
    }
  }
}

// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolModelParseException;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class FieldTypesTest {
  @Test
  public void testNullLongValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    DynamicParserImpl parser = new DynamicParserImpl(TypeWithNullableLong.class);

    {
      JSONObject json = parseJson("{'val': 2 }");
      TypeWithNullableLong val = parser.parse(json, TypeWithNullableLong.class);
      assertEquals(Long.valueOf(2), val.val());
    }

    {
      JSONObject json = parseJson("{'val': null }");
      TypeWithNullableLong val = parser.parse(json, TypeWithNullableLong.class);
      assertNull(val.val());
    }
  }

  @JsonType
  interface TypeWithNullableLong {
    Long val();
  }

  @Test
  public void testBrokenLongValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    DynamicParserImpl parser = new DynamicParserImpl(TypeWithLong.class);

    {
      JSONObject json = parseJson("{'val': 2 }");
      TypeWithLong val = parser.parse(json, TypeWithLong.class);
      assertEquals(2L, val.val());
    }

    {
      try {
        JSONObject json = parseJson("{'val': null }");
        TypeWithLong val = parser.parse(json, TypeWithLong.class);
        val.val();
        fail();
      } catch (Exception e) {
        // expected
      }
    }
  }

  @JsonType
  interface TypeWithLong {
    long val();
  }

  @Test
  public void testNullStructValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    DynamicParserImpl parser = new DynamicParserImpl(Something.class,
        TypeWithNullableSomething.class);

    {
      JSONObject json = parseJson("{'data': {} }");
      TypeWithNullableSomething val = parser.parse(json, TypeWithNullableSomething.class);
      assertNotNull(val.data());
    }

    {
      JSONObject json = parseJson("{'data': null }");
      TypeWithNullableSomething val = parser.parse(json, TypeWithNullableSomething.class);
      assertNull(val.data());
    }
  }

  @JsonType
  interface Something {
  }

  @JsonType
  interface TypeWithNullableSomething {
    @JsonNullable
    Something data();
  }

  @Test
  public void testBrokenNullStructValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    DynamicParserImpl parser = new DynamicParserImpl(Something.class, TypeWithSomething.class);

    {
      JSONObject json = parseJson("{'data': {} }");
      TypeWithSomething val = parser.parse(json, TypeWithSomething.class);
      assertNotNull(val.data());
    }

    {
      JSONObject json = parseJson("{'data': null }");
      try {
        parser.parse(json, TypeWithSomething.class);
        fail();
      } catch (Exception e) {
        // expected
      }
    }
  }

  @JsonType
  interface TypeWithSomething {
    Something data();
  }

  @Test
  public void testNonoptionalFields() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    DynamicParserImpl parser = new DynamicParserImpl(TypeWithNullableLong.class,
        TypeWithSomething.class, Something.class);

    // First couple of checks that parser does work
    {
      JSONObject json = parseJson("{'val': null}");
      TypeWithNullableLong val = parser.parse(json, TypeWithNullableLong.class);
      assertNull(val.val());
    }
    {
      JSONObject json = parseJson("{'data': {} }");
      TypeWithSomething val = parser.parse(json, TypeWithSomething.class);
      assertNotNull(val.data());
    }

    JSONObject emptyJson = parseJson("{}");

    {
      try {
        TypeWithNullableLong val = parser.parse(emptyJson, TypeWithNullableLong.class);
        val.val();
        fail();
      } catch (Exception e) {
        // expected
      }
    }
    {
      try {
        TypeWithSomething val = parser.parse(emptyJson, TypeWithSomething.class);
        fail();
      } catch (Exception e) {
        // expected
      }
    }
  }

  @Test
  public void testOptionalFields() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    DynamicParserImpl parser = new DynamicParserImpl(TypeWithOptionalLong.class,
        TypeWithOptionalSomething.class, Something.class);

    JSONObject emptyJson = parseJson("{}");

    {
      TypeWithOptionalLong val = parser.parse(emptyJson, TypeWithOptionalLong.class);
      Long l = val.val();
      assertNull(l);
    }
    {
      TypeWithOptionalSomething val = parser.parse(emptyJson, TypeWithOptionalSomething.class);
      Something something = val.data();
      assertNull(something);
    }
  }

  @JsonType
  interface TypeWithOptionalLong {
    @JsonOptionalField
    Long val();
  }

  @JsonType
  interface TypeWithOptionalSomething {
    @JsonOptionalField
    Something data();
  }

  private JSONObject parseJson(String semiJson) {
    String jsonString = semiJson.replace('\'', '"');
    JSONObject json;
    try {
      json = JsonUtil.jsonObjectFromJson(jsonString);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    return json;
  }
}

// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.protocolparser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.JsonNullable;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
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
    TypeWithNullableLongParser parser =
        createDynamicParser(TypeWithNullableLongParser.class, TypeWithNullableLong.class);

    {
      JSONObject json = parseJson("{'val': 2 }");
      TypeWithNullableLong val = parser.parseTypeWithNullableLong(json);
      assertEquals(Long.valueOf(2), val.val());
    }

    {
      JSONObject json = parseJson("{'val': null }");
      TypeWithNullableLong val = parser.parseTypeWithNullableLong(json);
      assertNull(val.val());
    }
  }

  @JsonType
  interface TypeWithNullableLong {
    Long val();
  }

  @JsonParserRoot
  interface TypeWithNullableLongParser {
    @JsonParseMethod
    TypeWithNullableLong parseTypeWithNullableLong(JSONObject json)
        throws JsonProtocolParseException;
  }

  @Test
  public void testBrokenLongValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    TypeWithLongParser parser = createDynamicParser(TypeWithLongParser.class, TypeWithLong.class);

    {
      JSONObject json = parseJson("{'val': 2 }");
      TypeWithLong val = parser.parseTypeWithLong(json);
      assertEquals(2L, val.val());
    }

    {
      try {
        JSONObject json = parseJson("{'val': null }");
        TypeWithLong val = parser.parseTypeWithLong(json);
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

  @JsonParserRoot
  interface TypeWithLongParser {
    @JsonParseMethod
    TypeWithLong parseTypeWithLong(JSONObject json) throws JsonProtocolParseException;
  }

  @Test
  public void testNullStructValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    TypeWithNullableSomethingParser parser = createDynamicParser(
        TypeWithNullableSomethingParser.class,
        Something.class,TypeWithNullableSomething.class);

    {
      JSONObject json = parseJson("{'data': {} }");
      TypeWithNullableSomething val = parser.parseTypeWithNullableSomething(json);
      assertNotNull(val.data());
    }

    {
      JSONObject json = parseJson("{'data': null }");
      TypeWithNullableSomething val = parser.parseTypeWithNullableSomething(json);
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

  @JsonParserRoot
  interface TypeWithNullableSomethingParser {
    @JsonParseMethod
    TypeWithNullableSomething parseTypeWithNullableSomething(JSONObject json)
        throws JsonProtocolParseException;
  }

  @Test
  public void testBrokenNullStructValue() throws JsonProtocolModelParseException,
      JsonProtocolParseException {
    TypeWithSomethingParser parser = createDynamicParser(TypeWithSomethingParser.class,
        Something.class, TypeWithSomething.class);

    {
      JSONObject json = parseJson("{'data': {} }");
      TypeWithSomething val = parser.parseTypeWithSomething(json);
      assertNotNull(val.data());
    }

    {
      JSONObject json = parseJson("{'data': null }");
      try {
        parser.parseTypeWithSomething(json);
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

  @JsonParserRoot
  interface TypeWithSomethingParser {
    @JsonParseMethod
    TypeWithSomething parseTypeWithSomething(JSONObject json) throws JsonProtocolParseException;
  }

  @Test
  public void testNonoptionalFields() throws JsonProtocolModelParseException,
      JsonProtocolParseException {

    SeveralTypesWithSomethingParser parser;
    {
      List<Class<?>> types = new ArrayList<Class<?>>(3);
      types.add(TypeWithNullableLong.class);
      types.add(TypeWithSomething.class);
      types.add(Something.class);
      parser = createDynamicParser(SeveralTypesWithSomethingParser.class, types);
    }

    // First couple of checks that parser does work
    {
      JSONObject json = parseJson("{'val': null}");
      TypeWithNullableLong val = parser.parseTypeWithNullableLong(json);
      assertNull(val.val());
    }
    {
      JSONObject json = parseJson("{'data': {} }");
      TypeWithSomething val = parser.parseTypeWithSomething(json);
      assertNotNull(val.data());
    }

    JSONObject emptyJson = parseJson("{}");

    {
      try {
        TypeWithNullableLong val = parser.parseTypeWithNullableLong(emptyJson);
        val.val();
        fail();
      } catch (Exception e) {
        // expected
      }
    }
    {
      try {
        TypeWithSomething val = parser.parseTypeWithSomething(emptyJson);
        fail();
      } catch (Exception e) {
        // expected
      }
    }
  }

  @JsonParserRoot
  interface SeveralTypesWithSomethingParser {
    @JsonParseMethod
    TypeWithSomething parseTypeWithSomething(JSONObject json) throws JsonProtocolParseException;

    @JsonParseMethod
    TypeWithNullableLong parseTypeWithNullableLong(JSONObject json)
        throws JsonProtocolParseException;
  }

  @Test
  public void testOptionalFields() throws JsonProtocolModelParseException,
      JsonProtocolParseException {

    SeveralTypesWithLongParser parser;
    {
      List<Class<?>> types = new ArrayList<Class<?>>(3);
      types.add(TypeWithOptionalLong.class);
      types.add(TypeWithOptionalSomething.class);
      types.add(Something.class);
      parser = createDynamicParser(SeveralTypesWithLongParser.class, types);
    }

    JSONObject emptyJson = parseJson("{}");

    {
      TypeWithOptionalLong val = parser.parseTypeWithOptionalLong(emptyJson);
      Long l = val.val();
      assertNull(l);
    }
    {
      TypeWithOptionalSomething val = parser.parseTypeWithOptionalSomething(emptyJson);
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

  @JsonParserRoot
  interface SeveralTypesWithLongParser {

    @JsonParseMethod
    TypeWithOptionalLong parseTypeWithOptionalLong(JSONObject emptyJson)
        throws JsonProtocolParseException;

    @JsonParseMethod
    TypeWithOptionalSomething parseTypeWithOptionalSomething(
        JSONObject emptyJson) throws JsonProtocolParseException;
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

  private static <ROOT> ROOT createDynamicParser(Class<ROOT> rootType, Class<?> oneType)
      throws JsonProtocolModelParseException {
    return createDynamicParser(rootType, Collections.<Class<?>>singletonList(oneType));
  }

  private static <ROOT> ROOT createDynamicParser(Class<ROOT> rootType,
      Class<?> firstType, Class<?> secondType) throws JsonProtocolModelParseException {
    List<Class<?>> list = new ArrayList<Class<?>>(2);
    list.add(firstType);
    list.add(secondType);
    return createDynamicParser(rootType, list);
  }

  private static <ROOT> ROOT createDynamicParser(Class<ROOT> rootType,
       List<Class<?>> protocolInterfaces) throws JsonProtocolModelParseException {
    DynamicParserImpl<ROOT> parser = new DynamicParserImpl<ROOT>(rootType, protocolInterfaces);
    return parser.getParserRoot();
  }
}

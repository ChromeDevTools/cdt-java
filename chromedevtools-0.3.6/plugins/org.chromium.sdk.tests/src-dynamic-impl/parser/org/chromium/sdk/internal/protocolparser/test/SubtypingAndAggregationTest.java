// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.EnumSet;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.EnumValueCondition;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonParseMethod;
import org.chromium.sdk.internal.protocolparser.JsonParserRoot;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionCustom;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class SubtypingAndAggregationTest {
  @Test
  public void testReadNamedColor() throws JsonProtocolParseException {
    String semiJson = "{'color': {'name': 'blue' }}";
    JSONObject json = createJsonObject(semiJson);
    TestParser parser = PARSER_INSTANCE.getParser();
    Ball ball = parser.parseBall(json);
    assertNotNull(ball);
    Color color = ball.color();
    assertNotNull(color);
    assertNull(color.asSchemedColor());
    NamedColor namedColor = color.asNamedColor();
    assertNotNull(namedColor);
    assertEquals("blue", namedColor.name());
  }

  @Test
  public void testReadRgb() throws JsonProtocolParseException {
    String semiJson = "{'color': {'scheme': 'rgb', 'red': 200, 'green': 50, 'blue': 5 }}";
    JSONObject json = createJsonObject(semiJson);
    TestParser parser = PARSER_INSTANCE.getParser();
    Ball ball = parser.parseBall(json);
    assertNotNull(ball);
    Color color = ball.color();
    assertNotNull(color);
    assertNull(color.asNamedColor());
    SchemedColor schemedColor = color.asSchemedColor();
    assertNotNull(schemedColor);
    assertNull(schemedColor.asCmykColor());
    RgbColor rgbColor = schemedColor.asRgbColor();
    assertNotNull(rgbColor);

    assertEquals(255L, rgbColor.red() + rgbColor.green() + rgbColor.blue());
  }

  @Test
  public void testParseAsSubtype() throws JsonProtocolParseException {
    String semiJson = "{'scheme': 'rgb', 'red': 200, 'green': 50, 'blue': 5 }";
    JSONObject json = createJsonObject(semiJson);
    TestParser parser = PARSER_INSTANCE.getParser();
    RgbColor rgbColor = parser.parseRgbColor(json);
    assertNotNull(rgbColor);
    assertEquals(255L, rgbColor.red() + rgbColor.green() + rgbColor.blue());
  }

  @Test
  public void testUnknownSubtype() {
    String semiJson = "{'fish': 'chips' }";
    JSONObject json = createJsonObject(semiJson);
    TestParser parser = PARSER_INSTANCE.getParser();
    try {
      parser.parseColor(json);
      fail("Exception expected");
    } catch (JsonProtocolParseException e) {
      // expected
    }
  }

  @Test
  public void testAmbiguousSubtype() {
    String semiJson = "{'scheme': 'rgb', 'red': 200, 'green': 50, 'blue': 5, 'name': 'black' }";
    JSONObject json = createJsonObject(semiJson);
    TestParser parser = PARSER_INSTANCE.getParser();
    try {
      parser.parseColor(json);
      fail("Exception expected");
    } catch (JsonProtocolParseException e) {
      // expected
    }
  }

  private static JSONObject createJsonObject(String semiJson) {
    String jsonString = semiJson.replace('\'', '"');
    try {
      return JsonUtil.jsonObjectFromJson(jsonString);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @JsonParserRoot
  public interface TestParser {

    @JsonParseMethod
    Ball parseBall(JSONObject json) throws JsonProtocolParseException;

    @JsonParseMethod
    Color parseColor(JSONObject json) throws JsonProtocolParseException;

    @JsonParseMethod
    RgbColor parseRgbColor(JSONObject json) throws JsonProtocolParseException;

  }

  @JsonType
  public interface Ball {
    Color color();
  }

  @JsonType
  public interface Color {
    @JsonSubtypeCasting SchemedColor asSchemedColor();
    @JsonSubtypeCasting NamedColor asNamedColor();
  }

  @JsonType
  public interface SchemedColor extends JsonSubtype<Color> {
    @JsonSubtypeCondition
    ColorScheme scheme();

    @JsonSubtypeCasting RgbColor asRgbColor();
    @JsonSubtypeCasting CmykColor asCmykColor();
  }

  public enum ColorScheme {
    RGB, CMYK
  }

  @JsonType
  public interface RgbColor extends JsonSubtype<SchemedColor> {
    @JsonOverrideField
    @JsonSubtypeConditionCustom(condition=RgbSchemeCondition.class)
    ColorScheme scheme();

    class RgbSchemeCondition extends EnumValueCondition<ColorScheme> {
      public RgbSchemeCondition() {
        super(EnumSet.<ColorScheme>of(ColorScheme.RGB));
      }
    }

    long red();
    long green();
    long blue();
  }

  @JsonType
  public interface CmykColor extends JsonSubtype<SchemedColor> {
    @JsonOverrideField
    @JsonSubtypeConditionCustom(condition=CmykSchemeCondition.class)
    ColorScheme scheme();

    class CmykSchemeCondition extends EnumValueCondition<ColorScheme> {
      public CmykSchemeCondition() {
        super(EnumSet.<ColorScheme>of(ColorScheme.CMYK));
      }
    }

    long cyan();
    long magenta();
    long yellow();
    long key();
  }

  @JsonType
  public interface NamedColor extends JsonSubtype<Color> {
    @JsonSubtypeCondition
    String name();
  }

  static final Class<?>[] ALL_JSON_INTERFACES = { Ball.class, Color.class, SchemedColor.class,
      RgbColor.class, CmykColor.class, NamedColor.class };

  private static final ParserHolder<TestParser> PARSER_INSTANCE =
      new ParserHolder<TestParser>(TestParser.class, ALL_JSON_INTERFACES);
}

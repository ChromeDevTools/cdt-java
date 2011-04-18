// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.protocolparser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;

import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.EnumValueCondition;
import org.chromium.sdk.internal.protocolparser.JsonOverrideField;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.protocolparser.JsonSubtype;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCasting;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeCondition;
import org.chromium.sdk.internal.protocolparser.JsonSubtypeConditionCustom;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.protocolparser.dynamicimpl.DynamicParserImpl;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class SubtypingAndAggregationTest {
  @Test
  public void testReadNamedColor() {
    String semiJson = "{'color': {'name': 'blue' }}";
    Ball ball = parse(semiJson, Ball.class);
    assertNotNull(ball);
    Color color = ball.color();
    assertNotNull(color);
    assertNull(color.asSchemedColor());
    NamedColor namedColor = color.asNamedColor();
    assertNotNull(namedColor);
    assertEquals("blue", namedColor.name());
  }

  @Test
  public void testReadRgb() {
    String semiJson = "{'color': {'scheme': 'rgb', 'red': 200, 'green': 50, 'blue': 5 }}";
    Ball ball = parse(semiJson, Ball.class);
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
  public void testParseAsSubtype() {
    String semiJson = "{'scheme': 'rgb', 'red': 200, 'green': 50, 'blue': 5 }";
    RgbColor rgbColor = parse(semiJson, RgbColor.class);
    assertNotNull(rgbColor);
    assertEquals(255L, rgbColor.red() + rgbColor.green() + rgbColor.blue());
  }

  @Test
  public void testUnknownSubtype() {
    String semiJson = "{'fish': 'chips' }";
    try {
      parse(semiJson, Color.class);
      assertTrue("Exception expected", false);
    } catch (RuntimeException e) {
      // expected
    }
  }

  @Test
  public void testAmbiguousSubtype() {
    String semiJson = "{'scheme': 'rgb', 'red': 200', 'green': 50, 'blue': 5, 'name': 'black' }";
    try {
      parse(semiJson, Color.class);
      assertTrue("Exception expected", false);
    } catch (RuntimeException e) {
      // expected
    }
  }


  private <T> T parse(String semiJson, Class<T> type) {
    String jsonString = semiJson.replace('\'', '"');
    JSONObject json;
    try {
      json = JsonUtil.jsonObjectFromJson(jsonString);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    DynamicParserImpl parser = PARSER_INSTANCE.getParser();
    try {
      return parser.parse(json, type);
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
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

  private static final ParserHolder PARSER_INSTANCE = new ParserHolder(ALL_JSON_INTERFACES);
}

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip.tools.protocolgenerator;

import java.util.List;

import org.chromium.sdk.internal.protocolparser.FieldLoadStrategy;
import org.chromium.sdk.internal.protocolparser.JsonField;
import org.chromium.sdk.internal.protocolparser.JsonOptionalField;
import org.chromium.sdk.internal.protocolparser.JsonType;
import org.chromium.sdk.internal.wip.tools.protocolgenerator.WipMetamodel.ArrayItemType;

/**
 * Defines schema of WIP metamodel defined in
 * "http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json".
 */
public interface WipMetamodel {
  @JsonType
  interface Root {
    @JsonOptionalField Object version();
    List<Domain> domains();
  }

  @JsonType
  interface Domain {
    String domain();

    @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
    @JsonOptionalField
    List<StandaloneType> types();

    @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
    List<Command> commands();

    @JsonField(loadStrategy=FieldLoadStrategy.LAZY)
    @JsonOptionalField
    List<Event> events();

    @JsonOptionalField String description();

    @JsonOptionalField Boolean hidden();
  }

  @JsonType
  interface Command {
    String name();
    @JsonOptionalField List<Parameter> parameters();
    @JsonOptionalField List<Parameter> returns();

    @JsonOptionalField String description();

    @JsonOptionalField Boolean hidden();
  }

  @JsonType
  interface Parameter {
    String name();

    @JsonOptionalField
    String type();

    @JsonOptionalField
    ArrayItemType items();

    @JsonField(jsonLiteralName="enum")
    @JsonOptionalField
    List<String> getEnum();

    // This is unparsable.
    @JsonOptionalField
    List<ObjectProperty> properties();

    @JsonOptionalField
    @JsonField(jsonLiteralName="$ref")
    String ref();

    @JsonOptionalField
    Boolean optional();

    @JsonOptionalField String description();

    @JsonOptionalField Boolean hidden();
  }

  @JsonType interface Event {
    String name();
    @JsonOptionalField List<Parameter> parameters();

    @JsonOptionalField String description();

    @JsonOptionalField Boolean hidden();
  }

  @JsonType interface StandaloneType {
    String id();

    @JsonOptionalField String description();

    String type();

    @JsonOptionalField Boolean hidden();

    @JsonOptionalField List<ObjectProperty> properties();

    @JsonField(jsonLiteralName="enum")
    @JsonOptionalField
    List<String> getEnum();

    @JsonOptionalField
    ArrayItemType items();
  }

  @JsonType interface ObjectProperty {
    String name();

    @JsonOptionalField
    String description();

    @JsonOptionalField
    Boolean optional();

    @JsonOptionalField
    String type();

    @JsonOptionalField
    ArrayItemType items();

    @JsonField(jsonLiteralName="$ref")
    @JsonOptionalField
    String ref();

    @JsonField(jsonLiteralName="enum")
    @JsonOptionalField
    List<String> getEnum();

    @JsonOptionalField Boolean hidden();
  }

  @JsonType interface ArrayItemType {
    @JsonOptionalField
    String description();

    @JsonOptionalField
    Boolean optional();

    @JsonOptionalField
    String type();

    @JsonOptionalField
    ArrayItemType items();

    @JsonField(jsonLiteralName="$ref")
    @JsonOptionalField
    String ref();

    @JsonField(jsonLiteralName="enum")
    @JsonOptionalField
    List<String> getEnum();

    @JsonOptionalField
    List<ObjectProperty> properties();
  }

  String STRING_TYPE = "string";
  String INTEGER_TYPE = "integer";
  String NUMBER_TYPE = "number";
  String BOOLEAN_TYPE = "boolean";
  String OBJECT_TYPE = "object";
  String ARRAY_TYPE = "array";
  String UNKNOWN_TYPE = "unknown";
  String ANY_TYPE = "any";
}

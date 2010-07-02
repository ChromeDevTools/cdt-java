// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.tools.v8;

/**
 * Events, command and response types and keys for the V8 Debugger
 * protocol messages.
 */
public enum V8Protocol implements CharSequence {

  KEY_SEQ("seq"),

  KEY_REQSEQ("request_seq"),

  KEY_TYPE("type"),

  TYPE_EVENT("event"),

  /**
   * VM state.
   */
  KEY_RUNNING("running"),

  TYPE_RESPONSE("response"),

  TYPE_REQUEST("request"),

  COMMAND_CONTINUE("continue"),

  COMMAND_EVALUATE("evaluate"),

  COMMAND_BACKTRACE("backtrace"),

  COMMAND_FRAME("frame"),

  COMMAND_SCRIPTS("scripts"),

  COMMAND_SOURCE("source"),

  COMMAND_SETBP("setbreakpoint"),

  KEY_COMMAND("command"),

  KEY_SUCCESS("success"),

  KEY_MESSAGE("message"),

  KEY_BODY("body"),

  KEY_V8_VERSION("V8Version"),

  FRAME_RECEIVER("receiver"),

  FRAME_FUNC("func"),

  BODY_INDEX("index"),

  BODY_LOCALS("locals"),

  BODY_SCOPES("scopes"),

  BODY_ARGUMENTS("arguments"),

  FRAME_SCRIPT("script"),

  ARGUMENT_NAME("name"),

  ARGUMENT_VALUE("value"),

  LOCAL_NAME("name"),

  LOCAL_VALUE("value"),

  FRAME_REFS("refs"),

  INFERRED_NAME("inferredName"),

  /**
   * Ref handle object. It contains name, value, type, handle (ref #).
   */
  REF_HANDLE("handle"),

  REF_TEXT("text"),

  REF_TYPE("type"),

  /**
   * A primitive type value.
   */
  REF_VALUE("value"),

  /**
   * A string-type value length.
   */
  REF_LENGTH("length"),

  /**
   * Object properties.
   */
  REF_PROPERTIES("properties"),

  REF_PROP_NAME("name"),

  REF_CONSTRUCTORFUNCTION("constructorFunction"),

  REF_PROTOOBJECT("protoObject"),

  REF_PROTOTYPEOBJECT("prototypeObject"),

  REF_CLASSNAME("className"),

  REF_PROP_TYPE("propertyType"),

  BODY_FRAMES("frames"),

  BODY_FRAME_TEXT("text"),

  BODY_FRAME_LINE("line"),

  BODY_FRAME_SRCLINE("sourceLineText"),

  BODY_SOURCELINE("sourceLine"),

  BODY_SOURCECOLUMN("sourceColumn"),

  BODY_FRAME_POSITION("position"),

  BREAK_BODY("body"),

  BREAK_BREAKPOINTS("breakpoints"),

  KEY_EVENT("event"),

  EVENT_BREAK("break"),

  EVENT_EXCEPTION("exception"),

  /**
   * Scripts and Source response.
   */
  SOURCE_CODE("source"),

  /**
   * Script name.
   */
  BODY_NAME("name"),

  BODY_LINEOFFSET("lineOffset"),

  BODY_LINECOUNT("lineCount"),

  BODY_SCRIPT_TYPE("scriptType"),

  BODY_SOURCE("body"),

  BODY_SETBP("body"),

  BODY_BREAKPOINT("breakpoint"),

  BODY_TYPE("type"),

  EVAL_BODY("body"),

  REF("ref"),

  ID("id"),

  DATA("data"),

  CONTEXT("context"),

  EXCEPTION("exception"),

  UNCAUGHT("uncaught"),

  ;

  public final String key;

  private V8Protocol(String key) {
    this.key = key;
  }

  public char charAt(int index) {
    return key.charAt(index);
  }

  public int length() {
    return key.length();
  }

  public CharSequence subSequence(int start, int end) {
    return key.subSequence(start, end);
  }

  @Override
  public String toString() {
    return key;
  }
}

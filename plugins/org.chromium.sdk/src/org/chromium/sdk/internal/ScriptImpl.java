// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * An objects that holds data for a "script" which is a part of a resource
 * loaded into the browser, identified by its original document URL, line offset
 * in the original document, and the line count this script spans.
 */
public class ScriptImpl implements Script {

  /**
   * An object containing data that uniquely identify a V8 script chunk.
   */
  public static class Descriptor {
    public final Type type;

    public final String name;

    public final int lineOffset;

    public final int lineCount;

    public final long id;

    public Descriptor(Type type, long id, String name, int lineOffset, int lineCount) {
      this.type = type;
      this.id = id;
      this.name = name;
      this.lineOffset = lineOffset;
      this.lineCount = lineCount;
    }

    @Override
    public int hashCode() {
      return
          name.hashCode() +
          (int) id * 0x101 +
          lineOffset * 0x1001 +
          lineCount * 0x10001;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Descriptor)) {
        return false;
      }
      Descriptor that = (Descriptor) obj;
      // The id equality is stronger than the name equality.
      return this.id == that.id &&
          this.lineOffset == that.lineOffset &&
          this.lineCount == that.lineCount;
    }

    public static Descriptor forResponse(JSONObject script, JSONArray refs) {
      script = V8ProtocolUtil.scriptWithName(script, refs);
      if (script == null) {
        return null;
      }
      String name = JsonUtil.getAsString(script, V8Protocol.BODY_NAME);
      if (name == null) {
        // We do not handle unnamed scripts.
        return null;
      }
      try {
        Long scriptType = JsonUtil.getAsLong(script, V8Protocol.BODY_SCRIPT_TYPE);
        Type type = V8ProtocolUtil.getScriptType(scriptType);
        if (type == null) {
          return null;
        }
        int lineOffset = JsonUtil.getAsLong(script, V8Protocol.BODY_LINEOFFSET).intValue();
        int lineCount = JsonUtil.getAsLong(script, V8Protocol.BODY_LINECOUNT).intValue();
        int id = V8ProtocolUtil.getScriptIdFromResponse(script).intValue();
        return new Descriptor(type, id, name, lineOffset, lineCount);
      } catch (Exception e) {
        // not a script object has been passed in
        return null;
      }
    }
  }

  private final Descriptor descriptor;

  private String source;

  /**
   * @param descriptor of the script retrieved from a "scripts" response
   */
  public ScriptImpl(Descriptor descriptor) {
    this.descriptor = descriptor;
    this.source = null;
  }

  public Type getType() {
    return this.descriptor.type;
  }

  public String getName() {
    return descriptor.name;
  }

  public int getEndLine() {
    return descriptor.lineOffset + descriptor.lineCount;
  }

  public int getLineOffset() {
    return descriptor.lineOffset;
  }

  public int getLineCount() {
    return descriptor.lineCount;
  }

  public long getId() {
    return descriptor.id;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getSource() {
    return source;
  }

  public boolean hasSource() {
    return source != null;
  }

  @Override
  public int hashCode() {
    return
        descriptor.hashCode() * 0x101 +
        (hasSource() ? (source.hashCode() * 0x1001) : 0);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ScriptImpl)) {
      return false;
    }
    ScriptImpl that = (ScriptImpl) obj;
    return this.descriptor.equals(that.descriptor) && eq(this.source, that.source);
  }

  private static boolean eq(Object left, Object right) {
    return left == right || (left != null && left.equals(right));
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[Script (").append(hasSource()
        ? "has"
        : "no").append(" source): name=").append(getName()).append(", lineRange=[").append(
        getLineOffset()).append(';').append(getEndLine()).append("]]");
    return sb.toString();
  }

  public static Long getScriptId(HandleManager handleManager, long scriptRef) {
    JSONObject handle = handleManager.getHandle(scriptRef);
    if (handle == null) {
      return -1L; // not found
    }
    return JsonUtil.getAsLong(handle, V8Protocol.ID);
  }

}

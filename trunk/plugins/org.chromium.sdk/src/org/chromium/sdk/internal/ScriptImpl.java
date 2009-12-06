// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.List;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;

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

    public final int endLine;

    public final long id;

    public Descriptor(Type type, long id, String name, int lineOffset, int lineCount) {
      this.type = type;
      this.id = id;
      this.name = name;
      this.lineOffset = lineOffset;
      this.endLine = lineOffset + lineCount - 1;
    }

    @Override
    public int hashCode() {
      return
          name != null ? name.hashCode() : (int) id * 0x101 +
          lineOffset * 0x1001 +
          endLine * 0x10001;
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
          this.endLine == that.endLine;
    }

    public static Descriptor forResponse(ScriptHandle script, List<SomeHandle> refs,
        V8ContextFilter contextFilter) {
      script = V8ProtocolUtil.validScript(script, refs, contextFilter);
      if (script == null) {
        return null;
      }
      String name = script.name();
      try {
        Long scriptType = script.scriptType();
        Type type = V8ProtocolUtil.getScriptType(scriptType);
        if (type == null) {
          return null;
        }
        int lineOffset = (int) script.lineOffset();
        int lineCount = (int) script.lineCount();
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

  public int getStartLine() {
    return descriptor.lineOffset;
  }

  public int getEndLine() {
    return descriptor.endLine;
  }

  public long getId() {
    return descriptor.id;
  }

  public String getSource() {
    return source;
  }

  public boolean hasSource() {
    return source != null;
  }

  public void setSource(String source) {
    this.source = source;
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
        getStartLine()).append(';').append(getEndLine()).append("]]");
    return sb.toString();
  }

  public static Long getScriptId(HandleManager handleManager, long scriptRef) {
    SomeHandle handle = handleManager.getHandle(scriptRef);
    if (handle == null) {
      return -1L; // not found
    }
    ScriptHandle scriptHandle;
    try {
      scriptHandle = handle.asScriptHandle();
    } catch (JsonProtocolParseException e) {
      throw new RuntimeException(e);
    }
    return scriptHandle.id();
  }

}

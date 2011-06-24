// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.Script;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocol.liveedit.LiveEditResult;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;

/**
 * An objects that holds data for a "script" which is a part of a resource
 * loaded into the browser, identified by its original document URL, line offset
 * in the original document, and the line count this script spans.
 */
public abstract class ScriptBase implements Script {
  /**
   * An object containing data that uniquely identify a V8 script chunk.
   */
  public static class Descriptor {
    public final Type type;

    public final String name;

    public final int lineOffset;

    public final int columnOffset;

    public final int endLine;

    public final long id;

    public Descriptor(Type type, long id, String name, int lineOffset, int columnOffset,
        int lineCount) {
      this.type = type;
      this.id = id;
      this.name = name;
      this.lineOffset = lineOffset;
      this.columnOffset = columnOffset;
      this.endLine = lineOffset + lineCount - 1;
    }

    @Override
    public int hashCode() {
      return
          name != null ? name.hashCode() : (int) id * 0x101 +
          lineOffset * 0x1001 + columnOffset * 0x10001 +
          endLine * 0x100001;
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
          this.columnOffset == that.columnOffset &&
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
        int columnOffset = (int) script.columnOffset();
        int lineCount = (int) script.lineCount();
        int id = V8ProtocolUtil.getScriptIdFromResponse(script).intValue();
        return new Descriptor(type, id, name, lineOffset, columnOffset, lineCount);
      } catch (Exception e) {
        // not a script object has been passed in
        return null;
      }
    }
  }

  private final Descriptor descriptor;

  private volatile String source = null;

  private volatile boolean isCollected = false;

  /**
   * @param descriptor of the script retrieved from a "scripts" response
   */
  public ScriptBase(Descriptor descriptor) {
    this.descriptor = descriptor;
    this.source = null;
  }

  @Override
  public Type getType() {
    return this.descriptor.type;
  }

  @Override
  public String getName() {
    return descriptor.name;
  }

  @Override
  public int getStartLine() {
    return descriptor.lineOffset;
  }

  @Override
  public int getStartColumn() {
    return descriptor.columnOffset;
  }

  @Override
  public int getEndLine() {
    return descriptor.endLine;
  }

  @Override
  public long getId() {
    return descriptor.id;
  }

  @Override
  public boolean isCollected() {
    return isCollected;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public boolean hasSource() {
    return source != null;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setCollected() {
    isCollected = true;
  }

  protected static class UpdateResultParser {
    public static UpdatableScript.ChangeDescription wrapChangeDescription(
        final LiveEditResult previewDescription) {
      if (previewDescription == null) {
        return null;
      }
      return new UpdatableScript.ChangeDescription() {
        @Override public UpdatableScript.OldFunctionNode getChangeTree() {
          return OLD_WRAPPER.wrap(previewDescription.change_tree());
        }

        @Override public String getCreatedScriptName() {
          return previewDescription.created_script_name();
        }

        @Override public boolean isStackModified() {
          return previewDescription.stack_modified();
        }

        @Override public TextualDiff getTextualDiff() {
          final LiveEditResult.TextualDiff protocolTextualData = previewDescription.textual_diff();
          if (protocolTextualData == null) {
            return null;
          }
          return new TextualDiff() {
            @Override public List<Long> getChunks() {
              return protocolTextualData.chunks();
            }
          };
        }
      };
    }

    private static class OldFunctionNodeImpl implements UpdatableScript.OldFunctionNode {
      private final LiveEditResult.OldTreeNode treeNode;
      private final FunctionPositions positions;
      private final FunctionPositions newPositions;

      OldFunctionNodeImpl(LiveEditResult.OldTreeNode treeNode) {
        this.treeNode = treeNode;
        this.positions = wrapPositions(treeNode.positions());
        if (treeNode.new_positions() == null) {
          this.newPositions = null;
        } else {
          this.newPositions = wrapPositions(treeNode.new_positions());
        }
      }
      @Override public String getName() {
        return treeNode.name();
      }
      @Override public ChangeStatus getStatus() {
        return statusCodes.get(treeNode.status());
      }
      @Override public String getStatusExplanation() {
        return treeNode.status_explanation();
      }
      @Override public List<? extends OldFunctionNode> children() {
        return wrapList(treeNode.children(), OLD_WRAPPER);
      }
      @Override public List<? extends NewFunctionNode> newChildren() {
        return wrapList(treeNode.new_children(), NEW_WRAPPER);
      }
      @Override public FunctionPositions getPositions() {
        return positions;
      }
      @Override public FunctionPositions getNewPositions() {
        return newPositions;
      }
      @Override public OldFunctionNode asOldFunction() {
        return this;
      }
    }
    private static class NewFunctionNodeImpl implements UpdatableScript.NewFunctionNode {
      private final LiveEditResult.NewTreeNode treeNode;
      private final FunctionPositions positions;
      NewFunctionNodeImpl(LiveEditResult.NewTreeNode treeNode) {
        this.treeNode = treeNode;
        this.positions = wrapPositions(treeNode.positions());
      }
      @Override public String getName() {
        return treeNode.name();
      }
      @Override public FunctionPositions getPositions() {
        return positions;
      }
      @Override public List<? extends NewFunctionNode> children() {
        return wrapList(treeNode.children(), NEW_WRAPPER);
      }
      @Override public OldFunctionNode asOldFunction() {
        return null;
      }
    }

    private static final Wrapper<LiveEditResult.OldTreeNode, OldFunctionNode> OLD_WRAPPER =
        new Wrapper<LiveEditResult.OldTreeNode, OldFunctionNode>() {
          @Override
          OldFunctionNode wrap(LiveEditResult.OldTreeNode original) {
            return new OldFunctionNodeImpl(original);
          }
    };

    private static final Wrapper<LiveEditResult.NewTreeNode, NewFunctionNode> NEW_WRAPPER =
        new Wrapper<LiveEditResult.NewTreeNode, NewFunctionNode>() {
          @Override
          NewFunctionNode wrap(LiveEditResult.NewTreeNode original) {
            return new NewFunctionNodeImpl(original);
          }
    };

    private static UpdatableScript.FunctionPositions wrapPositions(
        final LiveEditResult.Positions rawPositions) {
      return new UpdatableScript.FunctionPositions() {
        @Override public long getStart() {
          return rawPositions.start_position();
        }
        @Override public long getEnd() {
          return rawPositions.end_position();
        }
      };
    }

    private static abstract class Wrapper<FROM, TO> {
      abstract TO wrap(FROM original);
    }

    private static <FROM, TO> List<TO> wrapList(final List<? extends FROM> originalList,
        final Wrapper<FROM, TO> wrapper) {
      return new AbstractList<TO>() {
        @Override public TO get(int index) {
          return wrapper.wrap(originalList.get(index));
        }
        @Override public int size() {
          return originalList.size();
        }
      };
    }

    private static final Map<String, ChangeStatus> statusCodes;
    static {
      statusCodes = new HashMap<String, ChangeStatus>(5);
      statusCodes.put("unchanged", ChangeStatus.UNCHANGED);
      statusCodes.put("source changed", ChangeStatus.NESTED_CHANGED);
      statusCodes.put("changed", ChangeStatus.CODE_PATCHED);
      statusCodes.put("damaged", ChangeStatus.DAMAGED);
    }
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
    if (!(obj instanceof ScriptBase)) {
      return false;
    }
    ScriptBase that = (ScriptBase) obj;
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
}

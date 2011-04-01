// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.AbstractList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.chromium.sdk.DebugContext;
import org.chromium.sdk.DebugEventListener;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.internal.protocol.ChangeLiveBody;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.data.LiveEditResult;
import org.chromium.sdk.internal.protocol.data.LiveEditResult.OldTreeNode;
import org.chromium.sdk.internal.protocol.data.ScriptHandle;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.V8CommandCallbackBase;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8Helper.ScriptLoadCallback;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
import org.chromium.sdk.internal.tools.v8.request.ChangeLiveMessage;

/**
 * An objects that holds data for a "script" which is a part of a resource
 * loaded into the browser, identified by its original document URL, line offset
 * in the original document, and the line count this script spans.
 */
public class ScriptImpl implements Script {

  /** The class logger. */
  private static final Logger LOGGER = Logger.getLogger(ScriptImpl.class.getName());

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

  private final DebugSession debugSession;

  /**
   * @param descriptor of the script retrieved from a "scripts" response
   */
  public ScriptImpl(Descriptor descriptor, DebugSession debugSession) {
    this.descriptor = descriptor;
    this.source = null;
    this.debugSession = debugSession;
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

  public int getStartColumn() {
    return descriptor.columnOffset;
  }

  public int getEndLine() {
    return descriptor.endLine;
  }

  public long getId() {
    return descriptor.id;
  }

  public boolean isCollected() {
    return isCollected;
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

  public void setCollected() {
    isCollected = true;
  }

  public void setSourceOnRemote(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    V8CommandProcessor.V8HandlerCallback v8Callback = createScriptUpdateCallback(callback, false);
    debugSession.sendMessageAsync(new ChangeLiveMessage(getId(), newSource, Boolean.FALSE),
        true, v8Callback, syncCallback);
  }

  public void previewSetSource(String newSource, UpdateCallback callback,
      SyncCallback syncCallback) {
    V8CommandProcessor.V8HandlerCallback v8Callback = createScriptUpdateCallback(callback, true);
    debugSession.sendMessageAsync(new ChangeLiveMessage(getId(), newSource, Boolean.TRUE),
        true, v8Callback, syncCallback);
  }

  private V8CommandProcessor.V8HandlerCallback createScriptUpdateCallback(
      final UpdateCallback callback, final boolean previewOnly) {
    return new V8CommandCallbackBase() {
      @Override
      public void success(SuccessCommandResponse successResponse) {
        ChangeLiveBody body;
        try {
          body = successResponse.body().asChangeLiveBody();
        } catch (JsonProtocolParseException e) {
          throw new RuntimeException(e);
        }

        LiveEditResult resultDescription = body.getResultDescription();
        if (!previewOnly) {
          ScriptLoadCallback scriptCallback = new ScriptLoadCallback() {
            public void failure(String message) {
              LOGGER.log(Level.SEVERE,
                  "Failed to reload script after LiveEdit script update; " + message);
            }
            public void success() {
              DebugEventListener listener = debugSession.getDebugEventListener();
              if (listener != null) {
                listener.scriptContentChanged(ScriptImpl.this);
              }
            }
          };
          V8Helper.reloadScriptAsync(debugSession, Collections.singletonList(getId()),
              scriptCallback, null);

          if (body.stepin_recommended() == Boolean.TRUE) {
            DebugContext debugContext = debugSession.getContextBuilder().getCurrentDebugContext();
            if (debugContext == null) {
              // We may have already issued 'continue' since the moment that change live command
              // was sent so the context was dropped. Ignore this case.
            } else {
              debugContext.continueVm(DebugContext.StepAction.IN, 0, null);
            }
          } else {
            if (resultDescription != null && resultDescription.stack_modified()) {
              debugSession.recreateCurrentContext();
            }
          }
        }

        if (callback != null) {
          callback.success(body.getChangeLog(),
              UpdateResultParser.wrapChangeDescription(resultDescription));
        }
      }

      @Override
      public void failure(String message) {
        callback.failure(message);
      }
    };
  }

  private static class UpdateResultParser {
    static UpdatableScript.ChangeDescription wrapChangeDescription(
        final LiveEditResult previewDescription) {
      if (previewDescription == null) {
        return null;
      }
      return new UpdatableScript.ChangeDescription() {
        public UpdatableScript.OldFunctionNode getChangeTree() {
          return OLD_WRAPPER.wrap(previewDescription.change_tree());
        }

        public String getCreatedScriptName() {
          return previewDescription.created_script_name();
        }

        public boolean isStackModified() {
          return previewDescription.stack_modified();
        }

        public TextualDiff getTextualDiff() {
          final LiveEditResult.TextualDiff protocolTextualData = previewDescription.textual_diff();
          if (protocolTextualData == null) {
            return null;
          }
          return new TextualDiff() {
            public List<Long> getChunks() {
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
      public String getName() {
        return treeNode.name();
      }
      public ChangeStatus getStatus() {
        return statusCodes.get(treeNode.status());
      }
      public String getStatusExplanation() {
        return treeNode.status_explanation();
      }
      public List<? extends OldFunctionNode> children() {
        return wrapList(treeNode.children(), OLD_WRAPPER);
      }
      public List<? extends NewFunctionNode> newChildren() {
        return wrapList(treeNode.new_children(), NEW_WRAPPER);
      }
      public FunctionPositions getPositions() {
        return positions;
      }
      public FunctionPositions getNewPositions() {
        return newPositions;
      }
      public OldFunctionNode asOldFunction() {
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
      public String getName() {
        return treeNode.name();
      }
      public FunctionPositions getPositions() {
        return positions;
      }
      public List<? extends NewFunctionNode> children() {
        return wrapList(treeNode.children(), NEW_WRAPPER);
      }
      public OldFunctionNode asOldFunction() {
        return null;
      }
    }

    private static final Wrapper<LiveEditResult.OldTreeNode, OldFunctionNode> OLD_WRAPPER =
        new Wrapper<LiveEditResult.OldTreeNode, OldFunctionNode>() {
          @Override
          OldFunctionNode wrap(OldTreeNode original) {
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
        public long getStart() {
          return rawPositions.start_position();
        }
        public long getEnd() {
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

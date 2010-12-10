// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.liveedit.LiveEditDiffViewer.FunctionNode;
import org.chromium.debug.ui.liveedit.LiveEditDiffViewer.Side;
import org.chromium.debug.ui.liveedit.LiveEditDiffViewer.SourcePosition;
import org.chromium.debug.ui.liveedit.LiveEditDiffViewer.SourceText;
import org.chromium.sdk.Script;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.NewFunctionNode;
import org.chromium.sdk.UpdatableScript.OldFunctionNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

/**
 * Parses LiveEdit push result and produces input for {@link LiveEditDiffViewer}.
 * It is also responsible for providing user-visible explanations about situation with each
 * function.
 */
class PushResultParser {
  static LiveEditDiffViewer.Input createViewerInput(
      UpdatableScript.ChangeDescription changeDescription, ScriptTargetMapping filePair,
      boolean previewMode) {
    Script script = filePair.getScriptHolder().getSingleScript();
    IFile file = filePair.getFile();
    String newSourceTemp;
    try {
      byte[] newSourceBytes = ChromiumDebugPluginUtil.readFileContents(file);
      newSourceTemp = new String(newSourceBytes, "UTF-8"); //$NON-NLS-1$
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      newSourceTemp = Messages.PushResultParser_FAILED_SEE_LOG;
      ChromiumDebugPlugin.log(e);
    } catch (CoreException e) {
      newSourceTemp = Messages.PushResultParser_FAILED_SEE_LOG;
      ChromiumDebugPlugin.log(e);
    }

    final String finelNewSource = newSourceTemp;
    final String oldSource = script.getSource();

    TreeBuilder builder = new TreeBuilder(previewMode);
    final FunctionNode rootFunction = builder.build(changeDescription);

    return new LiveEditDiffViewer.Input() {
      public SourceText getNewSource() {
        return new SourceText() {
          public String getText() {
            return finelNewSource;
          }
          public String getTitle() {
            return Messages.PushResultParser_LOCAL_FILE;
          }
        };
      }
      public SourceText getOldSource() {
        return new SourceText() {
          public String getText() {
            return oldSource;
          }
          public String getTitle() {
            return Messages.PushResultParser_SCRIPT_IN_VM;
          }
        };
      }
      public FunctionNode getRootFunction() {
        return rootFunction;
      }
    };
  }

  private static class TreeBuilder {
    private final StatusRenderer statusRenderer;
    private final boolean hideOldVersion;

    public TreeBuilder(boolean previewOnly) {
      if (previewOnly) {
        this.statusRenderer = PREVIEW_STATUS_RENDERER;
      } else {
        this.statusRenderer = RESULT_STATUS_RENDERER;
      }
      this.hideOldVersion = !previewOnly;
    }

    public FunctionNode build(UpdatableScript.ChangeDescription changeDescription) {
      return buildNode(changeDescription.getChangeTree());
    }

    private NodeImpl buildNode(UpdatableScript.OldFunctionNode oldFunction) {
      List<NodeImpl> childListFirst = new ArrayList<NodeImpl>();
      for (UpdatableScript.OldFunctionNode oldChild : oldFunction.children()) {
        NodeImpl nodeImpl = buildNode(oldChild);
        childListFirst.add(nodeImpl);
      }
      List<NodeImpl> childListSecond = new ArrayList<NodeImpl>();
      for (NewFunctionNode newChild : oldFunction.newChildren()) {
        NodeImpl nodeImpl = buildNode(newChild);
        childListSecond.add(nodeImpl);
      }
      // Merge lists by positions SIDE.NEW
      List<NodeImpl> childList = new ArrayList<NodeImpl>();
      {
        int pos1 = 0;
        int pos2 = 0;
        while (true) {
          if (pos1 == childListFirst.size()) {
            childList.addAll(childListSecond.subList(pos2, childListSecond.size()));
            break;
          }
          if (pos2 == childListSecond.size()) {
            childList.addAll(childListFirst.subList(pos1, childListFirst.size()));
            break;
          }
          SourcePosition firstChildSourcePos = childListFirst.get(pos1).getPosition(Side.NEW);
          if (firstChildSourcePos == null) {
            childList.add(childListFirst.get(pos1));
            pos1++;
          } else {
            if (firstChildSourcePos.getStart() <
                childListSecond.get(pos2).getPosition(Side.NEW).getStart()) {
              childList.add(childListFirst.get(pos1));
              pos1++;
            } else {
              childList.add(childListSecond.get(pos2));
              pos2++;
            }
          }
        }
      }
      return new NodeImpl(oldFunction, createPosition(oldFunction.getPositions()),
          createPosition(oldFunction.getNewPositions()), childList);
    }
    private NodeImpl buildNode(UpdatableScript.NewFunctionNode newFunction) {
      List<NodeImpl> childList = new ArrayList<NodeImpl>();
      for (UpdatableScript.NewFunctionNode newChild : newFunction.children()) {
        NodeImpl nodeImpl = buildNode(newChild);
        childList.add(nodeImpl);
      }
      return new NodeImpl(newFunction, null, createPosition(newFunction.getPositions()), childList);
    }

    private static SourcePosition createPosition(
        final UpdatableScript.FunctionPositions positions) {
      if (positions == null) {
        return null;
      }
      return new SourcePosition() {
        public int getStart() {
          return (int) positions.getStart();
        }
        public int getEnd() {
          return (int) positions.getEnd();
        }
      };
    }

    private class NodeImpl implements FunctionNode {
      private final UpdatableScript.FunctionNode<?> rawFunction;
      private final String name;
      private final SourcePosition oldPosition;
      private final SourcePosition newPosition;
      private final List<? extends FunctionNode> childList;
      private FunctionNode parent = null;

      private NodeImpl(UpdatableScript.FunctionNode<?> rawFunction, SourcePosition oldPosition,
          SourcePosition newPosition, List<? extends NodeImpl> childList) {
        this.rawFunction = rawFunction;
        this.name = rawFunction.getName();
        this.oldPosition = oldPosition;
        this.newPosition = newPosition;
        this.childList = childList;
        for (NodeImpl child : childList) {
          child.parent = this;
        }
      }
      public List<? extends FunctionNode> children() {
        return childList;
      }
      public String getName() {
        return name;
      }
      public String getStatus() {
        return statusRenderer.getStatus(rawFunction, this);
      }
      public FunctionNode getParent() {
        return parent;
      }
      public SourcePosition getPosition(Side side) {
        switch (side) {
          case OLD: {
            if (newPosition != null && hideOldVersion && rawFunction.asOldFunction() != null &&
                rawFunction.asOldFunction().getStatus() != UpdatableScript.ChangeStatus.DAMAGED) {
              return null;
            }
            return oldPosition;
          }
          case NEW: return newPosition;
          default: throw new RuntimeException();
        }
      }
      protected SourcePosition getOldPositionInternal() {
        return oldPosition;
      }
      protected SourcePosition getNewPositionInternal() {
        return newPosition;
      }
    }

    private static abstract class StatusRenderer {
      abstract String getStatus(UpdatableScript.FunctionNode<?> rawFunction, NodeImpl nodeImpl);
    }

    private static final StatusRenderer PREVIEW_STATUS_RENDERER = new StatusRenderer() {
      @Override
      String getStatus(UpdatableScript.FunctionNode<?> rawFunction, NodeImpl nodeImpl) {
        OldFunctionNode asOldFunction = rawFunction.asOldFunction();
        if (asOldFunction == null) {
          return Messages.PushResultParser_NEW_FUNCTION;
        } else {
          switch (rawFunction.asOldFunction().getStatus()) {
            case UNCHANGED: return ""; //$NON-NLS-1$
            case NESTED_CHANGED: return Messages.PushResultParser_PREVIEW_CHANGED;
            case CODE_PATCHED: return Messages.PushResultParser_PREVIEW_PATCHED;
            case DAMAGED: {
              String message;
              if (nodeImpl.getPosition(Side.NEW) == null) {
                message = Messages.PushResultParser_PREVIEW_DAMAGED;
              } else {
                message =
                    Messages.PushResultParser_PREVIEW_DAMAGED_2;
              }
              String explanation = rawFunction.asOldFunction().getStatusExplanation();
              if (explanation != null) {
                message = message + "\n[" + explanation + "]"; //$NON-NLS-1$ //$NON-NLS-2$
              }
              return message;
            }
            default: return Messages.PushResultParser_PREVIEW_UNKNOWN;
          }
        }
      }
    };

    private static final StatusRenderer RESULT_STATUS_RENDERER = new StatusRenderer() {
      @Override
      String getStatus(UpdatableScript.FunctionNode<?> rawFunction, NodeImpl nodeImpl) {
        OldFunctionNode asOldFunction = rawFunction.asOldFunction();
        if (asOldFunction == null) {
          return Messages.PushResultParser_RESULT_NEW_FUNCTION;
        } else {
          switch (asOldFunction.getStatus()) {
            case UNCHANGED: return "";
            case NESTED_CHANGED: return Messages.PushResultParser_RESULT_CHANGED;
            case CODE_PATCHED: return Messages.PushResultParser_RESULT_PATHCED;
            case DAMAGED: {
              String message;
              if (nodeImpl.getNewPositionInternal() == null) {
                message = Messages.PushResultParser_RESULT_DAMAGED;
              } else {
                message =
                  Messages.PushResultParser_RESULT_DAMAGED_2;
              }
              String explanation = asOldFunction.getStatusExplanation();
              if (explanation != null) {
                message = message + "\n[" + explanation + "]"; //$NON-NLS-1$ //$NON-NLS-2$
              }
              return message;
            }
            default: return Messages.PushResultParser_RESULT_UNKNOWN;
          }
        }
      }
    };
  }
}
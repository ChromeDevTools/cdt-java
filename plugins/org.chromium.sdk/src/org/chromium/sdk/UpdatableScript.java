// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.List;

/**
 * This interface is a part of {@link Script} interface. It extends {@link Script} in order
 * to support experimental feature and is under development.
 */
public interface UpdatableScript extends Script {
  /**
   * Demands that script text should be replaced with a new one if possible.
   * @param newSource new text of script
   */
  void setSourceOnRemote(String newSource, UpdateCallback callback, SyncCallback syncCallback);

  /**
   * Same as {@link #setSourceOnRemote}, but does not actually update a script, only provides
   * a description of the planned changes.
   */
  void previewSetSource(String newSource, UpdateCallback callback, SyncCallback syncCallback);

  interface UpdateCallback {
    /**
     * Script text has been successfully changed.
     * {@link LiveEditDebugEventListener#scriptContentChanged(UpdatableScript)} will
     * be called additionally. Besides, a current context may be dismissed and recreated after this
     * event. The order of all listed event notifications is not currently specified.
     */
    void success(Object report, ChangeDescription changeDescription);
    void failure(String message);
  }

  /**
   * An interface that explains what has happened/going to happen within script update action.
   */
  interface ChangeDescription {
    /**
     * @return the root of the function chagne tree
     */
    OldFunctionNode getChangeTree();

    /**
     * @return the description of the textual diff
     */
    TextualDiff getTextualDiff();

    /**
     * @return the name of 'old script' that has been created or null if there was no need in
     *   creating a script
     */
    String getCreatedScriptName();

    boolean isStackModified();
  }

  interface TextualDiff {
    /**
     * @return textual diff of the script in form of list of 3-element diff chunk parameters
     *   that are (old_start_pos, old_end_pos, new_end_pos)
     */
    List<Long> getChunks();
  }

  /**
   * A basic element of function change tree. Subtyped as OldFunctionNode and NewFunctionNode.
   */
  interface FunctionNode<T extends FunctionNode<T>> {
    String getName();
    FunctionPositions getPositions();
    List<? extends T> children();
    OldFunctionNode asOldFunction();
  }

  interface FunctionPositions {
    long getStart();
    long getEnd();
  }

  enum ChangeStatus {
    UNCHANGED,
    NESTED_CHANGED,
    CODE_PATCHED,
    DAMAGED
  }

  /**
   * Represents an old function in the changed script. If it has new positions, it is also
   * represented in a new version of the script.
   */
  interface OldFunctionNode extends FunctionNode<OldFunctionNode> {
    ChangeStatus getStatus();

    String getStatusExplanation();

    /** @return nullable */
    FunctionPositions getNewPositions();

    List<? extends NewFunctionNode> newChildren();
  }

  /**
   * Represents a new function in the changed script, that has no corresponding old function.
   */
  interface NewFunctionNode extends FunctionNode<NewFunctionNode> {
  }
}

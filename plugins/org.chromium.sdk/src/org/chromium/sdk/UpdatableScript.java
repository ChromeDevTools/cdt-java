// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk;

import java.util.List;

/**
 * This interface is a part of {@link Script} interface. It covers live editing support.
 */
public interface UpdatableScript {
  /**
   * Demands that script text should be replaced with a new one if possible. VM may get resumed
   * after this command (this is defined by {@link ChangeDescription#isStackModified()}). In this
   * case a standard 'suspended' notification is expected in a moment.
   * @param newSource new text of script
   * TODO: add an explicit output parameter about whether VM was resumed or not.
   */
  RelayOk setSourceOnRemote(String newSource, UpdateCallback callback, SyncCallback syncCallback);

  /**
   * Same as {@link #setSourceOnRemote}, but does not actually update a script, only provides
   * a description of the planned changes.
   * @param callback receives change plan description
   */
  RelayOk previewSetSource(String newSource, UpdateCallback callback, SyncCallback syncCallback);

  interface UpdateCallback {
    /**
     * Script text change has succeeded or was successfully pre-calculated (in preview mode).
     * {@link DebugEventListener#scriptContentChanged} will
     * be called additionally. Besides, a current context may be dismissed and recreated after this
     * event. The order of all listed event notifications is not currently specified.
     * @param report unspecified implementation-dependent report for debugging purposes;
     *        may be null
     * @param changeDescription describes live editing change that has been applied or is planned
     *        to be applied; may be null if backend or VM does not support
     */
    void success(Object report, ChangeDescription changeDescription);
    void failure(String message);
  }

  /**
   * An interface that explains what has happened/going to happen within script update action.
   */
  interface ChangeDescription {
    /**
     * @return the root of the function change tree
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
   * A basic element of function change tree.
   * Subtyped as {@link OldFunctionNode} and {@link NewFunctionNode}.
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
   * Represents a brand new function in the changed script, that has no corresponding old function.
   */
  interface NewFunctionNode extends FunctionNode<NewFunctionNode> {
  }
}

// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import static org.chromium.debug.ui.DialogUtils.createErrorOptional;
import static org.chromium.debug.ui.DialogUtils.createOptional;

import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.DialogUtils.Message;
import org.chromium.debug.ui.DialogUtils.MessagePriority;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.Scope;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.chromium.debug.ui.actions.PushChangesAction;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.ChangeDescription;
import org.eclipse.osgi.util.NLS;

/**
 * An asynchronous loader of LiveEdit update preview data. It deals with outer world in terms of
 * {@link Updater} sources/consumers.
 * It implements {@link ValueSource} interface that provides a loaded result, or null data
 * if the result is not loaded yet. Updater gets notified whenever result is delivered.
 * <p>The input parameter may be changed at any moment.
 * <p>The loader may be in active or passive state. Since the preview data this class loads
 * is optional and is not required for wizard's work, the loader should be kept passive until
 * user actually needs its result (turns the page).
 */
class PreviewLoader implements ValueSource<Optional<UpdatableScript.ChangeDescription>> {
  private final Updater updater;
  private final ValueSource<ScriptTargetMapping> inputParameterSource;
  private boolean active = false;
  private final Monitor dataMonitor = new Monitor();

  PreviewLoader(Updater updater, ValueSource<ScriptTargetMapping> inputParameterSource) {
    this.updater = updater;
    this.inputParameterSource = inputParameterSource;
  }

  void registerSelf(Scope scope) {
    updater.addSource(scope, this);
    updater.addConsumer(scope, parametersConsumer);
    updater.addDependency(parametersConsumer, inputParameterSource);
  }

  void setActive(boolean active) {
    this.active = active;
    if (active) {
      requestPreview();
    }
  }

  private void requestPreview() {
    final ScriptTargetMapping inputPair = inputParameterSource.getValue();
    boolean inputIsNew = dataMonitor.updateInputAndStarted(inputPair);
    if (!inputIsNew) {
      return;
    }

    // Report about our value becoming empty.
    updater.reportChanged(this);

    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      public void failure(String message) {
        Optional<UpdatableScript.ChangeDescription> error = createErrorOptional(
            new Message(NLS.bind(Messages.PreviewLoader_FAILED_TO_GET, message),
                MessagePriority.WARNING));
        done(error);
      }
      public void success(Object report, UpdatableScript.ChangeDescription changeDescription) {
        Optional<UpdatableScript.ChangeDescription> result;
        if (changeDescription == null) {
          result = EMPTY_DATA;
        } else {
          result = createOptional(changeDescription);
        }
        done(result);
      }
      private void done(Optional<UpdatableScript.ChangeDescription> result) {
        boolean resultTaken = dataMonitor.updateResult(result, inputPair);
        if (resultTaken) {
          updater.reportChanged(PreviewLoader.this);
          updater.updateAsync();
        }
      }
    };
    PushChangesAction.execute(inputPair, callback, null, true);
  }

  public Optional<UpdatableScript.ChangeDescription> getValue() {
    return dataMonitor.getValue();
  }

  // A consumer that receive the actual input parameter (ScriptTargetMapping).
  private final ValueConsumer parametersConsumer = new ValueConsumer() {
    public void update(Updater updater) {
      ScriptTargetMapping inputPair = inputParameterSource.getValue();
      boolean updated = dataMonitor.updateInput(inputPair);
      if (updated && active) {
        requestPreview();
      }
    }
  };

  // Makes sure that some fields are only accessed in synchronized fashion.
  private static class Monitor {
    private ScriptTargetMapping input = null;
    private boolean alreadyStarted = false;
    private Optional<UpdatableScript.ChangeDescription> result = NO_DATA;

    synchronized boolean updateInputAndStarted(ScriptTargetMapping inputFilePair) {
      if (inputFilePair == input) {
        if (alreadyStarted) {
          return false;
        } else {
          alreadyStarted = true;
          return true;
        }
      } else {
        input = inputFilePair;
        result = NO_DATA;
        alreadyStarted = true;
        return true;
      }
    }

    synchronized boolean updateInput(ScriptTargetMapping inputFilePair) {
      if (inputFilePair == input) {
        return false;
      } else {
        input = inputFilePair;
        result = NO_DATA;
        alreadyStarted = false;
        return true;
      }
    }

    synchronized Optional<UpdatableScript.ChangeDescription> getValue() {
      return result;
    }

    synchronized boolean updateResult(Optional<ChangeDescription> result,
        ScriptTargetMapping inputPair) {
      if (inputPair != input) {
        return false;
      }
      this.result = result;
      return true;
    }
  }

  private static final Optional<UpdatableScript.ChangeDescription> NO_DATA = createErrorOptional(
      new Message(Messages.PreviewLoader_WAITING_FOR_DIFF, MessagePriority.NONE));
  private static final Optional<UpdatableScript.ChangeDescription> EMPTY_DATA =
      createErrorOptional(new Message(Messages.PreviewLoader_FAILED_TO_LOAD, MessagePriority.NONE));
}

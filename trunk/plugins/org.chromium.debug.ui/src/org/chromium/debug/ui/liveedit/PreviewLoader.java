// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import static org.chromium.debug.ui.DialogUtils.createErrorOptional;
import static org.chromium.debug.ui.DialogUtils.createOptional;

import org.chromium.debug.core.model.PushChangesPlan;
import org.chromium.debug.ui.DialogUtils.Message;
import org.chromium.debug.ui.DialogUtils.MessagePriority;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.Scope;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.chromium.sdk.UpdatableScript;
import org.chromium.sdk.UpdatableScript.CompileErrorFailure;
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
class PreviewLoader implements ValueSource<Optional<PreviewLoader.Data>> {
  private final Updater updater;
  private final ValueSource<PushChangesPlan> inputParameterSource;
  private boolean active = false;
  private final Monitor dataMonitor = new Monitor();

  PreviewLoader(Updater updater,
      ValueSource<PushChangesPlan> inputParameterSource) {
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
    final PushChangesPlan plan = inputParameterSource.getValue();
    boolean inputIsNew = dataMonitor.updateInputAndStarted(plan);
    if (!inputIsNew) {
      return;
    }

    // Report about our value becoming empty.
    updater.reportChanged(this);

    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      public void failure(final String message, UpdatableScript.Failure failure) {
        Optional<Data> result = failure.accept(
            new UpdatableScript.Failure.Visitor<Optional<Data>>() {
          @Override
          public Optional<Data> visitUnspecified() {
            return createErrorOptional(
                new Message(NLS.bind(Messages.PreviewLoader_FAILED_TO_GET, message),
                    MessagePriority.WARNING));
          }

          @Override
          public Optional<Data> visitCompileError(final CompileErrorFailure compileError) {
            Data data = new Data() {
              @Override
              public <R> R accept(Visitor<R> visitor) {
                return visitor.visitCompileError(compileError);
              }
              @Override public PushChangesPlan getChangesPlan() {
                return plan;
              }
            };
            return createOptional(data);
          }
        });
        done(result);
      }
      public void success(boolean resumed, Object report,
          final UpdatableScript.ChangeDescription changeDescription) {
        Optional<Data> result;
        if (changeDescription == null) {
          result = EMPTY_DATA;
        } else {
          Data data = new Data() {
            @Override public PushChangesPlan getChangesPlan() {
              return plan;
            }
            @Override public <R> R accept(Visitor<R> visitor) {
              return visitor.visitSuccess(changeDescription);
            }
          };
          result = createOptional(data);
        }
        done(result);
      }
      private void done(Optional<Data> result) {
        boolean resultTaken = dataMonitor.updateResult(result, plan);
        if (resultTaken) {
          updater.reportChanged(PreviewLoader.this);
          updater.updateAsync();
        }
      }
    };

    plan.execute(true, callback, null);
  }

  public interface Data {
    interface Visitor<R> {
      R visitSuccess(UpdatableScript.ChangeDescription changeDescription);
      R visitCompileError(UpdatableScript.CompileErrorFailure compileError);
    }

    <R> R accept(Visitor<R> visitor);

    PushChangesPlan getChangesPlan();
  }

  public Optional<Data> getValue() {
    return dataMonitor.getValue();
  }

  // A consumer that receive the actual input parameter (ScriptTargetMapping).
  private final ValueConsumer parametersConsumer = new ValueConsumer() {
    public void update(Updater updater) {
      PushChangesPlan inputPlan = inputParameterSource.getValue();
      boolean updated = dataMonitor.updateInput(inputPlan);
      if (updated && active) {
        requestPreview();
      }
    }
  };

  // Makes sure that some fields are only accessed in synchronized fashion.
  private static class Monitor {
    private PushChangesPlan input = null;
    private boolean alreadyStarted = false;
    private Optional<Data> result = NO_DATA;

    synchronized boolean updateInputAndStarted(PushChangesPlan inputPlan) {
      if (inputPlan == input) {
        if (alreadyStarted) {
          return false;
        } else {
          alreadyStarted = true;
          return true;
        }
      } else {
        input = inputPlan;
        result = NO_DATA;
        alreadyStarted = true;
        return true;
      }
    }

    synchronized boolean updateInput(PushChangesPlan inputPlan) {
      if (inputPlan == input) {
        return false;
      } else {
        input = inputPlan;
        result = NO_DATA;
        alreadyStarted = false;
        return true;
      }
    }

    synchronized Optional<Data> getValue() {
      return result;
    }

    synchronized boolean updateResult(Optional<Data> result,
        PushChangesPlan inputPlan) {
      if (inputPlan != input) {
        return false;
      }
      this.result = result;
      return true;
    }
  }

  private static final Optional<Data> NO_DATA = createErrorOptional(
      new Message(Messages.PreviewLoader_WAITING_FOR_DIFF, MessagePriority.NONE));
  private static final Optional<Data> EMPTY_DATA =
      createErrorOptional(new Message(Messages.PreviewLoader_FAILED_TO_LOAD, MessagePriority.NONE));
}

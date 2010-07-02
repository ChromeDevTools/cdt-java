// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Shell;

/**
 * A small set of utility classes that help programming dialog window logic.
 */
public class DialogUtils {
  /*
   * Part 1. Update graph.
   *
   * All logical elements of dialog and dependencies between them are modeled as DAG.
   * The data flows from vertices that corresponds to various input fields through
   * some transformations to the terminal vertices that are in-dialog helper messages and OK button.
   *
   * A linear data flow (no forks, no merges) is suitable for data transformation and
   * is programmed manually. Forks and merges are hard to dispatch manually and are managed by
   * class Updater.
   *
   * Updater knows about "source" vertices that may have several outgoing edges and about
   * "consumer" vertices that may have several incoming edges. Based on source vertex changes
   * updater updates consumer vertices in topological order.
   */

  /**
   * Represents source vertex for Updater. Technically updater uses this interface only as a flag
   * interface, because the only methods it uses are {@link Object#equals}/Object{@link #hashCode}.
   */
  public interface ValueSource<T> {
    /**
     * Method is not used by updater, for convenience only.
     * @return current value of the vertex
     */
    T getValue();
  }

  /**
   * Represents consumer vertex for Updater. Each instance should be explicitly registered in
   * Updater.
   */
  public interface ValueConsumer {
    /**
     * Called by updater when some linked sources have changed and it's time this vertex
     * got updated. {@link Updater#reportChanged} may be called if some {@line ValueSource}s have
     * changed during this update (but this should not break topological order of the graph).
     */
    void update(Updater updater);
  }

  /**
   * Helps to conduct update for vertices in a value graph.
   * Technically Updater does not see a real graph, because it doesn't support vertices
   * that are simultaneously source and consumer. Programmer helps manage other edges manually by
   * calling {@link #reportChanged} method.
   */
  public static class Updater {
    private final LinkedHashMap<ValueConsumer, Boolean> needsUpdateMap =
        new LinkedHashMap<ValueConsumer, Boolean>();
    private final Map<ValueSource<?>, List<ValueConsumer>> reversedDependenciesMap =
        new HashMap<ValueSource<?>, List<ValueConsumer>>();
    private boolean alreadyUpdating = false;

    public void addConsumer(ValueConsumer value, ValueSource<?> ... dependencies) {
      addConsumer(value, Arrays.asList(dependencies));
    }

    /**
     * Registers a consumer vertex with all its dependencies.
     */
    public void addConsumer(ValueConsumer value, List<? extends ValueSource<?>> dependencies) {
      Boolean res = needsUpdateMap.put(value, Boolean.FALSE);
      if (res != null) {
        throw new IllegalArgumentException("Already added"); //$NON-NLS-1$
      }
      for (ValueSource<?> dep : dependencies) {
        List<ValueConsumer> reversedDeps = reversedDependenciesMap.get(dep);
        if (reversedDeps == null) {
          reversedDeps = new ArrayList<ValueConsumer>(2);
          reversedDependenciesMap.put(dep, reversedDeps);
        }
        reversedDeps.add(value);
      }
    }

    /**
     * Reports about sources that have been changed and plans future update of consumers. This
     * method may be called at any time (it is not thread-safe though).
     */
    public void reportChanged(ValueSource<?> source) {
      List<ValueConsumer> reversedDeps = reversedDependenciesMap.get(source);
      if (reversedDeps != null) {
        for (ValueConsumer consumer : reversedDeps) {
          needsUpdateMap.put(consumer, Boolean.TRUE);
        }
      }
    }

    /**
     * Performs update of all vertices that need it. If some sources are reported changed
     * during the run of this method, their consumers are also updated.
     */
    public void update() {
      if (alreadyUpdating) {
        return;
      }
      alreadyUpdating = true;
      try {
        updateImpl();
      } finally {
        alreadyUpdating = false;
      }
    }

    private void updateImpl() {
      boolean hasChanges = true;
      while (hasChanges) {
        hasChanges = false;
        for (Map.Entry<ValueConsumer, Boolean> en : needsUpdateMap.entrySet()) {
          if (en.getValue() == Boolean.TRUE) {
            en.setValue(Boolean.FALSE);
            ValueConsumer currentValue = en.getKey();
            currentValue.update(this);
          }
        }
      }
    }

    /**
     * Updates all consumer vertices in graph.
     */
    public void updateAll() {
      for (Map.Entry<?, Boolean> en : needsUpdateMap.entrySet()) {
        en.setValue(Boolean.TRUE);
      }
      update();
    }
  }

  /**
   * A basic implementation of object that is both consumer and source. Updater will treat
   * as 2 separate objects.
   */
  public static abstract class ValueProcessor<T> implements ValueConsumer, ValueSource<T> {
    private T currentValue = null;
    public T getValue() {
      return currentValue;
    }
    protected void setCurrentValue(T currentValue) {
      this.currentValue = currentValue;
    }
  }

  /*
   * Part 2. Optional data type etc
   *
   * Since dialog should deal with error user entry, the typical data type is either value or error.
   * This is implemented as Optional interface. Most of data transformations should work only
   * when all inputs are non-error and generate error in return otherwise. This is implemented in
   * ExpressionProcessor.
   */


  /**
   * A primitive approach to "optional" algebraic type. This type is T + Set<Message>.
   */
  public interface Optional<V> {
    V getNormal();
    boolean isNormal();
    Set<? extends Message> errorMessages();
  }

  public static <V> Optional<V> createOptional(final V value) {
    return new Optional<V>() {
      public Set<Message> errorMessages() {
        return Collections.emptySet();
      }
      public V getNormal() {
        return value;
      }
      public boolean isNormal() {
        return true;
      }
    };
  }

  public static <V> Optional<V> createErrorOptional(Message message) {
    return createErrorOptional(Collections.singleton(message));
  }

  public static <V> Optional<V> createErrorOptional(final Set<? extends Message> messages) {
    return new Optional<V>() {
      public Set<? extends Message> errorMessages() {
        return messages;
      }
      public V getNormal() {
        throw new UnsupportedOperationException();
      }
      public boolean isNormal() {
        return false;
      }
    };
  }

  /**
   * A user interface message for dialog window. It has text and priority that helps choosing
   * the most important message it there are many of them.
   */
  public static class Message {
    private final String text;
    private final MessagePriority priority;
    public Message(String text, MessagePriority priority) {
      this.text = text;
      this.priority = priority;
    }
    public String getText() {
      return text;
    }
    public MessagePriority getPriority() {
      return priority;
    }
  }

  /**
   * Priority of a user interface message.
   * Constants are listed from most important to least important.
   */
  public enum MessagePriority {
    BLOCKING_PROBLEM(IMessageProvider.ERROR),
    BLOCKING_INFO(IMessageProvider.NONE),
    WARNING(IMessageProvider.WARNING);

    private final int messageProviderType;
    private MessagePriority(int messageProviderType) {
      this.messageProviderType = messageProviderType;
    }
    public int getMessageProviderType() {
      return messageProviderType;
    }
  }

  /**
   * A base class for the source-consumer pair that accepts several values as a consumer,
   * performs a calculation over them and gives it away the result via source interface.
   * Some sources may be of Optional type. If some of sources have error value the corresponding
   * error value is returned automatically.
   * <p>
   * The implementation should override a single method {@link #calculateNormal}.
   */
  public static abstract class ExpressionProcessor<T> extends ValueProcessor<Optional<T>> {
    private final List<ValueSource<? extends Optional<?>>> optionalSources;
    public ExpressionProcessor(List<ValueSource<? extends Optional<?>>> optionalSources) {
      this.optionalSources = optionalSources;
    }

    protected abstract Optional<T> calculateNormal();

    private Optional<T> calculateNewValue() {
      Set<Message> errors = new LinkedHashSet<Message>(0);
      for (ValueSource<? extends Optional<?>> source : optionalSources) {
        if (!source.getValue().isNormal()) {
          errors.addAll(source.getValue().errorMessages());
        }
      }
      if (errors.isEmpty()) {
        return calculateNormal();
      } else {
        return createErrorOptional(errors);
      }
    }
    public void update(Updater updater) {
      Optional<T> result = calculateNewValue();
      Optional<T> oldValue = getValue();
      setCurrentValue(result);
      if (!result.equals(oldValue)) {
        updater.reportChanged(this);
      }
    }
  }

  /*
   * Part 3. Various utils.
   */

  /**
   * A general-purpose implementation of OK button vertex. It works as a consumer of
   * 1 result value and several warning sources. From its sources it decides whether
   * OK button should be enabled and also provides dialog messages (errors, warnings, infos).
   */
  public static class OkButtonControl implements ValueConsumer {
    private final ValueSource<? extends Optional<?>> resultSource;
    private final List<? extends ValueSource<String>> warningSources;
    private final DialogElements dialogElements;

    public OkButtonControl(ValueSource<? extends Optional<?>> resultSource,
        List<? extends ValueSource<String>> warningSources, DialogElements dialogElements) {
      this.resultSource = resultSource;
      this.warningSources = warningSources;
      this.dialogElements = dialogElements;
    }

    /**
     * Returns a list of dependencies for updater -- a convenience method.
     */
    public List<? extends ValueSource<?>> getDependencies() {
      ArrayList<ValueSource<?>> result = new ArrayList<ValueSource<?>>();
      result.add(resultSource);
      result.addAll(warningSources);
      return result;
    }

    public void update(Updater updater) {
      Optional<?> result = resultSource.getValue();
      List<Message> messages = new ArrayList<Message>();
      for (ValueSource<String> warningSource : warningSources) {
        if (warningSource.getValue() != null) {
          messages.add(new Message(warningSource.getValue(), MessagePriority.WARNING));
        }
      }
      boolean enabled;
      if (result.isNormal()) {
        enabled = true;
      } else {
        enabled = false;
        messages.addAll(result.errorMessages());
      }
      dialogElements.getOkButton().setEnabled(enabled);
      String errorMessage;
      int type;
      if (messages.isEmpty()) {
        errorMessage = null;
        type = IMessageProvider.NONE;
      } else {
        Message visibleMessage = Collections.max(messages, messageComparatorBySeverity);
        errorMessage = visibleMessage.getText();
        type = visibleMessage.getPriority().getMessageProviderType();
      }
      dialogElements.setMessage(errorMessage, type);
    }

    private static final Comparator<Message> messageComparatorBySeverity =
        new Comparator<Message>() {
      public int compare(Message o1, Message o2) {
        int ordinal1 = o1.getPriority().ordinal();
        int ordinal2 = o2.getPriority().ordinal();
        if (ordinal1 < ordinal2) {
          return +1;
        } else if (ordinal1 == ordinal2) {
          return 0;
        } else {
          return -1;
        }
      }
    };
  }

  /**
   * A basic interface to elements of the dialog window from dialog logic part. The user may extend
   * this interface with more elements.
   */
  public interface DialogElements {
    Shell getShell();
    Button getOkButton();
    void setMessage(String message, int type);
  }

  /**
   * A wrapper around Combo that provides logic-level data-oriented access to the control.
   * This is not a simply convenience wrapper, because Combo itself does not keep a real data,
   * but only its string representation.
   */
  public static abstract class ComboWrapper<E> {
    private final Combo combo;
    public ComboWrapper(Combo combo) {
      this.combo = combo;
    }
    public Combo getCombo() {
      return combo;
    }
    public void addSelectionListener(SelectionListener listener) {
      combo.addSelectionListener(listener);
    }
    public abstract E getSelected();
    public abstract void setSelected(E element);
  }
}

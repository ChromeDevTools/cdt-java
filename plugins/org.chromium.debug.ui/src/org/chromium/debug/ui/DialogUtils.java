// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
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
   * A basic interface for anything that can give a value.
   */
  public interface Gettable<RES> {
    RES getValue();
  }

  /**
   * Represents source vertex for Updater. Technically updater uses this interface only as a flag
   * interface, because the only methods it uses are {@link Object#equals}/Object{@link #hashCode}.
   */
  public interface ValueSource<T> extends Gettable<T> {
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
   * <p>
   * Updater supports conditional updating. At some place in graph there may be switcher that
   * introduces several scopes. The switcher has only one active scope at a time, that is
   * controlled by an expression provided. The scope may contain parts of the graph and only
   * the active scope has its vertices updated, update in other scopes is deferred. Vertices from
   * the scope are only available via vertex called merger from outside of this scope.
   * Scopes may be nested. Updater always has a root scope.
   */
  public static class Updater {
    private final LinkedHashMap<ValueConsumer, Boolean> needsUpdateMap =
        new LinkedHashMap<ValueConsumer, Boolean>();
    private final Map<ValueSource<?>, List<ValueConsumer>> reversedDependenciesMap =
        new HashMap<ValueSource<?>, List<ValueConsumer>>();

    private final Map<ValueConsumer, ScopeImpl> consumer2Scope =
        new HashMap<ValueConsumer, ScopeImpl>();
    private final Map<ValueSource<?>, ScopeImpl> source2Scope =
        new HashMap<ValueSource<?>, ScopeImpl>();

    private boolean alreadyUpdating = false;

    private volatile boolean asyncStopped = false;

    private final ScopeImpl rootScope = new ScopeImpl(this);

    public void addConsumer(ValueConsumer value, ValueSource<?> ... dependencies) {
      addConsumer(value, Arrays.asList(dependencies));
    }
    /**
     * Registers a consumer vertex with all its dependencies. The root scope is assumed.
     */
    public void addConsumer(ValueConsumer value, List<? extends ValueSource<?>> dependencies) {
      addConsumer(rootScope, value);
      for (ValueSource<?> dep : dependencies) {
        addDependency(value, dep);
      }
    }

    /**
     * Registers a consumer within a particular scope.
     */
    public void addConsumer(Scope scope, ValueConsumer consumer) {
      Boolean res = needsUpdateMap.put(consumer, Boolean.FALSE);
      if (res != null) {
        throw new IllegalArgumentException("Already added"); //$NON-NLS-1$
      }
      consumer2Scope.put(consumer, (ScopeImpl)scope);
    }

    /**
     * Registers a source within a particular scope.
     */
    public void addSource(Scope scope, ValueSource<?> source) {
      ScopeImpl scopeImpl = (ScopeImpl) scope;
      Object conflict = source2Scope.put(source, scopeImpl);
      if (conflict != null) {
        throw new IllegalArgumentException("Already added"); //$NON-NLS-1$
      }
    }

    /**
     * Adds a dependency of consumer on source. Source may be from the same scope or from the outer
     * scope.
     */
    public void addDependency(ValueConsumer consumer, ValueSource<?> source) {
      Scope consumerScope = consumer2Scope.get(consumer);
      if (consumerScope == null) {
        throw new IllegalArgumentException("Unregistered consumer"); //$NON-NLS-1$
      }

      checkSourceVisibleInScope(source, consumerScope);
      addDependencyNoCheck(consumer, source);
    }

    /**
     * Reports about sources that have been changed and plans future update of consumers. This
     * method may be called at any time.
     */
    public synchronized void reportChanged(ValueSource<?> source) {
      List<ValueConsumer> reversedDeps = reversedDependenciesMap.get(source);
      if (reversedDeps != null) {
        for (ValueConsumer consumer : reversedDeps) {
          addConsumerToUpdate(consumer);
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

    /**
     * Updates all consumer vertices in graph.
     */
    public void updateAll() {
      for (Map.Entry<?, Boolean> en : needsUpdateMap.entrySet()) {
        en.setValue(Boolean.TRUE);
      }
      update();
    }

    /**
     * Returns a root scope that updater always has.
     */
    public Scope rootScope() {
      return rootScope;
    }

    /**
     * Request deferred update. This method may be called from any thread.
     */
    public void updateAsync() {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          if (asyncStopped) {
            return;
          }
          update();
        }
      });
    }

    /**
     * Stops asynchronous updates -- an activity which is hard to stop directly.
     */
    public void stopAsync() {
      asyncStopped = true;
    }

    void addDependencyNoCheck(ValueConsumer value, ValueSource<?> source) {
      List<ValueConsumer> reversedDeps = reversedDependenciesMap.get(source);
      if (reversedDeps == null) {
        reversedDeps = new ArrayList<ValueConsumer>(2);
        reversedDependenciesMap.put(source, reversedDeps);
      }
      reversedDeps.add(value);
    }

    /**
     * Makes sure that source is from the scope or from its ancestor.
     */
    void checkSourceVisibleInScope(ValueSource<?> source, Scope scope) {
      Scope sourceScope = source2Scope.get(source);
      if (sourceScope == null) {
        throw new IllegalArgumentException("Unregistered source"); //$NON-NLS-1$
      }
      do {
        if (sourceScope.equals(scope)) {
          return;
        }
        scope = scope.getOuterScope();
      } while (scope != null);
      throw new RuntimeException("Source from a wrong scope"); //$NON-NLS-1$
    }

    void addConsumerToUpdate(ValueConsumer consumer) {
      needsUpdateMap.put(consumer, Boolean.TRUE);
    }

    private void updateImpl() {
      boolean hasChanges = true;
      while (hasChanges) {
        hasChanges = false;
        for (Map.Entry<ValueConsumer, Boolean> en : needsUpdateMap.entrySet()) {
          if (en.getValue() == Boolean.TRUE) {
            en.setValue(Boolean.FALSE);
            ValueConsumer currentValue = en.getKey();
            ScopeImpl scope = consumer2Scope.get(currentValue);
            if (scope.isActive()) {
              currentValue.update(this);
            } else {
              scope.addDelayedConsumer(currentValue);
            }
          }
        }
      }
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

  /**
   * A scope for update graph vertices. Scope may have inner switchers, that own other (nested)
   * scopes.
   * <p>Scope roughly corresponds to a group of UI controls that may become disabled,
   * and thus may not generate any data and need no inner updates.
   */
  public interface Scope {
    Scope getOuterScope();

    /**
     * Creates a switcher that is operated by optional expression.
     * @param <T> type of expression
     */
    <T> OptionalSwitcher<T> addOptionalSwitch(Gettable<? extends Optional<T>> expression);

    /**
     * Creates a switcher that is operated by non-optional expression.
     * @param <T> type of expression
     */
    <T> Switcher<T> addSwitch(Gettable<T> expression);
  }

  /**
   * A callback that lets UI to reflect that some scope became enabled/disabled.
   */
  public interface ScopeEnabler {
    void setEnabled(boolean enabled, boolean recursive);
  }

  /**
   * Base interface for 2 types of switchers. The switcher is a logical element that
   * enables/disables its scopes according to the value of its expression.
   * @param <T> type of expression
   */
  public interface SwitchBase<T> {
    Scope addScope(T tag, ScopeEnabler scopeEnabler);
    ValueConsumer getValueConsumer();
  }

  /**
   * A switcher that is operated by non-optional expression.
   */
  public interface Switcher<T> extends SwitchBase<T> {
    /**
     * Creates a merge element, that links to sources inside switcher's scopes and
     * exposes them outside a single {@link ValueSource} that has a value of a corresponding source
     * in an active scope.
     * @param sources the list of sources in the corresponding scopes; must be in the same order
     */
    <P> ValueSource<P> createMerge(ValueSource<? extends P> ... sources);
  }

  /**
   * A switcher that is operated by optional expression.
   */
  public interface OptionalSwitcher<T> extends SwitchBase<T> {
    /**
     * See javadoc for {@link Switcher#createMerge}; the difference of this method is that
     * all sources have optional type and the merge source itself of optional type. The switcher
     * expression may have error value, in this case the merger also returns this error value.
     */
    <P> ValueSource<? extends Optional<P>> createOptionalMerge(
        ValueSource<? extends Optional<P>> ... sources);
  }

  public static <T> ValueSource<T> createConstant(final T constnant, Updater updater) {
    ValueSource<T> source = new ValueSource<T>() {
      public T getValue() {
        return constnant;
      }
    };
    updater.addSource(updater.rootScope(), source);
    return source;
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
    WARNING(IMessageProvider.WARNING),
    NONE(IMessageProvider.NONE);

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
   * Some sources may be of Optional type. If any of sources has error value the corresponding
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

  /**
   * An interface similar to {@link Gettable}, but with a quite specific contract:
   * it may depend on some optional values, but its calculate method should only be called
   * when all of the values are normal (non-error). This way its implementations becomes simpler.
   * It's up to someone who calls calculate method to check that contract is held.
   */
  public interface NormalExpression<RES> {
    RES calculate();
  }

  /**
   * Creates a {@link ValueProcessor} that is backed by {@link NormalExpression}.
   * @param optionalSources list of inputs that are optional and thus have to be checked for
   *   {@link NormalExpression} contract.
   */
  public static <T> ValueProcessor<Optional<T>> createOptionalProcessor(
      final NormalExpression<T> expression,
      ValueSource<? extends Optional<?>> ... optionalSources) {
    final Gettable<Optional<T>> getter = handleErrors(expression, optionalSources);
    return createProcessor(getter);
  }

  /**
   * Implements the basic contract of {@link NormalExpression}. Wraps it as {@link Gettable} and
   * keeps all its optional sources that are checked before each calculation.
   */
  public static <RES> Gettable<Optional<RES>> handleErrors(final NormalExpression<RES> expression,
      final ValueSource<? extends Optional<?>> ... optionalSources) {
    NormalExpression<Optional<RES>> wrapper = new NormalExpression<Optional<RES>>() {
      public Optional<RES> calculate() {
        return createOptional(expression.calculate());
      }
    };
    return handleErrorsAddNew(wrapper, optionalSources);
  }

  /**
   * Implements the basic contract of {@link NormalExpression}. Wraps it as {@link Gettable} and
   * keeps all its optional sources that are checked before each calculation.
   * The expression may rely on all optionalSources being of normal values, but it is allowed to
   * return error value itself.
   */
  public static <RES> Gettable<Optional<RES>> handleErrorsAddNew(
      final NormalExpression<Optional<RES>> expression,
      final ValueSource<? extends Optional<?>> ... optionalSources) {
    return new Gettable<Optional<RES>>() {
      public Optional<RES> getValue() {
        boolean hasErrors = false;
        for (ValueSource<? extends Optional<?>> source : optionalSources) {
          if (!source.getValue().isNormal()) {
            hasErrors = true;
            break;
          }
        }

        if (hasErrors) {
          Set<Message> errors = new LinkedHashSet<Message>(0);
          for (ValueSource<? extends Optional<?>> source : optionalSources) {
            if (!source.getValue().isNormal()) {
              errors.addAll(source.getValue().errorMessages());
            }
          }
          return createErrorOptional(errors);
        } else {
          return expression.calculate();
        }
      }
    };
  }

  public static ValueSource<? extends Optional<?>>[] dependencies(
      ValueSource<? extends Optional<?>>... sources) {
    return sources;
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
      Message visibleMessage = chooseImportantMessage(messages);
      dialogElements.setMessage(visibleMessage.getText(),
          visibleMessage.getPriority().getMessageProviderType());
    }

  }

  public static final Message NULL_MESSAGE = new Message(null, MessagePriority.NONE);

  public static Message chooseImportantMessage(Collection<? extends Message> messages) {
    if (messages.isEmpty()) {
      return NULL_MESSAGE;
    }
    return Collections.max(messages, messageComparatorBySeverity);
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
  /**
   * A basic access to elements of the dialog window from dialog logic part. The user may extend
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

  public static <T> ValueProcessor<T> createProcessor(final Gettable<T> expression) {
    return new ValueProcessor<T>() {
      public void update(Updater updater) {
        T newValue = expression.getValue();
        T oldValue = getValue();
        boolean same = (newValue == null) ? oldValue == null : newValue.equals(oldValue);
        setCurrentValue(newValue);
        if (!same) {
          updater.reportChanged(this);
        }
      }
    };
  }


  /*
   * Part 4. Implementation stuff.
   */

  private static abstract class SwitcherBaseImpl<T> implements SwitchBase<T> {
    private final ScopeImpl outerScope;

    private final Map<T, ScopeImpl> innerScopes = new LinkedHashMap<T, ScopeImpl>(2);

    private ScopeImpl currentScope = null;

    private final ValueConsumer consumer = new ValueConsumer() {
      public void update(Updater updater) {
        updateScopes();
        updater.reportChanged(getSourceForMerge());
      }
    };

    ScopeImpl getScopeForTag(T tag) {
      return innerScopes.get(tag);
    }

    abstract void updateScopes();

    abstract ValueSource<?> getSourceForMerge();

    void setCurrentScope(ScopeImpl newScope) {
      if (newScope == currentScope) {
        return;
      }
      if (currentScope != null) {
        currentScope.setEnabled(false);
      }
      currentScope = newScope;
      if (currentScope != null) {
        currentScope.setEnabled(true);
      }
    }

    ScopeImpl getCurrentScope() {
      return currentScope;
    }

    SwitcherBaseImpl(ScopeImpl outerScope) {
      this.outerScope = outerScope;
    }

    public Scope addScope(T tag, ScopeEnabler scopeEnabler) {
      ScopeImpl scope = new ScopeImpl(this, scopeEnabler, outerScope.getUpdater());
      Object conflict = innerScopes.put(tag, scope);
      if (conflict != null) {
        throw new IllegalStateException();
      }
      return scope;
    }

    public ValueConsumer getValueConsumer() {
      return consumer;
    }

    ScopeImpl getOuterScope() {
      return outerScope;
    }

    <V extends ValueSource<?>> Map<T,V> sortSources(List<V> sources) {
      if (innerScopes.size() != sources.size()) {
        throw new IllegalArgumentException();
      }
      Map<T,V> result = new HashMap<T, V>();
      Updater updater = outerScope.getUpdater();
      int i = 0;
      for (Map.Entry<T, ScopeImpl> en : innerScopes.entrySet()) {
        V source = sources.get(i);
        updater.checkSourceVisibleInScope(source, en.getValue());
        result.put(en.getKey(), source);
        i++;
      }
      return result;
    }
  }

  private static class SwitcherImpl<T> extends SwitcherBaseImpl<T> implements Switcher<T> {
    private final Gettable<? extends T> expression;
    private final ValueSource<T> sourceForMerge = new ValueSource<T>() {
      public T getValue() {
        return expression.getValue();
      }
    };

    SwitcherImpl(ScopeImpl outerScope, Gettable<T> expression) {
      super(outerScope);
      this.expression = expression;
    }

    @Override
    ValueSource<?> getSourceForMerge() {
      return sourceForMerge;
    }

    @Override
    void updateScopes() {
      T tag = expression.getValue();
      ScopeImpl newScope = getScopeForTag(tag);
      setCurrentScope(newScope);
    }

    public <P> ValueSource<P> createMerge(ValueSource<? extends P> ... sources) {
      final Map<T, ValueSource<? extends P>> map = sortSources(Arrays.asList(sources));

      ValueProcessor<P> result = new ValueProcessor<P>() {
        public void update(Updater updater) {
          setCurrentValue(calculate());
          updater.reportChanged(this);
        }

        private P calculate() {
          T tag = sourceForMerge.getValue();
          ValueSource<? extends P> oneSource = map.get(tag);
          return oneSource.getValue();
        }
      };
      ScopeImpl outerScope = getOuterScope();
      Updater updater = outerScope.getUpdater();
      updater.addConsumer(outerScope, result);
      updater.addDependency(result, getSourceForMerge());
      for (ValueSource<?> source : sources) {
        updater.addDependencyNoCheck(result, source); // AddNoCheck
      }

      outerScope.getUpdater().addSource(outerScope, result);

      return result;
    }
  }

  private static class OptionalSwitcherImpl<T> extends SwitcherBaseImpl<T>
      implements OptionalSwitcher<T> {
    private final Gettable<? extends Optional<? extends T>> expression;

    private final ValueSource<Optional<? extends T>> sourceForMerge =
        new ValueSource<Optional<? extends T>>() {
          public Optional<? extends T> getValue() {
            return expression.getValue();
          }
        };

    OptionalSwitcherImpl(ScopeImpl outerScope,
        Gettable<? extends Optional<? extends T>> expression) {
      super(outerScope);
      this.expression = expression;
    }

    @Override
    ValueSource<?> getSourceForMerge() {
      return sourceForMerge;
    }

    @Override
    void updateScopes() {
      ScopeImpl newScope;
      Optional<? extends T> control = expression.getValue();
      if (control.isNormal()) {
        T tag = control.getNormal();
        newScope = getScopeForTag(tag);
      } else {
        newScope = null;
      }
      setCurrentScope(newScope);
    }

    public <P> ValueSource<? extends Optional<P>> createOptionalMerge(
        ValueSource<? extends Optional<P>>... sources) {

      final Map<T, ValueSource<? extends Optional<P>>> map = sortSources(Arrays.asList(sources));

      ValueProcessor<? extends Optional<P>> result = new ValueProcessor<Optional<P>>() {
        public void update(Updater updater) {
          setCurrentValue(calculate());
          updater.reportChanged(this);
        }

        private Optional<P> calculate() {
          Optional<? extends T> control = sourceForMerge.getValue();
          if (control.isNormal()) {
            ValueSource<? extends Optional<P>> oneSource = map.get(control.getNormal());
            return oneSource.getValue();
          } else {
            return createErrorOptional(control.errorMessages());
          }
        }
      };
      ScopeImpl outerScope = getOuterScope();
      Updater updater = outerScope.getUpdater();
      updater.addConsumer(outerScope, result);
      updater.addDependency(result, getSourceForMerge());
      for (ValueSource<?> source : sources) {
        updater.addDependencyNoCheck(result, source); // AddNoCheck
      }

      outerScope.getUpdater().addSource(outerScope, result);

      return result;
    }
  }

  private static class ScopeImpl implements Scope {
    private final Updater updater;

    private final SwitcherBaseImpl<?> switcher;

    private final ScopeEnabler scopeEnabler;

    private final Set<ValueConsumer> delayedConsumers = new HashSet<ValueConsumer>(0);

    ScopeImpl(Updater updater) {
      this(null, null, updater);
    }

    public boolean isActive() {
      if (switcher == null) {
        return true;
      }
      return switcher.getOuterScope().isActive() && switcher.getCurrentScope() == this;
    }

    public void addDelayedConsumer(ValueConsumer consumer) {
      delayedConsumers.add(consumer);
    }

    public void setEnabled(boolean enabled) {
      if (enabled) {
        for (ValueConsumer consumer : delayedConsumers) {
          updater.addConsumerToUpdate(consumer);
        }
        delayedConsumers.clear();
      }
      if (scopeEnabler != null) {
        scopeEnabler.setEnabled(enabled, false);
      }
    }

    Updater getUpdater() {
      return updater;
    }

    ScopeImpl(SwitcherBaseImpl<?> switcher, ScopeEnabler scopeEnabler, Updater updater) {
      this.switcher = switcher;
      this.scopeEnabler = scopeEnabler;
      this.updater = updater;
    }

    public <P> OptionalSwitcher<P> addOptionalSwitch(Gettable<? extends Optional<P>> expression) {
      OptionalSwitcherImpl<P> switcher = new OptionalSwitcherImpl<P>(this, expression);
      updater.addConsumer(this, switcher.getValueConsumer());
      updater.addSource(this, switcher.getSourceForMerge());
      updater.addDependency(switcher.getValueConsumer(), switcher.getSourceForMerge());
      return switcher;
    }

    public <P> Switcher<P> addSwitch(Gettable<P> expression) {
      SwitcherImpl<P> switcher = new SwitcherImpl<P>(this, expression);
      updater.addConsumer(this, switcher.getValueConsumer());
      updater.addSource(this, switcher.getSourceForMerge());
      updater.addDependency(switcher.getValueConsumer(), switcher.getSourceForMerge());
      return switcher;
    }

    public Scope getOuterScope() {
      if (switcher == null) {
        return null;
      }
      return switcher.getOuterScope();
    }
  }
}

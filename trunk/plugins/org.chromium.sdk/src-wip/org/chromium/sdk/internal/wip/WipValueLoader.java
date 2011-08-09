// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.wip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsEvaluateContext;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.PropertyNameBuilder;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.protocol.input.runtime.GetPropertiesData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.RemotePropertyValue;
import org.chromium.sdk.internal.wip.protocol.output.runtime.GetPropertiesParams;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFutureRef;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * Responsible for loading values of properties. It works in pair with {@link WipValueBuilder}.
 * TODO: add a cache for already loaded values if remote protocol ever has
 * permanent object ids (same object reported under the same id within a debug context).
 */
public abstract class WipValueLoader implements RemoteValueMapping {
  private final WipTabImpl tabImpl;
  private final AtomicInteger cacheStateRef = new AtomicInteger(1);
  private final WipValueBuilder valueBuilder = new WipValueBuilder(this);

  public WipValueLoader(WipTabImpl tabImpl) {
    this.tabImpl = tabImpl;
  }

  @Override
  public void clearCaches() {
    cacheStateRef.incrementAndGet();
  }

  WipValueBuilder getValueBuilder() {
    return valueBuilder;
  }

  WipTabImpl getTabImpl() {
    return tabImpl;
  }

  /**
   * Asynchronously loads object properties. It starts a load operation of a corresponding
   * {@link AsyncFuture}. The operation is fully synchronous, so this method actually
   * blocks. Meanwhile any other thread trying to access the same object properties won't block
   * here, but will wait on the {@link AsyncFuture} getters instead.
   * @param innerNameBuilder name builder for qualified names of all properties and subproperties
   * @param output future object that will hold result of load operation
   */
  void loadJsObjectPropertiesAsync(final String objectId,
      PropertyNameBuilder innerNameBuilder, boolean reload, int currentCacheState,
      AsyncFutureRef<Getter<ObjectProperties>> output) {
    ObjectPropertyProcessor propertyProcessor = new ObjectPropertyProcessor(innerNameBuilder);
    loadPropertiesAsync(objectId, propertyProcessor, reload, currentCacheState, output);
  }

  int getCacheState() {
    return cacheStateRef.get();
  }

  abstract String getObjectGroupId();

  /**
   * A utility method that initializes {@link AsyncFuture} of an object without properties.
   */
  static void setEmptyJsObjectProperties(AsyncFutureRef<Getter<ObjectProperties>> output) {
    output.initializeTrivial(EMPTY_OBJECT_PROPERTIES_GETTER);
  }

  /**
   * A getter that either returns a value or throws an exception with some failure description.
   * Exception is the only means of passing message about some problem to user in SDK API.
   */
  static abstract class Getter<T> {
    abstract T get();

    static <V> Getter<V> newNormal(final V value) {
      return new Getter<V>() {
        @Override
        V get() {
          return value;
        }
      };
    }

    static <S> Getter<S> newFailure(final Exception cause) {
      return new Getter<S>() {
        @Override
        S get() {
          throw new RuntimeException("Failed to load properties", cause);
        }
      };
    }
  }

  interface ObjectProperties {
    List<? extends JsVariable> properties();
    JsVariable getProperty(String name);

    List<? extends JsVariable> internalProperties();
    int getCacheState();
  }

  /**
   * An abstract processor that is reads protocol response and creates proper
   * property objects. This may be implemented differently for objects, functions or
   * scopes.
   */
  interface LoadPostprocessor<RES> {
    RES process(List<? extends RemotePropertyValue> propertyList, int currentCacheState);
    RES getEmptyResult();
    RES forException(Exception exception);
  }

  private class ObjectPropertyProcessor implements LoadPostprocessor<Getter<ObjectProperties>> {
    private final PropertyNameBuilder propertyNameBuilder;

    ObjectPropertyProcessor(PropertyNameBuilder propertyNameBuilder) {
      this.propertyNameBuilder = propertyNameBuilder;
    }

    @Override
    public Getter<ObjectProperties> process(
        List<? extends RemotePropertyValue> propertyList, final int currentCacheState) {
      final List<JsVariable> properties = new ArrayList<JsVariable>(propertyList.size());
      final List<JsVariable> internalProperties = new ArrayList<JsVariable>(2);

      for (RemotePropertyValue property : propertyList) {
        String name = property.name();
        boolean isInternal = INTERNAL_PROPERTY_NAME.contains(name);

        ValueNameBuilder valueNameBuilder =
            WipExpressionBuilder.createValueOfPropertyNameBuilder(name, propertyNameBuilder);

        JsVariable variable = valueBuilder.createVariable(property.value(), valueNameBuilder);
        if (isInternal) {
          internalProperties.add(variable);
        } else {
          properties.add(variable);
        }
      }

      final ObjectProperties result = new ObjectProperties() {
        private volatile Map<String, JsVariable> propertyMap = null;

        @Override
        public List<? extends JsVariable> properties() {
          return properties;
        }

        @Override
        public List<? extends JsVariable> internalProperties() {
          return internalProperties;
        }

        @Override
        public JsVariable getProperty(String name) {
          Map<String, JsVariable> map = propertyMap;
          if (map == null) {
            List<? extends JsVariable> list = properties();
            map = new HashMap<String, JsVariable>(list.size());
            for (JsVariable property : list) {
              map.put(property.getName(), property);
            }
            // Possibly overwrite other already created map, but we don't care about instance here.
            propertyMap = map;
          }
          return map.get(name);
        }

        @Override
        public int getCacheState() {
          return currentCacheState;
        }
      };
      return Getter.newNormal(result);
    }

    @Override
    public Getter<ObjectProperties> getEmptyResult() {
      return EMPTY_OBJECT_PROPERTIES_GETTER;
    }

    @Override
    public Getter<ObjectProperties> forException(Exception exception) {
      return Getter.newFailure(exception);
    }
  }

  private static final Getter<ObjectProperties> EMPTY_OBJECT_PROPERTIES_GETTER =
      Getter.newNormal(((ObjectProperties) new ObjectProperties() {
        @Override public List<? extends JsVariable> properties() {
          return Collections.emptyList();
        }
        @Override public JsVariable getProperty(String name) {
          return null;
        }
        @Override public List<? extends JsVariable> internalProperties() {
          return Collections.emptyList();
        }
        @Override public int getCacheState() {
          return Integer.MAX_VALUE;
        }
      }));

  <RES> void loadPropertiesAsync(final String objectId,
      final LoadPostprocessor<RES> propertyPostprocessor, boolean reload,
      final int currentCacheState, AsyncFutureRef<RES> output) {
    if (objectId == null) {
      output.initializeTrivial(propertyPostprocessor.getEmptyResult());
      return;
    }

    // The entire operation that first loads properties from remote and then postprocess them
    // (without occupying Dispatch thread).
    // The operation is sync because we don't want to do postprocessing in Dispatch thread.
    AsyncFuture.Operation<RES> syncOperation = new AsyncFuture.Operation<RES>() {
      @Override
      public RelayOk start(AsyncFuture.Callback<RES> callback, SyncCallback syncCallback) {
        loadSync(callback);
        return RelaySyncCallback.finish(syncCallback);
      }

      private void loadSync(AsyncFuture.Callback<RES> callback) {
        // Get response from remote.
        LoadPropertiesResponse response = loadRawPropertiesSync(objectId);

        // Process result (in the calling thread).
        RES result = response.accept(new LoadPropertiesResponse.Visitor<RES>() {
          @Override
          public RES visitData(GetPropertiesData data) {
            // TODO: check exception.
            return propertyPostprocessor.process(data.result(), currentCacheState);
          }

          @Override
          public RES visitFailure(final Exception exception) {
            return propertyPostprocessor.forException(new RuntimeException(
                "Failed to read properties from remote", exception));
          }
        });

        callback.done(result);
      }
    };

    // This is blocking (unless we concur with someone; in this case we're going to wait for result
    // later, on AsyncFuture getter).
    if (reload) {
      output.reinitializeRunning(syncOperation);
    } else {
      output.initializeRunning(syncOperation);
    }
  }

  /**
   * Response is either data or error message. We wrap whatever it is for postprocessing
   * that is conducted off Dispatch thread.
   */
  private static abstract class LoadPropertiesResponse {
    interface Visitor<R> {
      R visitData(GetPropertiesData response);

      R visitFailure(Exception exception);
    }
    abstract <R> R accept(Visitor<R> visitor);
  }

  private LoadPropertiesResponse loadRawPropertiesSync(String objectId) {
    final LoadPropertiesResponse[] result = { null };
    GenericCallback<GetPropertiesData> callback =
        new GenericCallback<GetPropertiesData>() {
      @Override
      public void success(final GetPropertiesData value) {
        result[0] = new LoadPropertiesResponse() {
          @Override
          <R> R accept(Visitor<R> visitor) {
            return visitor.visitData(value);
          }
        };
      }

      @Override
      public void failure(final Exception exception) {
        result[0] = new LoadPropertiesResponse() {
          @Override
          <R> R accept(Visitor<R> visitor) {
            return visitor.visitFailure(exception);
          }
        };
      }
    };

    final GetPropertiesParams request;
    {
      boolean ignoreHasOwnProperty = false;
      request = new GetPropertiesParams(objectId, ignoreHasOwnProperty);
    }

    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    RelayOk relayOk =
        tabImpl.getCommandProcessor().send(request, callback, callbackSemaphore);
    callbackSemaphore.acquireDefault(relayOk);

    return result[0];
  }

  static WipValueLoader castArgument(RemoteValueMapping mapping) {
    try {
      return (WipValueLoader) mapping;
    } catch (ClassCastException e) {
      throw new IllegalArgumentException("Incorrect evaluate context argument", e);
    }
  }

  // List is too short to use HashSet.
  private static final Collection<String> INTERNAL_PROPERTY_NAME =
      Arrays.asList("__proto__", "constructor");
}

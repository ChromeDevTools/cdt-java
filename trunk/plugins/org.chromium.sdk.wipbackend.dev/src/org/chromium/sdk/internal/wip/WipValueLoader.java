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
import org.chromium.sdk.JsObjectProperty;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.PropertyNameBuilder;
import org.chromium.sdk.internal.wip.WipExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.wip.protocol.input.debugger.GetFunctionDetailsData;
import org.chromium.sdk.internal.wip.protocol.input.debugger.LocationValue;
import org.chromium.sdk.internal.wip.protocol.input.runtime.GetPropertiesData;
import org.chromium.sdk.internal.wip.protocol.input.runtime.PropertyDescriptorValue;
import org.chromium.sdk.internal.wip.protocol.output.debugger.GetFunctionDetailsParams;
import org.chromium.sdk.internal.wip.protocol.output.runtime.GetPropertiesParams;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Callback;
import org.chromium.sdk.util.AsyncFutureRef;
import org.chromium.sdk.util.GenericCallback;
import org.chromium.sdk.util.MethodIsBlockingException;

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
   * Loads object properties. It starts and executes a load operation of a corresponding
   * {@link AsyncFuture}. The operation is fully synchronous, so this method normally
   * blocks. There is a chance that other thread is already executing load operation.
   * In this case the method will return immediately, but {@link AsyncFuture} will hold
   * thread until the operation finishes.
   * @param innerNameBuilder name builder for qualified names of all properties and subproperties
   * @param futureRef future reference that will hold result of load operation
   */
  void loadJsObjectPropertiesInFuture(final String objectId,
      PropertyNameBuilder innerNameBuilder, boolean reload, int currentCacheState,
      AsyncFutureRef<Getter<ObjectProperties>> futureRef) throws MethodIsBlockingException {
    ObjectPropertyProcessor propertyProcessor =
        new ObjectPropertyProcessor(innerNameBuilder, objectId);
    loadPropertiesInFuture(objectId, propertyProcessor, reload, currentCacheState, futureRef);
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
    List<? extends JsObjectProperty> properties();
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
    RES process(List<? extends PropertyDescriptorValue> propertyList, int currentCacheState);
    RES getEmptyResult();
    RES forException(Exception exception);
  }

  private class ObjectPropertyProcessor implements LoadPostprocessor<Getter<ObjectProperties>> {
    private final PropertyNameBuilder propertyNameBuilder;
    private final String objectId;

    ObjectPropertyProcessor(PropertyNameBuilder propertyNameBuilder, String objectId) {
      this.propertyNameBuilder = propertyNameBuilder;
      this.objectId = objectId;
    }

    @Override
    public Getter<ObjectProperties> process(
        List<? extends PropertyDescriptorValue> propertyList, final int currentCacheState) {
      final List<JsObjectProperty> properties =
          new ArrayList<JsObjectProperty>(propertyList.size());
      final List<JsVariable> internalProperties = new ArrayList<JsVariable>(2);

      for (PropertyDescriptorValue propertyDescriptor : propertyList) {
        String name = propertyDescriptor.name();
        boolean isInternal = INTERNAL_PROPERTY_NAME.contains(name);

        ValueNameBuilder valueNameBuilder =
            WipExpressionBuilder.createValueOfPropertyNameBuilder(name, propertyNameBuilder);

        JsObjectProperty property = valueBuilder.createObjectProperty(propertyDescriptor,
            objectId, valueNameBuilder);
        if (isInternal) {
          internalProperties.add(property);
        } else {
          properties.add(property);
        }
      }

      final ObjectProperties result = new ObjectProperties() {
        private volatile Map<String, JsVariable> propertyMap = null;

        @Override
        public List<? extends JsObjectProperty> properties() {
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
        @Override public List<? extends JsObjectProperty> properties() {
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

  <RES> void loadPropertiesInFuture(final String objectId,
      final LoadPostprocessor<RES> propertyPostprocessor, boolean reload,
      final int currentCacheState, AsyncFutureRef<RES> futureRef)
      throws MethodIsBlockingException {
    if (objectId == null) {
      futureRef.initializeTrivial(propertyPostprocessor.getEmptyResult());
      return;
    }

    // The entire operation that first loads properties from remote and then postprocess them
    // (without occupying Dispatch thread).
    // The operation is sync because we don't want to do postprocessing in Dispatch thread.
    AsyncFuture.SyncOperation<RES> syncOperation = new AsyncFuture.SyncOperation<RES>() {
      @Override
      protected RES runSync() throws MethodIsBlockingException {
        // Get response from remote.
        LoadPropertiesResponse response = loadRawPropertiesSync(objectId);

        // Process result.
        return response.accept(new LoadPropertiesResponse.Visitor<RES>() {
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
      }
    };

    if (reload) {
      futureRef.reinitializeRunning(syncOperation.asAsyncOperation());
    } else {
      futureRef.initializeRunning(syncOperation.asAsyncOperation());
    }
    syncOperation.execute();
  }

  void loadFunctionLocationInFuture(final String objectId,
      AsyncFutureRef<Getter<LocationValue>> loadedPositionRef) throws MethodIsBlockingException {

    AsyncFuture.Operation<Getter<LocationValue>> operation =
        new AsyncFuture.Operation<Getter<LocationValue>>() {
      @Override
      public RelayOk start(final Callback<Getter<LocationValue>> callback,
          SyncCallback syncCallback) {
        GetFunctionDetailsParams request = new GetFunctionDetailsParams(objectId);
        GenericCallback<GetFunctionDetailsData> wrappedCallback =
            new GenericCallback<GetFunctionDetailsData>() {
          @Override public void success(GetFunctionDetailsData value) {
            callback.done(Getter.newNormal(value.details().location()));
          }

          @Override public void failure(Exception exception) {
            callback.done(Getter.<LocationValue>newFailure(exception));
          }
        };
        return tabImpl.getCommandProcessor().send(request, wrappedCallback, syncCallback);
      }
    };

    loadedPositionRef.initializeRunning(operation);
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

  private LoadPropertiesResponse loadRawPropertiesSync(String objectId)
      throws MethodIsBlockingException {
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
      boolean ownProperties = true;
      request = new GetPropertiesParams(objectId, ownProperties);
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

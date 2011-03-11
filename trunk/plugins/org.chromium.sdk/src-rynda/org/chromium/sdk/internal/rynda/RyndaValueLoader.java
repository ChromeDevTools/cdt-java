// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.rynda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.rynda.RyndaExpressionBuilder.PropertyNameBuilder;
import org.chromium.sdk.internal.rynda.RyndaExpressionBuilder.ValueNameBuilder;
import org.chromium.sdk.internal.rynda.RyndaTabImpl.RyndaDebugContextImpl;
import org.chromium.sdk.internal.rynda.protocol.input.GetPropertiesData;
import org.chromium.sdk.internal.rynda.protocol.input.GetPropertiesData.Property;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse;
import org.chromium.sdk.internal.rynda.protocol.input.RyndaCommandResponse.Success;
import org.chromium.sdk.internal.rynda.protocol.input.ValueData;
import org.chromium.sdk.internal.rynda.protocol.output.GetPropertiesRequest;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFutureRef;

/**
 * Responsible for loading values of properties. It works in pair with {@link RyndaValueBuilder}.
 * TODO: add a cache for already loaded values if remote protocol ever has
 * permanent object ids (same object reported under the same id within a debug context).
 */
public class RyndaValueLoader {
  private final RyndaDebugContextImpl debugContextImpl;
  private final RyndaValueBuilder valueBuilder = new RyndaValueBuilder(this);

  public RyndaValueLoader(RyndaDebugContextImpl debugContextImpl) {
    this.debugContextImpl = debugContextImpl;
  }

  RyndaValueBuilder getValueBuilder() {
    return valueBuilder;
  }

  /**
   * Asynchronously loads object properties. It starts a load operation of a corresponding
   * {@link AsyncFuture}. The operation is fully synchronous, so this method actually
   * blocks. Meanwhile any other thread trying to access the same object properties won't block
   * here, but will wait on the {@link AsyncFuture} getters instead.
   * @param innerNameBuilder name builder for qualified names of all properties and subproperties
   * @param output future object that will hold result of load operation
   */
  void loadJsObjectPropertiesAsync(final ValueData.Id objectId,
      PropertyNameBuilder innerNameBuilder,
      AsyncFutureRef<Getter<ObjectProperties>> output) {
    ObjectPropertyProcessor propertyProcessor = new ObjectPropertyProcessor(innerNameBuilder);
    loadPropertiesAsync(objectId, propertyProcessor, output);
  }

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
    List<? extends JsVariable> internalProperties();
  }

  /**
   * An abstract processor that is reads protocol response and creates proper
   * property objects. This may be implemented differently for objects, functions or
   * scopes.
   */
  interface LoadPostprocessor<RES> {
    RES process(List<? extends Property> propertyList);
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
        List<? extends GetPropertiesData.Property> propertyList) {
      final List<JsVariable> properties = new ArrayList<JsVariable>(propertyList.size());
      final List<JsVariable> internalProperties = new ArrayList<JsVariable>(2);

      for (GetPropertiesData.Property property : propertyList) {
        String name = property.name();
        boolean isInternal = INTERNAL_PROPERTY_NAME.contains(name);

        ValueNameBuilder valueNameBuilder =
            RyndaExpressionBuilder.createValueOfPropertyNameBuilder(name, propertyNameBuilder);

        JsValue jsValue = valueBuilder.wrap(property.value(), valueNameBuilder);
        JsVariable variable = RyndaValueBuilder.createVariable(jsValue, name, valueNameBuilder);
        if (isInternal) {
          internalProperties.add(variable);
        } else {
          properties.add(variable);
        }
      }

      final ObjectProperties result = new ObjectProperties() {
        @Override
        public List<? extends JsVariable> properties() {
          return properties;
        }
        @Override
        public List<? extends JsVariable> internalProperties() {
          return internalProperties;
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
        @Override
        public List<? extends JsVariable> properties() {
          return Collections.emptyList();
        }
        @Override
        public List<? extends JsVariable> internalProperties() {
          return Collections.emptyList();
        }
      }));

  <RES> void loadPropertiesAsync(final ValueData.Id objectId,
      final LoadPostprocessor<RES> propertyPostprocessor, AsyncFutureRef<RES> output) {
    if (objectId == null) {
      output.initializeTrivial(propertyPostprocessor.getEmptyResult());
      return;
    }

    // The entire operation that first loads properties from remote and then postprocess them
    // (without occupying Dispatch thread).
    AsyncFuture.Operation<RES> syncOperation = new AsyncFuture.Operation<RES>() {
      @Override
      public void start(AsyncFuture.Callback<RES> callback, SyncCallback syncCallback) {
        try {
          loadSync(callback);
        } finally {
          if (syncCallback != null) {
            syncCallback.callbackDone(null);
          }
        }
      }

      private void loadSync(AsyncFuture.Callback<RES> callback) {
        // Get response from remote.
        LoadPropertiesResponse response = loadRawPropertiesSync(objectId);

        // Process result (in the calling thread).
        RES result = response.accept(new LoadPropertiesResponse.Visitor<RES>() {
          @Override
          public RES visitData(RyndaCommandResponse.Success success) {
            final GetPropertiesData data;
            try {
              data = success.data().asGetPropertiesData();
            } catch (JsonProtocolParseException e) {
              throw new RuntimeException(e);
            }

            if (data.isException() == Boolean.TRUE) {
              // TODO: prepare better exception description (without JSON details).
              String rawMessage = success.getSuper().getUnderlyingObject().toJSONString();

              // We have no better representation for exception than Getter that throws.
              return propertyPostprocessor.forException(
                  new Exception("Exception in JavaScript: " + rawMessage));
            }

            return propertyPostprocessor.process(data.result());
          }

          @Override
          public RES visitFailure(final String message) {
            return propertyPostprocessor.forException(new RuntimeException(
                "Failed to read properties from remote: " + message));
          }
        });

        callback.done(result);
      }
    };

    // This is blocking (unless we concur with someone; in this case we're going to wait for result
    // later, on AsyncFuture getter).
    output.initializeRunning(syncOperation);
  }

  /**
   * Response is either data or error message. We wrap whatever it is for postprocessing
   * that is conducted off Dispatch thread.
   */
  private static abstract class LoadPropertiesResponse {
    interface Visitor<R> {
      R visitData(RyndaCommandResponse.Success response);

      R visitFailure(String message);
    }
    abstract <R> R accept(Visitor<R> visitor);
  }

  private LoadPropertiesResponse loadRawPropertiesSync(ValueData.Id objectId) {
    final LoadPropertiesResponse[] result = { null };
    RyndaCommandCallback callback = new RyndaCommandCallback.Default() {
      @Override
      protected void onSuccess(final Success success) {
        result[0] = new LoadPropertiesResponse() {
          @Override
          <R> R accept(Visitor<R> visitor) {
            return visitor.visitData(success);
          }
        };
      }

      @Override
      protected void onError(final String message) {
        result[0] = new LoadPropertiesResponse() {
          @Override
          <R> R accept(Visitor<R> visitor) {
            return visitor.visitFailure(message);
          }
        };
      }
    };

    final GetPropertiesRequest request;
    {
      boolean ignoreHasOwnProperty = false;
      boolean abbreviate = true;
      request = new GetPropertiesRequest(objectId.id(),  objectId.injectedScriptId(),
          ignoreHasOwnProperty, abbreviate);
    }

    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();
    debugContextImpl.getCommandProcessor().send(request, callback, callbackSemaphore);
    callbackSemaphore.acquireDefault();

    return result[0];
  }

  // List is too short to use HashSet.
  private static final Collection<String> INTERNAL_PROPERTY_NAME =
      Arrays.asList("__proto__", "constructor");
}

// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.sdk.internal.v8native.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.JsFunction;
import org.chromium.sdk.JsObject;
import org.chromium.sdk.JsVariable;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.protocol.output.EvaluateMessage;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.MethodIsBlockingException;

/**
 * A generic implementation of the JsObject interface.
 * @param <D> type of convenience user-provided property-data-holding structure, that always
 *     includes {@link BasicPropertyData} but may also contain other fields that expire when
 *     properties expire
 */
public abstract class JsObjectBase<D> extends JsValueBase implements JsObject {

  private final long ref;

  private final String className;

  private final ValueLoader valueLoader;

  /**
   * Property data in form of {@link AsyncFuture} for property load operation
   * that several threads may access simultaneously. The future gets reinitialized
   * on the next access after cache state was updated.
   */
  private final AtomicReference<AsyncFuture<D>> propertyDataRef =
      new AtomicReference<AsyncFuture<D>>(null);

  /**
   * This constructor implies the lazy resolution of object properties.
   *
   * @param valueLoader where this instance belongs in
   * @param variableFqn the fully qualified name of the variable holding this object
   * @param valueState the value data from the JS VM
   */
  JsObjectBase(ValueLoader valueLoader, ValueMirror mirror) {
    super(mirror);
    this.valueLoader = valueLoader;
    this.ref = mirror.getRef();
    this.className = mirror.getClassName();
  }

  @Override
  public Collection<JsVariableBase.Property> getProperties() throws MethodIsBlockingException {
    return getBasicPropertyData(true).getPropertyList();
  }

  @Override
  public Collection<JsVariableBase.Impl> getInternalProperties() throws MethodIsBlockingException {
    return getBasicPropertyData(true).getIntenalPropertyList();
  }

  @Override
  public String getRefId() {
    if (ref < 0) {
      // Negative handle means that it's transient. We don't expose it.
      return null;
    } else {
      return String.valueOf(ref);
    }
  }

  @Override
  public ValueLoader getRemoteValueMapping() {
    return valueLoader;
  }

  @Override
  public JsObjectBase<D> asObject() {
    return this;
  }

  @Override
  public JsVariable getProperty(String name) throws MethodIsBlockingException {
    return getBasicPropertyData(true).getPropertyMap().get(name);
  }

  @Override
  public String getClassName() {
    return className;
  }

  @Override
  public EvaluateMessage.Value getJsonParam(InternalContext hostInternalContext) {
    valueLoader.getInternalContext().checkContextIsCompatible(hostInternalContext);
    return EvaluateMessage.Value.createForId(ref);
  }

  @Override
  public String getValueString() {
    switch (getType()) {
      case TYPE_OBJECT:
      case TYPE_ARRAY:
        return "[" + getClassName() + "]";
      case TYPE_FUNCTION:
        return "[Function]";
      default:
        return "";
    }
  }

  protected InternalContext getInternalContext() {
    return valueLoader.getInternalContext();
  }

  protected long getRef() {
    return ref;
  }

  /**
   * Gets or creates property data. The data have cache timestamp inside and gets recreated
   * if checkFreshness is true and if global timestamp has been updated.
   * @param checkFreshness whether data freshness is to be check against global cache timestamp
   * @return property data wrapped in convenience class as specified by
   *     {@link #wrapBasicData(BasicPropertyData)} method
   */
  protected D getPropertyData(boolean checkFreshness) throws MethodIsBlockingException {
    if (propertyDataRef.get() == null) {
      int currentCacheState = getRemoteValueMapping().getCurrentCacheState();
      startPropertyLoadOperation(false, currentCacheState);
    } else {
      if (checkFreshness) {
        int currentCacheState = getRemoteValueMapping().getCurrentCacheState();
        D result = propertyDataRef.get().getSync();
        BasicPropertyData basicPropertyData = unwrapBasicData(result);
        if (basicPropertyData.getCacheState() == currentCacheState) {
          return result;
        }
        startPropertyLoadOperation(true, currentCacheState);
      }
    }

    return propertyDataRef.get().getSync();
  }

  /**
   * Convenience method that gets property data and returns wrapped {@link BasicPropertyData}.
   */
  protected BasicPropertyData getBasicPropertyData(boolean checkFreshness)
      throws MethodIsBlockingException {
    D propertyData = getPropertyData(checkFreshness);
    return unwrapBasicData(propertyData);
  }

  private void startPropertyLoadOperation(boolean reload, final int currentCacheState)
      throws MethodIsBlockingException {
    // The operation is blocking, because we will wait for its result anyway.
    // On the other hand there is a post-load job that we need a thread to occupy with.

    AsyncFuture.SyncOperation<D> blockingOperation =
        new AsyncFuture.SyncOperation<D>() {
      @Override
      protected D runSync() throws MethodIsBlockingException {
        SubpropertiesMirror subpropertiesMirror =
            getRemoteValueMapping().getOrLoadSubproperties(ref);

        List<JsVariableBase.Property> properties = wrapProperties(
            subpropertiesMirror.getProperties(), getRef(), PropertyMirrorParser.PROPERTY);
        List<JsVariableBase.Impl> internalProperties = wrapProperties(
            subpropertiesMirror.getInternalProperties(), null, PropertyMirrorParser.VARIABLE);

        BasicPropertyData data = new BasicPropertyData(currentCacheState, properties,
            internalProperties, subpropertiesMirror);
        return wrapBasicData(data);
      }

      private <V> List<V> wrapProperties(List<? extends PropertyReference> propertyRefs,
          Long hostRef, PropertyMirrorParser<V> parser) throws MethodIsBlockingException {
        List<ValueMirror> subMirrors = valueLoader.getOrLoadValueFromRefs(propertyRefs);

        List<V> wrappedProperties = createPropertiesFromMirror(subMirrors,
            propertyRefs, hostRef, parser);
        return Collections.unmodifiableList(wrappedProperties);
      }
    };
    if (reload) {
      AsyncFuture.reinitializeReference(propertyDataRef, blockingOperation.asAsyncOperation());
    } else {
      AsyncFuture.initializeReference(propertyDataRef, blockingOperation.asAsyncOperation());
    }
    blockingOperation.execute();
  }

  /**
   * User-provided method that wraps basic property data in the class of user choice D.
   * User wrapper will be kept by {@link JsObjectBase} and easily accessible when needed,
   * plus it will be dropped when caches become reset.
   * <p>
   * Alternative design would be to require D to extend BasicPropertyData, but we would have
   * to expose all of its constructor parameters in this case.
   */
  protected abstract D wrapBasicData(BasicPropertyData basicPropertyData);

  /**
   * User-provided method that extracts basic property data from user-provided data class.
   */
  protected abstract BasicPropertyData unwrapBasicData(D wrappedBasicData);

  /**
   * Contains immutable data about object properties plus lazy-initialized fields that are
   * derived from property data. There can be more fields in user-provided wrapper class.
   */
  protected static class BasicPropertyData {
    private final int cacheState;

    private final List<JsVariableBase.Property> propertyList;
    private final List<JsVariableBase.Impl> intenalPropertyList;
    private final SubpropertiesMirror subpropertiesMirror;

    private volatile Map<String, JsVariableBase> propertyMap = null;

    BasicPropertyData(int cacheState,
        List<JsVariableBase.Property> propertyList,
        List<JsVariableBase.Impl> intenalPropertyList, SubpropertiesMirror subpropertiesMirror) {
      this.cacheState = cacheState;
      this.propertyList = propertyList;
      this.intenalPropertyList = intenalPropertyList;
      this.subpropertiesMirror = subpropertiesMirror;
    }

    int getCacheState() {
      return cacheState;
    }

    List<JsVariableBase.Property> getPropertyList() {
      return propertyList;
    }

    List<JsVariableBase.Impl> getIntenalPropertyList() {
      return intenalPropertyList;
    }

    SubpropertiesMirror getSubpropertiesMirror() {
      return subpropertiesMirror;
    }

    Map<String, JsVariableBase> getPropertyMap() {
      // Method is not synchronized -- it's OK if we initialize volatile propertyMap field
      // several times.
      if (propertyMap == null) {
        Map<String, JsVariableBase> map =
            new HashMap<String, JsVariableBase>(propertyList.size() * 2, 0.75f);
        for (JsVariableBase prop : propertyList) {
          map.put(prop.getName(), prop);
        }
        // Make make synchronized for such not thread-safe methods as entrySet.
        propertyMap = Collections.unmodifiableMap(Collections.synchronizedMap(map));
      }
      return propertyMap;
    }
  }

  private <V> List<V> createPropertiesFromMirror(List<ValueMirror> mirrorProperties,
      List<? extends PropertyReference> propertyRefs, Long hostRef,
      PropertyMirrorParser<V> parser) {
    List<V> result = new ArrayList<V>(mirrorProperties.size());
    for (int i = 0; i < mirrorProperties.size(); i++) {
      ValueMirror mirror = mirrorProperties.get(i);
      Object varName = propertyRefs.get(i).getName();
      result.add(parser.parse(valueLoader, mirror, varName));
    }
    return result;
  }

  /**
   * A helper class that helps parameterize some methods with either variable or property.
   * Functionally it contains factory method, that creates either this or that.
   * @param <V> variable or property type
   */
  private static abstract class PropertyMirrorParser<V> {
    abstract V parse(ValueLoader valueLoader, ValueMirror valueData,
        Object rawName);

    static final PropertyMirrorParser<JsVariableBase.Impl> VARIABLE =
        new PropertyMirrorParser<JsVariableBase.Impl>() {
          @Override
          JsVariableBase.Impl parse(ValueLoader valueLoader,
              ValueMirror valueData, Object rawName) {
            return new JsVariableBase.Impl(valueLoader, valueData, rawName);
          }
        };

    // This parser is expected to parse getter, setter and other JavaScript property properties.
    // TODO: implement getter, setter etc. once supported by protocol.
    static final PropertyMirrorParser<JsVariableBase.Property> PROPERTY =
        new PropertyMirrorParser<JsVariableBase.Property>() {
          @Override
          JsVariableBase.Property parse(ValueLoader valueLoader,
              ValueMirror valueData, Object rawName) {
            return new JsVariableBase.Property(valueLoader, valueData, rawName);
          }
        };
  }

  public static class Impl extends JsObjectBase<BasicPropertyData> {
    Impl(ValueLoader valueLoader, ValueMirror valueState) {
      super(valueLoader, valueState);
    }

    @Override
    public JsArrayImpl asArray() {
      return null;
    }

    @Override
    public JsFunction asFunction() {
      return null;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      result.append("[JsObject: type=").append(getType());
      try {
        for (JsVariable prop : getProperties()) {
          result.append(',').append(prop);
        }
      } catch (MethodIsBlockingException e) {
        return "[JsObject: Exception in retrieving data]";
      }
      result.append(']');
      return result.toString();
    }

    @Override
    protected BasicPropertyData wrapBasicData(BasicPropertyData basicPropertyData) {
      return basicPropertyData;
    }

    @Override
    protected BasicPropertyData unwrapBasicData(BasicPropertyData additionalPropertyStore) {
      return additionalPropertyStore;
    }
  }
}

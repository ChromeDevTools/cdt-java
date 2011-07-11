// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.MethodIsBlockingException;
import org.chromium.sdk.util.AsyncFuture;
import org.chromium.sdk.util.AsyncFuture.Callback;
import org.chromium.sdk.util.RelaySyncCallback;

/**
 * A generic implementation of the JsObject interface.
 * @param <D> type of convenience user-provided property-data-holding structure, that always
 *     includes {@link BasicPropertyData} but may also contain other fields that expire when
 *     properties expire
 */
public abstract class JsObjectBase<D> extends JsValueBase implements JsObject {

  private final long ref;

  private final String className;

  private final InternalContext context;

  /**
   * Fully qualified name of variable holding this object.
   */
  private final String variableFqn;

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
   * @param context where this instance belongs in
   * @param variableFqn the fully qualified name of the variable holding this object
   * @param valueState the value data from the JS VM
   */
  JsObjectBase(InternalContext context, String variableFqn, ValueMirror mirror) {
    super(mirror);
    this.context = context;
    this.ref = mirror.getRef();
    this.className = mirror.getClassName();
    this.variableFqn = variableFqn;
  }

  @Override
  public Collection<JsVariableImpl> getProperties() throws MethodIsBlockingException {
    return getBasicPropertyData(true).getPropertyList();
  }

  @Override
  public Collection<JsVariableImpl> getInternalProperties() throws MethodIsBlockingException {
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
    return context.getValueLoader();
  }

  public static int parseRefId(String value) {
    return Integer.parseInt(value);
  }

  @Override
  public JsObjectBase<D> asObject() {
    return this;
  }

  @Override
  public JsVariable getProperty(String name) {
    return getBasicPropertyData(true).getPropertyMap().get(name);
  }

  @Override
  public String getClassName() {
    return className;
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
    return context;
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
  protected D getPropertyData(boolean checkFreshness) {
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
  protected BasicPropertyData getBasicPropertyData(boolean checkFreshness) {
    D propertyData = getPropertyData(checkFreshness);
    return unwrapBasicData(propertyData);
  }

  private void startPropertyLoadOperation(boolean reload, final int currentCacheState) {
    // The operation is blocking, because we will wait for its result anyway.
    // On the other hand there is a post-load job that we need a thread to occupy with.
    AsyncFuture.Operation<D> operation = new AsyncFuture.Operation<D>() {
      @Override
      public RelayOk start(Callback<D> callback, SyncCallback syncCallback) {

        SubpropertiesMirror subpropertiesMirror =
            getRemoteValueMapping().loadSubpropertiesInMirror(ref);

        List<JsVariableImpl> properties = wrapProperties(subpropertiesMirror.getProperties());
        List<JsVariableImpl> internalProperties =
            wrapProperties(subpropertiesMirror.getInternalProperties());

        BasicPropertyData data = new BasicPropertyData(currentCacheState, properties,
            internalProperties, subpropertiesMirror);
        callback.done(wrapBasicData(data));

        return RelaySyncCallback.finish(syncCallback);
      }

      private List<JsVariableImpl> wrapProperties(List<? extends PropertyReference> propertyRefs) {
        ValueLoader valueLoader = context.getValueLoader();
        List<ValueMirror> subMirrors = valueLoader.getOrLoadValueFromRefs(propertyRefs);

        List<JsVariableImpl> wrappedProperties = createPropertiesFromMirror(subMirrors,
            propertyRefs);
        return Collections.unmodifiableList(wrappedProperties);
      }
    };
    if (reload) {
      AsyncFuture.reinitializeReference(propertyDataRef, operation);
    } else {
      AsyncFuture.initializeReference(propertyDataRef, operation);
    }
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

    private final List<JsVariableImpl> propertyList;
    private final List<JsVariableImpl> intenalPropertyList;
    private final SubpropertiesMirror subpropertiesMirror;

    private volatile Map<String, JsVariableImpl> propertyMap = null;

    BasicPropertyData(int cacheState,
        List<JsVariableImpl> propertyList,
        List<JsVariableImpl> intenalPropertyList, SubpropertiesMirror subpropertiesMirror) {
      this.cacheState = cacheState;
      this.propertyList = propertyList;
      this.intenalPropertyList = intenalPropertyList;
      this.subpropertiesMirror = subpropertiesMirror;
    }

    int getCacheState() {
      return cacheState;
    }

    List<JsVariableImpl> getPropertyList() {
      return propertyList;
    }

    List<JsVariableImpl> getIntenalPropertyList() {
      return intenalPropertyList;
    }

    SubpropertiesMirror getSubpropertiesMirror() {
      return subpropertiesMirror;
    }

    Map<String, JsVariableImpl> getPropertyMap() {
      // Method is not synchronized -- it's OK if we initialize volatile propertyMap field
      // several times.
      if (propertyMap == null) {
        Map<String, JsVariableImpl> map =
            new HashMap<String, JsVariableImpl>(propertyList.size() * 2, 0.75f);
        for (JsVariableImpl prop : propertyList) {
          map.put(prop.getName(), prop);
        }
        // Make make synchronized for such not thread-safe methods as entrySet.
        propertyMap = Collections.unmodifiableMap(Collections.synchronizedMap(map));
      }
      return propertyMap;
    }
  }

  private List<JsVariableImpl> createPropertiesFromMirror(List<ValueMirror> mirrorProperties,
      List<? extends PropertyReference> propertyRefs) throws MethodIsBlockingException {
    // TODO(peter.rybin) Maybe assert that context is valid here

    List<JsVariableImpl> result = new ArrayList<JsVariableImpl>(mirrorProperties.size());
    for (int i = 0; i < mirrorProperties.size(); i++) {
      ValueMirror mirror = mirrorProperties.get(i);
      Object varName = propertyRefs.get(i).getName();
      String fqn = getFullyQualifiedName(varName);
      if (fqn == null) {
        continue;
      }
      String decoratedName = JsVariableImpl.NameDecorator.decorateVarName(varName);
      result.add(new JsVariableImpl(context, mirror, varName, decoratedName, fqn));
    }
    return result;
  }

  private String getFullyQualifiedName(Object propName) {
    if (variableFqn == null) {
      return null;
    }
    if (propName instanceof String) {
      String propNameStr = (String) propName;
      if (propNameStr.startsWith(".")) {
        // ".arguments" is not legal
        return null;
      }
    }
    return variableFqn + JsVariableImpl.NameDecorator.buildAccessSuffix(propName);
  }

  public static class Impl extends JsObjectBase<BasicPropertyData> {
    Impl(InternalContext context, String variableFqn, ValueMirror valueState) {
      super(context, variableFqn, valueState);
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

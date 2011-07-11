// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal.v8native.value;

import static org.chromium.sdk.util.BasicUtil.getSafe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.chromium.sdk.JavascriptVm;
import org.chromium.sdk.JavascriptVm.GenericCallback;
import org.chromium.sdk.JsValue;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.RemoteValueMapping;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.internal.JsonUtil;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.v8native.DebugSession;
import org.chromium.sdk.internal.v8native.InternalContext;
import org.chromium.sdk.internal.v8native.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.v8native.V8BlockingCallback;
import org.chromium.sdk.internal.v8native.V8CommandCallbackBase;
import org.chromium.sdk.internal.v8native.V8Helper;
import org.chromium.sdk.internal.v8native.protocol.input.ScopeBody;
import org.chromium.sdk.internal.v8native.protocol.input.SuccessCommandResponse;
import org.chromium.sdk.internal.v8native.protocol.input.V8ProtocolParserAccess;
import org.chromium.sdk.internal.v8native.protocol.input.data.ObjectValueHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.RefWithDisplayData;
import org.chromium.sdk.internal.v8native.protocol.input.data.SomeHandle;
import org.chromium.sdk.internal.v8native.protocol.input.data.ValueHandle;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessage;
import org.chromium.sdk.internal.v8native.protocol.output.DebuggerMessageFactory;
import org.chromium.sdk.internal.v8native.protocol.output.LookupMessage;
import org.json.simple.JSONObject;

/**
 * The elaborate factory and storage for {@link ValueMirror}'s, that loads values from remote and
 * caches them. All the data comes originally in form of JSON strings which may contain
 * less or more fields, so it creates {@link ValueMirror} accordingly.
 * {@link ValueMirror} is an immutable wrapper around JSON data. Several data instances
 * may occur, the map should always hold the fullest (and the less expired) version.
 * <p>V8 typically sends a lot of (unsolicited) data about properties. There could be various
 * strategies about whether to parse and add them into a map or save parsing time and ignore.
 */
public class ValueLoader implements RemoteValueMapping {

  private final ConcurrentMap<Long, ValueMirror> refToMirror =
      new ConcurrentHashMap<Long, ValueMirror>();

  private final HandleManager specialHandleManager = new HandleManager();

  private final InternalContext context;
  private final LoadableString.Factory loadableStringFactory;

  private final AtomicInteger cacheStateRef = new AtomicInteger(1);

  public ValueLoader(InternalContext context) {
    this.context = context;
    this.loadableStringFactory = new StringFactory();
  }

  public LoadableString.Factory getLoadableStringFactory() {
    return loadableStringFactory;
  }

  public HandleManager getSpecialHandleManager() {
    return specialHandleManager;
  }

  @Override
  public void clearCaches() {
    cacheStateRef.incrementAndGet();
    refToMirror.clear();
  }

  int getCurrentCacheState() {
    return cacheStateRef.get();
  }

  public void addHandleFromRefs(SomeHandle handle) {
    if (HandleManager.isSpecialType(handle.type())) {
      specialHandleManager.put(handle.handle(), handle);
    } else {
      ValueHandle valueHandle;
      try {
        valueHandle = handle.asValueHandle();
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException(e);
      }
      addDataToMap(valueHandle);
    }
  }

  public ValueMirror addDataToMap(RefWithDisplayData refWithDisplayData) {
    ValueMirror mirror = ValueMirror.create(refWithDisplayData, getLoadableStringFactory());
    return putValueMirrorIntoMapRecursive(mirror);
  }

  public ValueMirror addDataToMap(ValueHandle valueHandle) {
    ValueMirror mirror = ValueMirror.create(valueHandle, getLoadableStringFactory());
    return putValueMirrorIntoMapRecursive(mirror);
  }

  public ValueMirror addDataToMap(Long ref, JsValue.Type type, String className,
      LoadableString loadableString, SubpropertiesMirror subpropertiesMirror) {
    ValueMirror mirror =
        ValueMirror.create(ref, type, className, loadableString, subpropertiesMirror);
    return putValueMirrorIntoMapRecursive(mirror);
  }

  private ValueMirror putValueMirrorIntoMapRecursive(ValueMirror mirror) {
    if (PRE_PARSE_PROPERTIES) {
      SubpropertiesMirror subpropertiesMirror = mirror.getProperties();
      if (subpropertiesMirror != null) {
        subpropertiesMirror.reportAllProperties(this);
      }
    }
    return mergeValueMirrorIntoMap(mirror.getRef(), mirror);
  }

  private ValueMirror mergeValueMirrorIntoMap(Long ref, ValueMirror mirror) {
    while (true) {
      ValueMirror old = refToMirror.putIfAbsent(ref, mirror);
      if (old == null) {
        return mirror;
      }
      ValueMirror merged = ValueMirror.merge(old, mirror);
      if (merged == old) {
        return merged;
      }
      boolean updated = refToMirror.replace(ref, old, merged);
      if (updated) {
        return merged;
      }
    }
  }

  private static final boolean PRE_PARSE_PROPERTIES = false;

  /**
   * Looks up {@link ValueMirror} in map, loads them if needed or reloads them
   * if property data is unavailable (or expired).
   */
  public SubpropertiesMirror loadSubpropertiesInMirror(Long ref) {
    ValueMirror mirror = getSafe(refToMirror, ref);

    SubpropertiesMirror references;
    if (mirror == null) {
      references = null;
    } else {
      references = mirror.getProperties();
    }
    if (references == null) {
      // need to look up this value again
      List<ValueMirror> loadedMirrors =
          loadValuesFromRemote(Collections.singletonList(ref));
      ValueMirror loadedMirror = loadedMirrors.get(0);
      references = loadedMirror.getProperties();
      if (references == null) {
        throw new RuntimeException("Failed to load properties");
      }
    }
    return references;
  }

  /**
   * Looks up data for scope on remote in form of scope object handle.
   */
  public ObjectValueHandle loadScopeFields(int scopeNumber, int frameNumber) {
    DebuggerMessage message = DebuggerMessageFactory.scope(scopeNumber, frameNumber);

    V8BlockingCallback<ObjectValueHandle> callback = new V8BlockingCallback<ObjectValueHandle>() {
      @Override
      protected ObjectValueHandle handleSuccessfulResponse(
          SuccessCommandResponse response) {
        return readFromScopeResponse(response);
      }
    };

    try {
      return V8Helper.callV8Sync(context, message, callback);
    } catch (ContextDismissedCheckedException e) {
      context.getDebugSession().maybeRethrowContextException(e);
      // or
      return null;
    }
  }

  private ObjectValueHandle readFromScopeResponse(SuccessCommandResponse response) {
    List<SomeHandle> refs = response.refs();

    for (SomeHandle handle : refs) {
      addHandleFromRefs(handle);
    }

    ScopeBody body;
    try {
      body = response.body().asScopeBody();
    } catch (JsonProtocolParseException e) {
      throw new ValueLoadException(e);
    }
    return body.object();
  }

  /**
   * For each PropertyReference from propertyRefs tries to either: 1. read it from PropertyReference
   * (possibly cached value) or 2. lookup value by refId from remote
   */
  public List<ValueMirror> getOrLoadValueFromRefs(List<? extends PropertyReference> propertyRefs) {
    ValueMirror[] result = new ValueMirror[propertyRefs.size()];
    Map<Long, Integer> refToRequestIndex = new HashMap<Long, Integer>();
    List<PropertyReference> needsLoading = new ArrayList<PropertyReference>();

    for (int i = 0; i < propertyRefs.size(); i++) {
      PropertyReference property = propertyRefs.get(i);
      DataWithRef dataWithRef = property.getValueObject();
      Long ref = dataWithRef.ref();
      RefWithDisplayData dataWithDisplayData = dataWithRef.getWithDisplayData();
      ValueMirror mirror;
      if (dataWithDisplayData == null) {
        mirror = getSafe(refToMirror, ref);
      } else {
        mirror = ValueMirror.create(dataWithDisplayData, loadableStringFactory);
      }
      if (mirror == null) {
        // We don't have the data (enough) right now. We are requesting them from server.
        // There might be simultaneous request for the same value, which is a normal though
        // undesired case.
        Integer requestPos = getSafe(refToRequestIndex, ref);
        if (requestPos == null) {
          refToRequestIndex.put(ref, needsLoading.size());
          needsLoading.add(property);
        }
      } else {
        result[i] = mirror;
      }
    }

    if (!needsLoading.isEmpty()) {
      List<Long> refIds = getRefIdFromReferences(needsLoading);
      List<ValueMirror> loadedMirrors = loadValuesFromRemote(refIds);
      assert refIds.size() == loadedMirrors.size();
      for (int i = 0; i < propertyRefs.size(); i++) {
        if (result[i] == null) {
          PropertyReference property = propertyRefs.get(i);
          DataWithRef dataWithRef = property.getValueObject();
          Long ref = dataWithRef.ref();
          int pos = getSafe(refToRequestIndex, ref);
          result[i] = loadedMirrors.get(pos);
        }
      }
    }
    return Arrays.asList(result);
  }

  private static List<Long> getRefIdFromReferences(final List<PropertyReference> propertyRefs) {
    List<Long> result = new ArrayList<Long>(propertyRefs.size());
    for (PropertyReference ref : propertyRefs) {
      result.add(Long.valueOf(ref.getRef()));
    }
    return result;
  }

  /**
   * Requests values from remote via "lookup" command. Automatically caches received data.
   * @param propertyRefIds list of ref ids we need to look up
   * @return loaded value mirrors in the same order as in propertyRefIds
   */
  public List<ValueMirror> loadValuesFromRemote(final List<Long> propertyRefIds) {
    if (propertyRefIds.isEmpty()) {
      return Collections.emptyList();
    }

    DebuggerMessage message = DebuggerMessageFactory.lookup(propertyRefIds, false);

    V8BlockingCallback<List<ValueMirror>> callback =
        new V8BlockingCallback<List<ValueMirror>>() {
      @Override
      protected List<ValueMirror> handleSuccessfulResponse(
          SuccessCommandResponse response) {
        return readResponseFromLookup(response, propertyRefIds);
      }
    };

    try {
      return V8Helper.callV8Sync(context, message, callback);
    } catch (ContextDismissedCheckedException e) {
      context.getDebugSession().maybeRethrowContextException(e);
      // or
      throw new ValueLoadException("Invalid context", e);
    }
  }

  private List<ValueMirror> readResponseFromLookup(
      SuccessCommandResponse successResponse, List<Long> propertyRefIds) {
    List<ValueMirror> result = new ArrayList<ValueMirror>(propertyRefIds.size());
    JSONObject body;
    try {
      body = successResponse.body().asLookupMap();
    } catch (JsonProtocolParseException e) {
      throw new ValueLoadException(e);
    }
    for (int i = 0; i < propertyRefIds.size(); i++) {
      int ref = propertyRefIds.get(i).intValue();
      JSONObject value = JsonUtil.getAsJSON(body, String.valueOf(ref));
      if (value == null) {
        throw new ValueLoadException("Failed to find value for ref=" + ref);
      }
      ValueHandle valueHandle;
      try {
        valueHandle = V8ProtocolParserAccess.get().parse(value, ValueHandle.class);
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException(e);
      }

      long refLong = valueHandle.handle();
      if (refLong != ref) {
        throw new ValueLoadException("Inconsistent ref in response, ref=" + ref);
      }
      ValueMirror mirror = addDataToMap(valueHandle);
      result.add(mirror);
    }
    return result;
  }

  private List<ValueHandle> readResponseFromLookupRaw(SuccessCommandResponse successResponse,
      List<Long> propertyRefIds) {
    List<ValueHandle> result = new ArrayList<ValueHandle>(propertyRefIds.size());
    JSONObject body;
    try {
      body = successResponse.body().asLookupMap();
    } catch (JsonProtocolParseException e) {
      throw new ValueLoadException(e);
    }
    for (int i = 0; i < propertyRefIds.size(); i++) {
      int ref = propertyRefIds.get(i).intValue();
      JSONObject value = JsonUtil.getAsJSON(body, String.valueOf(ref));
      if (value == null) {
        throw new ValueLoadException("Failed to find value for ref=" + ref);
      }
      ValueHandle valueHandle;
      try {
        valueHandle = V8ProtocolParserAccess.get().parse(value, ValueHandle.class);
      } catch (JsonProtocolParseException e) {
        throw new ValueLoadException(e);
      }

      addDataToMap(valueHandle);

      result.add(valueHandle);
    }
    return result;
  }

  private RelayOk relookupValue(long handleId, Long maxLength,
      final JavascriptVm.GenericCallback<ValueHandle> callback,
      SyncCallback syncCallback) throws ContextDismissedCheckedException {
    final List<Long> ids = Collections.singletonList(handleId);
    DebuggerMessage message = new LookupMessage(ids, false, maxLength);

    V8CommandCallbackBase innerCallback = new V8CommandCallbackBase() {
      @Override
      public void success(SuccessCommandResponse successResponse) {
        List<ValueHandle> handleList = readResponseFromLookupRaw(successResponse, ids);
        callback.success(handleList.get(0));
      }
      @Override
      public void failure(String message) {
        callback.failure(new Exception(message));
      }
    };

    return this.context.sendV8CommandAsync(message, true, innerCallback, syncCallback);
  }

  private class StringFactory implements LoadableString.Factory {
    @Override
    public LoadableString create(ValueHandle handle) {
      final long handleId = handle.handle();
      final LoadedValue initialValue = new LoadedValue(handle);

      return new LoadableString() {
        private final AtomicReference<LoadedValue> valueRef =
            new AtomicReference<LoadedValue>(initialValue);

        @Override
        public String getCurrentString() {
          return valueRef.get().stringValue;
        }

        @Override
        public boolean needsReload() {
          LoadedValue loadedValue = valueRef.get();
          return loadedValue.loadedSize < loadedValue.actualSize;
        }

        @Override
        public RelayOk reloadBigger(final GenericCallback<Void> callback,
            SyncCallback syncCallback) {

          long currentlyLoadedSize = valueRef.get().loadedSize;
          long newRequstedSize = chooseNewMaxStringLength(currentlyLoadedSize);

          JavascriptVm.GenericCallback<ValueHandle> innerCallback =
              new JavascriptVm.GenericCallback<ValueHandle>() {
            @Override
            public void success(ValueHandle handle) {
              LoadedValue newLoadedValue = new LoadedValue(handle);
              replaceValue(handle, newLoadedValue);
              if (callback != null) {
                callback.success(null);
              }
            }

            @Override
            public void failure(Exception e) {
              if (callback != null) {
                callback.failure(new Exception(e));
              }
            }
          };

          try {
            return relookupValue(handleId, newRequstedSize, innerCallback, syncCallback);
          } catch (final ContextDismissedCheckedException e) {
            DebugSession debugSession = context.getDebugSession();
            debugSession.maybeRethrowContextException(e);
            // or
            return debugSession.sendLoopbackMessage(new Runnable() {
              @Override
              public void run() {
                if (callback != null) {
                  callback.failure(e);
                }
              }
            }, syncCallback);
          }
        }

        private void replaceValue(ValueHandle handle, LoadedValue newValue) {
          while (true) {
            LoadedValue currentValue = valueRef.get();
            if (currentValue.loadedSize >= newValue.loadedSize) {
              return;
            }
            boolean updated = valueRef.compareAndSet(currentValue, newValue);
            if (updated) {
              return;
            }
          }
        }
        private long chooseNewMaxStringLength(long currentSize) {
          return Math.max(currentSize * 10, 64 * 1024);
        }
      };
    }

    private class LoadedValue {
      final String stringValue;
      final long loadedSize;
      final long actualSize;

      LoadedValue(ValueHandle handle) {
        this.stringValue = (String) handle.value();
        Long toIndex = handle.toIndex();
        if (toIndex == null) {
          this.loadedSize = this.stringValue.length();
          this.actualSize = this.loadedSize;
        } else {
          this.loadedSize = toIndex;
          this.actualSize = handle.length();
        }
      }
    }
  }
}

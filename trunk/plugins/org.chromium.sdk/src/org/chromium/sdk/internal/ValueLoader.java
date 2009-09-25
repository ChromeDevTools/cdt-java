// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.chromium.sdk.CallbackSemaphore;
import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.tools.v8.V8CommandProcessor;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessage;
import org.chromium.sdk.internal.tools.v8.request.DebuggerMessageFactory;
import org.json.simple.JSONObject;

/**
 * The elaborate factory for {@link ValueMirror}'s, that loads values from remote and
 * caches them. All the data comes originally in form of JSON strings which may contain
 * less or more fields, so it creates {@link ValueMirror} or {@link PropertyHoldingValueMirror}
 * accordingly.
 */
public class ValueLoader {
  private final ConcurrentMap<Long, ValueMirror> refToMirror =
      new ConcurrentHashMap<Long, ValueMirror>();
  private final InternalContext context;

  ValueLoader(InternalContext context) {
    this.context = context;
  }

  /**
   * Receives {@link ValueMirror} and makes sure it has its properties loaded.
   */
  public PropertyHoldingValueMirror loadSubpropertiesInMirror(ValueMirror mirror) {
    PropertyHoldingValueMirror references = mirror.getProperties();
    if (references == null) {
      // need to look up this value again
      List<PropertyHoldingValueMirror> loadedMirrors =
          loadValuesFromRemote(Collections.singletonList(Long.valueOf(mirror.getRef())));
      references = loadedMirrors.get(0);
    }
    return references;
  }

  /**
   * For each PropertyReference from propertyRefs tries to either: 1. read it from PropertyReference
   * (possibly cached value) or 2. lookup value by refId from remote
   */
  public List<ValueMirror> getOrLoadValueFromRefs(List<PropertyReference> propertyRefs) {
    ValueMirror[] result = new ValueMirror[propertyRefs.size()];
    List<Integer> mapForLoadResults = new ArrayList<Integer>();
    List<PropertyReference> needsLoading = new ArrayList<PropertyReference>();

    for (int i = 0; i < propertyRefs.size(); i++) {
      PropertyReference ref = propertyRefs.get(i);
      ValueMirror mirror = readFromPropertyReference(ref);
      if (mirror == null) {
        // We don't have the data (enough) right now. We are requesting them from server.
        // There might be simultaneous request for the same value, which is a normal though
        // undesired case.
        needsLoading.add(ref);
        mapForLoadResults.add(i);
      }
      result[i] = mirror;
    }

    List<Long> refIds = getRefIdFromReferences(needsLoading);
    List<PropertyHoldingValueMirror> loadedMirrors = loadValuesFromRemote(refIds);
    assert refIds.size() == loadedMirrors.size();
    for (int i = 0; i < loadedMirrors.size(); i++) {
      int pos = mapForLoadResults.get(i);
      result[pos] = loadedMirrors.get(i).getValueMirror();
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
   * Reads data from caches or from JSON from propertyReference. Never accesses remote.
   */
  private ValueMirror readFromPropertyReference(PropertyReference propertyReference) {
    int refId = propertyReference.getRef();
    Long refIdObject = Long.valueOf(refId);

    ValueMirror mirror = refToMirror.get(refIdObject);
    if (mirror != null) {
      return mirror;
    }
    JSONObject cachedHandle = context.getHandleManager().getHandle(refIdObject);
    // If we have cached handle, we reads cached handle, not using one from propertyeReference
    // because we expect to find more complete version in cache. Is it ok?
    if (cachedHandle != null) {
      mirror = V8Helper.createValueMirrorOptional(cachedHandle);
    } else {
      JSONObject handleFromProperty = propertyReference.getValueObject();

      mirror = V8Helper.createValueMirrorOptional(handleFromProperty);
    }
    if (mirror != null) {
      ValueMirror mirror2 = refToMirror.putIfAbsent(refIdObject, mirror);
      if (mirror2 != null) {
        mergeMirrors(mirror2, mirror);
      }
    }

    return mirror;
  }

  /**
   * Requests values from remote via "lookup" command. Automatically caches JSON objects
   * in {@link HandleManager}.
   * @param propertyRefIds list of ref ids we need to look up
   * @return loaded value mirrors in the same order as in propertyRefIds
   */
  public List<PropertyHoldingValueMirror> loadValuesFromRemote(final List<Long> propertyRefIds) {
    if (propertyRefIds.isEmpty()) {
      return Collections.emptyList();
    }

    DebuggerMessage message = DebuggerMessageFactory.lookup(propertyRefIds, false);
    CallbackSemaphore callbackSemaphore = new CallbackSemaphore();

    final Exception[] exceptionBuff = { null };
    final List<PropertyHoldingValueMirror> result =
        new ArrayList<PropertyHoldingValueMirror>(propertyRefIds.size());

    V8CommandProcessor.V8HandlerCallback callback = new V8CommandProcessor.V8HandlerCallback() {
      public void messageReceived(JSONObject response) {
        if (!JsonUtil.isSuccessful(response)) {
          exceptionBuff[0] = new Exception("Unsuccessful result");
          return;
        }
        readResponseFromLookup(response, propertyRefIds, result);
      }

      public void failure(String message) {
        exceptionBuff[0] = new Exception(message);
      }
    };
    try {
      context.sendMessageAsync(message, true, callback, callbackSemaphore);
    } catch (ContextDismissedCheckedException e) {
      context.getDebugSession().maybeRethrowContextException(e);
      // or
      return Collections.emptyList();
    }
    boolean res = callbackSemaphore.tryAcquireDefault();
    if (!res) {
      throw new ValueLoadException("Timeout");
    }

    if (exceptionBuff[0] != null) {
      throw new ValueLoadException(exceptionBuff[0]);
    }

    return result;
  }

  private void readResponseFromLookup(JSONObject response, List<Long> propertyRefIds,
      List<PropertyHoldingValueMirror> result) {
    JSONObject body = JsonUtil.getAsJSON(response, "body");
    for (int i = 0; i < propertyRefIds.size(); i++) {
      int ref = propertyRefIds.get(i).intValue();
      JSONObject value = JsonUtil.getAsJSON(body, String.valueOf(ref));
      if (value == null) {
        throw new ValueLoadException("Failed to find value for ref=" + ref);
      }
      context.getHandleManager().put((long)ref, value);
      result.add(readMirrorFromLookup(ref, value));
    }
  }

  /**
   * Constructs a ValueMirror given a V8 debugger object specification and the
   * value name.
   *
   * @param jsonValue containing the object specification from the V8 debugger
   * @param ref
   * @return a ValueMirror instance with the specified name, containing data
   *         from handle, or {@code null} if {@code handle} is not a handle
   */
  private PropertyHoldingValueMirror readMirrorFromLookup(int ref, JSONObject jsonValue) {
    PropertyHoldingValueMirror propertiesMirror = V8Helper.createMirrorFromLookup(jsonValue);
    ValueMirror newMirror = propertiesMirror.getValueMirror();

    ValueMirror oldMirror = refToMirror.putIfAbsent((long)ref, newMirror);
    if (oldMirror != null) {
      mergeMirrors(oldMirror, newMirror);
    }
    return propertiesMirror;
  }

  private static void mergeMirrors(ValueMirror baseMirror, ValueMirror alternativeMirror) {
    baseMirror.mergeFrom(alternativeMirror);
  }
}

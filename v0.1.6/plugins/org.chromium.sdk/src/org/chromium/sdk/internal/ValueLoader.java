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

import org.chromium.sdk.internal.InternalContext.ContextDismissedCheckedException;
import org.chromium.sdk.internal.protocol.ScopeBody;
import org.chromium.sdk.internal.protocol.SuccessCommandResponse;
import org.chromium.sdk.internal.protocol.data.ObjectValueHandle;
import org.chromium.sdk.internal.protocol.data.SomeHandle;
import org.chromium.sdk.internal.protocol.data.ValueHandle;
import org.chromium.sdk.internal.protocolparser.JsonProtocolParseException;
import org.chromium.sdk.internal.tools.v8.V8BlockingCallback;
import org.chromium.sdk.internal.tools.v8.V8Helper;
import org.chromium.sdk.internal.tools.v8.V8ProtocolUtil;
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
   * Looks up data for scope on remote.
   */
  public List<? extends PropertyReference> loadScopeFields(int scopeNumber, int frameNumber) {
    DebuggerMessage message = DebuggerMessageFactory.scope(scopeNumber, frameNumber);

    V8BlockingCallback<List<? extends PropertyReference>> callback =
        new V8BlockingCallback<List<? extends PropertyReference>>() {
      @Override
      protected List<? extends PropertyReference> handleSuccessfulResponse(
          SuccessCommandResponse response) {
        return readFromScopeResponse(response);
      }
    };

    try {
      return V8Helper.callV8Sync(context, message, callback);
    } catch (ContextDismissedCheckedException e) {
      context.getDebugSession().maybeRethrowContextException(e);
      // or
      return Collections.emptyList();
    }
  }

  private List<? extends PropertyReference> readFromScopeResponse(SuccessCommandResponse response) {
    List<SomeHandle> refs = response.getRefs();

    HandleManager handleManager = context.getHandleManager();
    for (int i = 0; i < refs.size(); i++) {
      SomeHandle ref = refs.get(i);
      handleManager.put(ref);
    }
    ScopeBody body;
    try {
      body = response.getBody().asScopeBody();
    } catch (JsonProtocolParseException e) {
      throw new ValueLoadException(e);
    }
    ObjectValueHandle objectRef = body.getObject();
    return V8ProtocolUtil.extractObjectProperties(objectRef);
  }

/**
   * For each PropertyReference from propertyRefs tries to either: 1. read it from PropertyReference
   * (possibly cached value) or 2. lookup value by refId from remote
   */
  public List<ValueMirror> getOrLoadValueFromRefs(List<? extends PropertyReference> propertyRefs) {
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
    Long refIdObject = propertyReference.getRef();

    ValueMirror mirror = refToMirror.get(refIdObject);
    if (mirror != null) {
      return mirror;
    }
    SomeHandle cachedHandle = context.getHandleManager().getHandle(refIdObject);
    // If we have cached handle, we reads cached handle, not using one from propertyeReference
    // because we expect to find more complete version in cache. Is it ok?
    if (cachedHandle != null) {
      ValueHandle valueHandle;
      try {
        valueHandle = cachedHandle.asValueHandle();
      } catch (JsonProtocolParseException e) {
        throw new RuntimeException(e);
      }
      mirror = V8Helper.createValueMirrorOptional(valueHandle);
    } else {
      DataWithRef handleFromProperty = propertyReference.getValueObject();

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

    V8BlockingCallback<List<PropertyHoldingValueMirror>> callback =
        new V8BlockingCallback<List<PropertyHoldingValueMirror>>() {
      @Override
      protected List<PropertyHoldingValueMirror> handleSuccessfulResponse(
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

  private List<PropertyHoldingValueMirror> readResponseFromLookup(
      SuccessCommandResponse successResponse, List<Long> propertyRefIds) {
    List<PropertyHoldingValueMirror> result =
        new ArrayList<PropertyHoldingValueMirror>(propertyRefIds.size());
    JSONObject body;
    try {
      body = successResponse.getBody().asLookupMap();
    } catch (JsonProtocolParseException e) {
      throw new ValueLoadException(e);
    }
    for (int i = 0; i < propertyRefIds.size(); i++) {
      int ref = propertyRefIds.get(i).intValue();
      JSONObject value = JsonUtil.getAsJSON(body, String.valueOf(ref));
      if (value == null) {
        throw new ValueLoadException("Failed to find value for ref=" + ref);
      }
      SomeHandle smthHandle = context.getHandleManager().put((long)ref, value);
      ValueHandle valueHandle;
      try {
        valueHandle = smthHandle.asValueHandle();
      } catch (JsonProtocolParseException e) {
        throw new ValueLoadException(e);
      }

      result.add(readMirrorFromLookup(ref, valueHandle));
    }
    return result;
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
  private PropertyHoldingValueMirror readMirrorFromLookup(int ref, ValueHandle jsonValue) {
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

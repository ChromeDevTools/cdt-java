// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chromium.sdk.JsScope;
import org.chromium.sdk.JsVariable;

/**
 * A generic implementation of the JsScope interface.
 */
public class JsScopeImpl implements JsScope {

  private final CallFrameImpl callFrameImpl;
  private final ScopeMirror mirror;
  private List<JsVariable> properties = null;

  public JsScopeImpl(CallFrameImpl callFrameImpl, ScopeMirror mirror) {
    this.callFrameImpl = callFrameImpl;
    this.mirror = mirror;
  }

  public Type getType() {
    Type type = CODE_TO_TYPE.get(mirror.getType());
    if (type == null) {
      type = Type.UNKNOWN;
    }
    return type;
  }

  public synchronized List<? extends JsVariable> getVariables() {
    if (properties == null) {
      ValueLoader valueLoader = callFrameImpl.getInternalContext().getValueLoader();
      List<? extends PropertyReference> propertyRefs =
          valueLoader.loadScopeFields(mirror.getIndex(), callFrameImpl.getIdentifier());
      List<ValueMirror> propertyMirrors = valueLoader.getOrLoadValueFromRefs(propertyRefs);

      properties = new ArrayList<JsVariable>(propertyMirrors.size());
      for (int i = 0; i < propertyMirrors.size(); i++) {
        properties.add(new JsVariableImpl(callFrameImpl, propertyMirrors.get(i),
            propertyRefs.get(i).getName()));
      }
    }
    return properties;
  }

  private static final Map<Integer, Type> CODE_TO_TYPE;
  static {
    CODE_TO_TYPE = new HashMap<Integer, Type>();
    CODE_TO_TYPE.put(0, Type.GLOBAL);
    CODE_TO_TYPE.put(1, Type.LOCAL);
    CODE_TO_TYPE.put(2, Type.WITH);
    CODE_TO_TYPE.put(3, Type.CLOSURE);
    CODE_TO_TYPE.put(4, Type.CATCH);
  }
}

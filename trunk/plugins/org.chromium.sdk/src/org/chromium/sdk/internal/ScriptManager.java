// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.sdk.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.chromium.sdk.Script;
import org.chromium.sdk.internal.ScriptImpl.Descriptor;
import org.chromium.sdk.internal.tools.v8.V8Protocol;
import org.json.simple.JSONObject;

/**
 * Manages scripts known in the current stack context.
 */
public class ScriptManager {

  public interface Callback {
    /**
     * This method gets invoked for every script in the manager.
     *
     * @param script to process
     * @return whether other scripts should be processed. If false, the #forEach
     *         method terminates.
     */
    boolean process(Script script);
  }

  /**
   * Maps script id's to scripts.
   */
  private final Map<Long, ScriptImpl> idToScript =
      Collections.synchronizedMap(new HashMap<Long, ScriptImpl>());

  /**
   * Adds a script using a "script" V8 response.
   *
   * @param response
   * @return the new script, or null if the response does not contain a script
   *         name
   */
  public Script addScript(JSONObject response) {
    Descriptor desc = Descriptor.forResponse(response);
    if (desc == null) {
      return null;
    }

    ScriptImpl theScript = (ScriptImpl) findScript(desc);

    if (theScript == null) {
      theScript = new ScriptImpl(desc);
      idToScript.put(JsonUtil.getAsLong(response, V8Protocol.ID), theScript);
    }
    if (response.containsKey(V8Protocol.SOURCE_CODE.key)) {
      setSourceCode(response, theScript);
    }

    return theScript;
  }

  /**
   * Tells whether a script specified by the {@code response} is known to this
   * manager.
   *
   * @param response containing the script to check
   * @return whether the script is known to this manager. Will also return
   *         {@code false} if the script name is absent in the {@code response}
   */
  public boolean hasScript(JSONObject response) {
    Descriptor desc = Descriptor.forResponse(response);
    if (desc == null) {
      return false;
    }
    return findScript(desc) != null;
  }

  /**
   * Associates a source received in a "source" V8 response with the given
   * script.
   *
   * @param body the JSON response body
   * @param script the script to associate the source with
   */
  public void setSourceCode(JSONObject body, Script script) {
    String src = JsonUtil.getAsString(body, V8Protocol.SOURCE_CODE);
    if (src == null) {
      return;
    }
    if (script != null) {
      script.setSource(src);
    }
  }

  /**
   * @param name script original document URL
   * @param lineOffset script start line offset in the original document
   * @param lineCount script line count
   * @return the corresponding script, or null if no such script found
   */
  private Script findScript(Descriptor descriptor) {
    return findById(descriptor.id);
  }

  /**
   * @param id of the script to find
   * @return the script with {@code id == ref} or {@code null} if none found
   */
  public Script findById(Long id) {
    return idToScript.get(id);
  }

  /**
   * Determines whether all scripts added into this manager have associated
   * sources.
   *
   * @return whether all known scripts have associated sources
   */
  public boolean isAllSourcesLoaded() {
    final boolean[] result = new boolean[1];
    result[0] = true;
    forEach(new Callback() {
      public boolean process(Script script) {
        if (!script.hasSource()) {
          result[0] = false;
          return false;
        }
        return true;
      }
    });
    return result[0];
  }

  public Collection<Script> allScripts() {
    final Collection<Script> result = new HashSet<Script>();
    forEach(new Callback() {
      public boolean process(Script script) {
        result.add(script);
        return true;
      }
    });
    return result;
  }

  /**
   * This method allows running the same code for all scripts in the manager.
   *
   * @param callback to invoke for every script, until
   *        {@link Callback#process(Script)} returns {@code false}.
   */
  public void forEach(Callback callback) {
    for (Script script : idToScript.values()) {
      if (!callback.process(script)) {
        return;
      }
    }
  }

  public void reset() {
    idToScript.clear();
  }

}

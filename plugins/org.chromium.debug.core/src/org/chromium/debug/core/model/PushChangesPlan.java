// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.core.model;

import java.io.IOException;

import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.sdk.RelayOk;
import org.chromium.sdk.Script;
import org.chromium.sdk.SyncCallback;
import org.chromium.sdk.UpdatableScript;
import org.eclipse.core.runtime.CoreException;

/**
 * Holds parameters of a planned LiveEdit push operation. This object could be used in
 * LiveEdit action or preview implementation.
 * This class is going to become more complicated when we support compound scripts.
 */
public class PushChangesPlan {
  public static PushChangesPlan create(ScriptTargetMapping filePair) {
    // TODO: fix the rough behavior (inside this call).
    Script script = filePair.getSingleScript();

    SourceWrapSupport.Wrapper.Match wrapperMatch;
    if (filePair.isVirtualProjectResource()) {
      wrapperMatch = null;
    } else {
      SourceWrapSupport sourceWrapSupport =
          filePair.getConnectedTargetData().getSourceWrapSupport();

      wrapperMatch = sourceWrapSupport.chooseWrapper(script.getSource());
    }

    byte[] fileData;
    try {
      fileData = ChromiumDebugPluginUtil.readFileContents(filePair.getFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }

    // We are using default charset here as usual.
    String newSource = new String(fileData);

    return new PushChangesPlan(script, filePair, newSource, wrapperMatch);
  }

  private final Script script;
  private final String newSource;
  private final ScriptTargetMapping scriptTargetMapping;
  private final SourceWrapSupport.Wrapper.Match wrapperMatch;

  private PushChangesPlan(Script script, ScriptTargetMapping scriptTargetMapping,
      String newSource, SourceWrapSupport.Wrapper.Match wrapperMatch) {
    this.script = script;
    this.newSource = newSource;
    this.scriptTargetMapping = scriptTargetMapping;
    this.wrapperMatch = wrapperMatch;
  }

  public SourceWrapSupport.Wrapper.Match getSourceWrapperMatch() {
    return wrapperMatch;
  }

  public String getNewSource() {
    return newSource;
  }

  public ScriptTargetMapping getScriptTargetMapping() {
    return scriptTargetMapping;
  }

  public Script getScript() {
    return script;
  }

  public RelayOk execute(boolean previewOnly,
      UpdatableScript.UpdateCallback callback, SyncCallback syncCallback) {
    String wrappedSource;
    if (wrapperMatch == null) {
      wrappedSource = newSource;
    } else {
      wrappedSource = wrapperMatch.wrap(newSource);
    }

    if (previewOnly) {
      return script.previewSetSource(wrappedSource, callback, syncCallback);
    } else {
      return script.setSourceOnRemote(wrappedSource, callback, syncCallback);
    }
  }
}

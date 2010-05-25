// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.actions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.sdk.LiveEditExtension;
import org.chromium.sdk.UpdatableScript;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * The main action of LiveEdit feature. It gets the current state of a working file and pushes
 * it into running V8 VM.
 */
public class PushChangesAction extends V8ScriptAction {
  @Override
  protected void execute(final FilePair filePair) {
    UpdatableScript updatableScript =
        LiveEditExtension.castToUpdatableScript(filePair.getVmResource().getScript());

    if (updatableScript == null) {
      throw new RuntimeException();
    }

    byte[] fileData;
    try {
      fileData = readFileContents(filePair.getFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (CoreException e) {
      throw new RuntimeException(e);
    }

    // We are using default charset here like usually.
    String newSource = new String(fileData);

    UpdatableScript.UpdateCallback callback = new UpdatableScript.UpdateCallback() {
      public void success(Object report) {
        ChromiumDebugPlugin.log(new Status(IStatus.OK, ChromiumDebugPlugin.PLUGIN_ID,
            "Script has been successfully updated on remote: " + report)); //$NON-NLS-1$
      }
      public void failure(String message) {
        ChromiumDebugPlugin.log(new Status(IStatus.ERROR, ChromiumDebugPlugin.PLUGIN_ID,
            "Failed to change script on remote: " + message)); //$NON-NLS-1$
      }
    };

    updatableScript.setSourceOnRemote(newSource, callback, null);
  }


  private static byte[] readFileContents(IFile file) throws IOException, CoreException {
    InputStream inputStream = file.getContents();
    try {
      return readBytes(inputStream);
    } finally {
      inputStream.close();
    }
  }

  private static byte[] readBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] array = new byte[1024];
    while (true) {
      int len = inputStream.read(array);
      if (len == -1) {
        break;
      }
      buffer.write(array, 0, len);
    }
    return buffer.toByteArray();
  }
}

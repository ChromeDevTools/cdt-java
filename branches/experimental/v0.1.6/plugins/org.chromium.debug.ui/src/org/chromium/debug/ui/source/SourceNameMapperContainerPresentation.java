// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.source;

import org.chromium.debug.core.SourceNameMapperContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser;
import org.eclipse.swt.widgets.Shell;

/**
 * A presentation for JavaScript Source Name Mapper container that supports adding and editing.
 */
public class SourceNameMapperContainerPresentation implements ISourceContainerBrowser {

  public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
    return openDialog(shell, director, null);
  }

  public boolean canAddSourceContainers(ISourceLookupDirector director) {
    return true;
  }

  public boolean canEditSourceContainers(ISourceLookupDirector director,
      ISourceContainer[] containers) {
    return containers.length == 1;
  }

  public ISourceContainer[] editSourceContainers(Shell shell, ISourceLookupDirector director,
      ISourceContainer[] containers) {
    final SourceNameMapperContainer originalContainer = (SourceNameMapperContainer) containers[0];
    SourceNameMapperContainerDialog.PresetFieldValues params =
        new SourceNameMapperContainerDialog.PresetFieldValues() {
      public ISourceContainer getContainer() {
        return originalContainer.getTargetContainer();
      }
      public String getPrefix() {
        return originalContainer.getPrefix();
      }
    };

    return openDialog(shell, director, params);
  }

  private ISourceContainer[] openDialog(Shell shell, ISourceLookupDirector director,
      SourceNameMapperContainerDialog.PresetFieldValues params) {
    SourceNameMapperContainerDialog dialog =
        new SourceNameMapperContainerDialog(shell, director, params);
    dialog.open();
    SourceNameMapperContainerDialog.Result dialogResult = dialog.getResult();
    if (dialogResult == null) {
      return new ISourceContainer[0];
    }
    ISourceContainer result = new SourceNameMapperContainer(dialogResult.getResultPrefix(),
        dialogResult.getResultContainer());
    return new ISourceContainer[] { result };
  }
}

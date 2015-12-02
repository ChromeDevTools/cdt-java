// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.actions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.model.JavaScriptFormatter;
import org.chromium.debug.core.model.StringMappingData;
import org.chromium.debug.core.model.ConnectedTargetData;
import org.chromium.debug.core.model.VmResource;
import org.chromium.debug.core.model.WorkspaceBridge;
import org.chromium.debug.core.sourcemap.SourcePositionMapBuilder;
import org.chromium.debug.core.sourcemap.TextSectionMapping;
import org.chromium.debug.core.sourcemap.TextSectionMappingImpl;
import org.chromium.debug.core.util.ChromiumDebugPluginUtil;
import org.chromium.debug.ui.JsDebugModelPresentation;
import org.chromium.debug.ui.actions.FileBasedAction.FileFilter;
import org.chromium.debug.ui.editors.JsEditor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;

/**
 * An action delegate for "Temporarily format source". It formats downloaded JavaScript source,
 * i.e. a file under virtual project and puts the result into another file under virtual project.
 * It also registers a mapping between these 2 files.
 * <p>Additionally the action may work in another mode: "Delete formatted source". This mode
 * gets enabled when user selection contains files created in "Temporarily format source".
 */
public class TemporarilyFormatSourceAction
    extends FileBasedAction.Multiple<TemporarilyFormatSourceAction.ResourceData> {
  public TemporarilyFormatSourceAction() {
    super(RESOURCE_FILTER);
  }

  private static final FileFilter<ResourceData> RESOURCE_FILTER =
      new FileFilter<ResourceData>() {
    @Override
    ResourceData accept(IFile file) {
      for (ConnectedTargetData targetData : DebugTargetImpl.getAllConnectedTargetDatas()) {
        VmResource vmResource = targetData.getWorkspaceRelations().getVProjectVmResource(file);
        if (vmResource == null) {
          continue;
        }
        return new ResourceData(file, vmResource, targetData);
      }
      return null;
    }
  };

  @Override
  protected ActionRunnable createRunnable(
      final List<? extends ResourceData> resourceDataList) {

    // Choose between two modes of the action: actually format or delete already formatted file
    // (i.e. undo one of previous actions).
    Iterator<? extends ResourceData> iterator = resourceDataList.iterator();
    if (!iterator.hasNext()) {
      return null;
    }

    ResourceData firstElement = iterator.next();
    ActionMode firstElementMode = checkActionMode(firstElement);
    if (firstElementMode == ActionMode.UNAPPLICABLE) {
      return null;
    }
    while (iterator.hasNext()) {
      ResourceData otherElement = iterator.next();
      ActionMode otherElementMode = checkActionMode(otherElement);
      if (otherElementMode == ActionMode.UNAPPLICABLE) {
        return null;
      }
      if (otherElementMode != firstElementMode) {
        return null;
      }
    }

    if (firstElementMode == ActionMode.FORMAT) {
      JavaScriptFormatter formatter = JavaScriptFormatter.Access.getInstance();
      if (formatter == null) {
        return noFormatterActionStub;
      } else {
        return new FormatActionRunnable(resourceDataList, formatter);
      }
    } else {
      return new DeleteFormattedActionRunnable(resourceDataList);
    }
  }

  private abstract class ActionRunnableImpl implements ActionRunnable {
    private final List<? extends ResourceData> resourceDataList;

    ActionRunnableImpl(List<? extends ResourceData> resourceDataList) {
      this.resourceDataList = resourceDataList;
    }

    public void run(final Shell shell, final IWorkbenchPart workbenchPart) {
      Job job = new Job(getAction().getText()) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          for (int i = 0; i < resourceDataList.size(); i++) {
            ResourceData resourceData = resourceDataList.get(i);
            try {
              runInWorkerThread(resourceData, shell, workbenchPart, i == 0);
            } catch (CoreException e) {
              throw new RuntimeException(e);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
          return Status.OK_STATUS;
        }
      };
      job.schedule();
    }

    protected abstract void runInWorkerThread(ResourceData resourceData, Shell shell,
        IWorkbenchPart workbenchPart, boolean highlightResult) throws CoreException, IOException;
  }

  private class FormatActionRunnable extends ActionRunnableImpl {
    private final JavaScriptFormatter formatter;

    FormatActionRunnable(List<? extends ResourceData> resourceDataList,
        JavaScriptFormatter formatter) {
      super(resourceDataList);
      this.formatter = formatter;
    }

    public void adjustAction() {
      restoreActionText();
    }

    @Override
    protected void runInWorkerThread(ResourceData data, Shell shell, IWorkbenchPart workbenchPart,
        boolean highlightResult) throws CoreException, IOException {
      byte[] sourceBytes = ChromiumDebugPluginUtil.readFileContents(data.getFile());
      String sourceString = new String(sourceBytes);

      JavaScriptFormatter.Result result = formatter.format(sourceString);

      WorkspaceBridge workspaceRelations = data.getConnectedTargetData().getWorkspaceRelations();

      String proposedFileName = data.getVmResource().getLocalVisibleFileName() +
          Messages.TemporarilyFormatSourceAction_FORMATTER_SUFFIX;

      MetadataImpl metadata = new MetadataImpl();
      VmResource formattedResource =
          workspaceRelations.createTemporaryFile(metadata, proposedFileName);

      SourcePositionMapBuilder.MappingHandle mappingHandle;
      try {
        SourcePositionMapBuilder builder =
            data.getConnectedTargetData().getSourcePositionMapBuilder();

        // Unformatted text is a VM text.
        StringMappingData vmTextData = result.getInputTextData();

        // Formatter text is *like* original text in our case.
        StringMappingData originalTextData = result.getFormattedTextData();

        SourcePositionMapBuilder.ResourceSection vmResourceSection =
            new SourcePositionMapBuilder.ResourceSection(data.getVmResource().getId(), 0, 0,
                vmTextData.getEndLine(), vmTextData.getEndColumn());

        SourcePositionMapBuilder.ResourceSection originalResourceSection =
            new SourcePositionMapBuilder.ResourceSection(formattedResource.getId(), 0, 0,
                originalTextData.getEndLine(), originalTextData.getEndColumn());

        TextSectionMapping mapTable =
            new TextSectionMappingImpl(originalTextData, vmTextData);

        mappingHandle = builder.addMapping(originalResourceSection, vmResourceSection, mapTable);
      } catch (SourcePositionMapBuilder.CannotAddException e) {
        formattedResource.deleteResourceAndFile();
        throw new RuntimeException(e);
      }
      metadata.mappingHandle = mappingHandle;

      ChromiumDebugPluginUtil.writeFile(formattedResource.getVProjectFile(),
          result.getFormattedText());

      if (highlightResult) {
        highlightResult(formattedResource, workbenchPart, shell);
      }
    }

    private void highlightResult(final VmResource formattedResource,
        final IWorkbenchPart workbenchPart, Shell shell) {
      shell.getDisplay().asyncExec(new Runnable() {
        public void run() {
          if (workbenchPart instanceof IShowInTarget) {
            IShowInTarget showInTarget = (IShowInTarget) workbenchPart;
            ShowInContext showInContext =
                new ShowInContext(formattedResource.getVProjectFile(), null);
            showInTarget.show(showInContext);
          } else {
            openFileInEditorAsync(formattedResource, workbenchPart.getSite().getWorkbenchWindow());
          }
        }
      });
    }

    private void openFileInEditorAsync(VmResource formattedResource,
        IWorkbenchWindow workbenchWindow) {
      IEditorInput input =
          JsDebugModelPresentation.toEditorInput(formattedResource.getVProjectFile());
      try {
        workbenchWindow.getActivePage().openEditor(input, JsEditor.EDITOR_ID);
      } catch (PartInitException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class DeleteFormattedActionRunnable extends ActionRunnableImpl {
    DeleteFormattedActionRunnable(List<? extends ResourceData> resourceDataList) {
      super(resourceDataList);
    }

    public void adjustAction() {
      modifyActionText(Messages.TemporarilyFormatSourceAction_DELETE_FORMATTER_ACTION_NAME);
    }

    @Override
    protected void runInWorkerThread(ResourceData resourceData, Shell shell,
        IWorkbenchPart workbenchPart, boolean highlightResult)
        throws CoreException, IOException {
      MetadataImpl metadataImpl = (MetadataImpl) resourceData.getVmResource().getMetadata();
      metadataImpl.mappingHandle.delete();
      resourceData.getVmResource().deleteResourceAndFile();
    }
  }

  private final ActionRunnable noFormatterActionStub = new ActionRunnable() {
    public void adjustAction() {
      restoreActionText();
      String baseText = getAction().getText();
      modifyActionText(baseText +
          Messages.TemporarilyFormatSourceAction_NO_FORMATTER_DISABLED_SUFFIX);
      getAction().setEnabled(false);
    }

    public void run(Shell shell, IWorkbenchPart workbenchPart) {
      // Should be unreachable. Do nothing.
    }
  };

  /**
   * Depending on user selection, the action may work in 2 modes: format or
   * delete formatted.
   */
  private enum ActionMode {
    FORMAT, DELETE_FORMATTED, UNAPPLICABLE
  }

  private ActionMode checkActionMode(ResourceData data) {
    Object metadata = data.getVmResource().getMetadata();
    if (metadata instanceof VmResource.ScriptHolder) {
      return ActionMode.FORMAT;
    }
    if (metadata instanceof MetadataImpl) {
      return ActionMode.DELETE_FORMATTED;
    }
    return ActionMode.UNAPPLICABLE;
  }

  static class ResourceData {
    private final IFile file;
    private final VmResource vmResource;
    private final ConnectedTargetData connectedTargetData;

    ResourceData(IFile file, VmResource vmResource, ConnectedTargetData connectedTargetData) {
      this.file = file;
      this.vmResource = vmResource;
      this.connectedTargetData = connectedTargetData;
    }

    IFile getFile() {
      return file;
    }

    public VmResource getVmResource() {
      return vmResource;
    }

    public ConnectedTargetData getConnectedTargetData() {
      return connectedTargetData;
    }
  }

  /**
   * Keeps the data that helps us undo formatting later.
   */
  private static class MetadataImpl implements VmResource.Metadata {
    SourcePositionMapBuilder.MappingHandle mappingHandle;
  }
}

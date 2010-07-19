// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;

/**
 * A UI control that shows V8 update preview. It consists of function tree structure and source
 * text viewer each for both old and new version of the script. The tree viewers are
 * synchronized together: their selection, expansion and scroll position are synchronized.
 */
public class LiveEditDiffViewer {
  public static LiveEditDiffViewer create(Composite parent, Configuration configuration) {
    return new LiveEditDiffViewer(parent, configuration);
  }

  /**
   * A static parameters for the viewer. They should not change.
   */
  public interface Configuration {
    String getOldLabel();
    String getNewLabel();
    boolean oldOnLeft();
  }

  /**
   * An input for the viewer.
   */
  public interface Input {
    /**
     * The root of JavaScript function tree. The tree combines functions from old and new versions
     * of the script.
     * @return
     */
    FunctionNode getRootFunction();
    SourceText getOldSource();
    SourceText getNewSource();
  }

  public interface SourceText {
    String getText();
    String getTitle();
  }

  /**
   * A function in old and/or new version of the script.
   */
  public interface FunctionNode {
    String getName();
    String getStatus();
    List<? extends FunctionNode> children();
    /**
     * @return positions inside a particular version of the script, or null if function does not
     *   linked to this version
     */
    SourcePosition getPosition(Side side);

    FunctionNode getParent();
  }

  /**
   * A version of the script.
   */
  public enum Side {
    OLD, NEW
  }

  public interface SourcePosition {
    int getStart();
    int getEnd();
  }

  private final Composite mainControl;
  private final SideControls oldSideView;
  private final SideControls newSideView;
  private final TreeLinkMonitor linkMonitor;
  private final Text functionStatusText;

  private LiveEditDiffViewer(Composite parent, Configuration configuration) {
    Composite composite = new Composite(parent, SWT.NONE);
    {
      composite.setLayoutData(new GridData(GridData.FILL_BOTH));
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 1;
      composite.setLayout(topLayout);
    }

    Composite labelPairComposite = new Composite(composite, SWT.NONE);
    {
      labelPairComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      FillLayout fillLayout = new FillLayout();
      fillLayout.type = SWT.HORIZONTAL;
      fillLayout.spacing = 5;
      labelPairComposite.setLayout(fillLayout);
    }
    Label labelLeft = new Label(labelPairComposite, SWT.NONE);
    Label labelRight = new Label(labelPairComposite, SWT.NONE);

    Composite fourCells = new Composite(composite, SWT.NONE);
    {
      GridData gd = new GridData(GridData.FILL_BOTH);
      // TODO(peter.rybin): fix magic number.
      gd.heightHint = 500;
      gd.widthHint = 600;
      fourCells.setLayoutData(gd);
      FillLayout fillLayout = new FillLayout();
      fillLayout.type = SWT.VERTICAL;
      fillLayout.spacing = 5;
      fourCells.setLayout(fillLayout);
    }

    Composite treePairComposite = new Composite(fourCells, SWT.NONE);
    {
      FillLayout fillLayout = new FillLayout();
      fillLayout.type = SWT.HORIZONTAL;
      fillLayout.spacing = 5;
      treePairComposite.setLayout(fillLayout);
    }

    linkMonitor = new TreeLinkMonitor();

    TreeViewer treeViewerLeft = new TreeViewer(treePairComposite);
    TreeViewer treeViewerRight = new TreeViewer(treePairComposite);

    Composite sourcePairComposite = new Composite(fourCells, SWT.NONE);
    {
      FillLayout fillLayout = new FillLayout();
      fillLayout.type = SWT.HORIZONTAL;
      fillLayout.spacing = 5;
      sourcePairComposite.setLayout(fillLayout);
    }
    SourceViewer sourceViewerLeft =
        new SourceViewer(sourcePairComposite, null, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    sourceViewerLeft.getTextWidget().setEditable(false);
    SourceViewer sourceViewerRight =
        new SourceViewer(sourcePairComposite, null, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    sourceViewerRight.getTextWidget().setEditable(false);

    functionStatusText = createTextField(composite);
    {
      GridData gd = new GridData(GridData.FILL_BOTH);
      // TODO(peter.rybin): fix magic number.
      gd.heightHint = 90;
      gd.grabExcessHorizontalSpace = true;
      gd.horizontalAlignment = GridData.FILL;
      functionStatusText.setLayoutData(gd);
    }

    SideControls sideViewLeft = new SideControls(labelLeft, treeViewerLeft, sourceViewerLeft);
    SideControls sideViewRight = new SideControls(labelRight, treeViewerRight, sourceViewerRight);

    if (configuration.oldOnLeft()) {
      oldSideView = sideViewLeft;
      newSideView = sideViewRight;
    } else {
      oldSideView = sideViewRight;
      newSideView = sideViewLeft;
    }

    oldSideView.label.setText(configuration.getOldLabel());
    newSideView.label.setText(configuration.getNewLabel());

    configureTreeViewer(oldSideView.treeViewer, newSideView.treeViewer, Side.OLD);
    configureTreeViewer(newSideView.treeViewer, oldSideView.treeViewer, Side.NEW);

    mainControl = composite;
  }

  private void configureTreeViewer(TreeViewer treeViewer, TreeViewer opposite, Side side) {
    treeViewer.setContentProvider(new FunctionTreeContentProvider());
    treeViewer.setLabelProvider(new LabelProviderImpl(side));
    treeViewer.addSelectionChangedListener(new SelectionChangeListener(opposite));
    treeViewer.addTreeListener(new TreeListenerImpl(opposite));
    treeViewer.getTree().getVerticalBar().addListener(SWT.Selection,
        new ScrollBarListener(opposite));
  }

  private static class SideControls {
    final Label label;
    final TreeViewer treeViewer;
    final SourceViewer sourceViewer;

    SideControls(Label label, TreeViewer treeViewer, SourceViewer sourceViewer) {
      this.label = label;
      this.treeViewer = treeViewer;
      this.sourceViewer = sourceViewer;
    }
  }

  private static Text createTextField(Composite parent) {
    Text valueText = new Text(parent, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
    Display display = parent.getDisplay();
    valueText.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    return valueText;
  }

  public Control getControl() {
    return mainControl;
  }

  public void setInput(Input input) {
    linkMonitor.block();
    try {
      oldSideView.treeViewer.setInput(input);
      newSideView.treeViewer.setInput(input);
      oldSideView.treeViewer.expandAll();
      newSideView.treeViewer.expandAll();

      Document oldDocument;
      if (input == null) {
        oldDocument = null;
      } else {
        oldDocument = new Document(input.getOldSource().getText());
      }
      oldSideView.sourceViewer.setDocument(oldDocument);
      Document newDocument;
      if (input == null) {
        newDocument = null;
      } else {
        newDocument = new Document(input.getNewSource().getText());
      }
      newSideView.sourceViewer.setDocument(newDocument);
    } finally {
      linkMonitor.unblock();
    }
    setSelectedFunction(null);
  }

  private static class FunctionTreeContentProvider implements ITreeContentProvider {
    public Object[] getChildren(Object parentElement) {
      FunctionNode functionNode = (FunctionNode) parentElement;
      return functionNode.children().toArray();
    }

    public Object getParent(Object element) {
      FunctionNode functionNode = (FunctionNode) element;
      return functionNode.getParent();
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length != 0;
    }

    public Object[] getElements(Object inputElement) {
      Input input = (Input) inputElement;
      if (input == null) {
        return new Object[] { };
      } else {
        return new Object[] { input.getRootFunction() };
      }
    }

    public void dispose() {
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
  }

  private static class LabelProviderImpl implements ILabelProvider {
    private final Side side;

    LabelProviderImpl(Side side) {
      this.side = side;
    }

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      FunctionNode functionNode = (FunctionNode) element;
      SourcePosition position = functionNode.getPosition(side);
      if (position == null) {
        return "."; //$NON-NLS-1$
      } else {
        if (functionNode.getParent() == null) {
          return Messages.LiveEditDiffViewer_SCRIPT;
        } else {
          String name = functionNode.getName();
          if (name == null) {
            return Messages.LiveEditDiffViewer_UNNAMED;
          } else {
            return name;
          }
        }
      }
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void dispose() {
    }
  }

  private class SelectionChangeListener implements ISelectionChangedListener {
    private final TreeViewer oppositeViewer;

    SelectionChangeListener(TreeViewer oppositeViewer) {
      this.oppositeViewer = oppositeViewer;
    }

    public void selectionChanged(SelectionChangedEvent event) {
      if (linkMonitor.isBlocked()) {
        return;
      }
      linkMonitor.block();
      try {
        ISelection selection = event.getSelection();
        oppositeViewer.setSelection(selection);
        updateFunctionSelection(selection);
      } finally {
        linkMonitor.unblock();
      }
    }
  }

  private class ScrollBarListener implements Listener {
    private final TreeViewer oppositeViewer;

    ScrollBarListener(TreeViewer oppositeViewer) {
      this.oppositeViewer = oppositeViewer;
    }

    public void handleEvent(Event e) {
      if (linkMonitor.isBlocked()) {
        return;
      }
      linkMonitor.block();
      try {
        int vpos = ((ScrollBar)e.widget).getSelection();
        oppositeViewer.getTree().getVerticalBar().setSelection(vpos);
      } finally {
        linkMonitor.unblock();
      }
    }
  }

  private void updateFunctionSelection(ISelection selection) {
    FunctionNode functionNode = null;
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      if (structuredSelection.size() == 1) {
        Object element = structuredSelection.getFirstElement();
        functionNode = (FunctionNode) element;
      }
    }
    setSelectedFunction(functionNode);
  }
  private void setSelectedFunction(FunctionNode functionNode) {
    String text;
    if (functionNode == null) {
      text = ""; //$NON-NLS-1$
    } else {
      text = functionNode.getStatus();
      highlightCode(functionNode, Side.OLD, oldSideView.sourceViewer);
      highlightCode(functionNode, Side.NEW, newSideView.sourceViewer);
    }
    functionStatusText.setText(text);
  }
  private void highlightCode(FunctionNode node, Side side, SourceViewer sourceViewer) {
    SourcePosition position = node.getPosition(side);
    if (position == null) {
      Point oldSelection = sourceViewer.getSelectedRange();
      sourceViewer.setSelectedRange(oldSelection.x, 0);
    } else {
      sourceViewer.setSelectedRange(position.getStart(), position.getEnd() - position.getStart());
      sourceViewer.revealRange(position.getStart(), position.getEnd() - position.getStart());
    }
  }

  private class TreeListenerImpl implements ITreeViewerListener {
    private final TreeViewer oppositeViewer;

    TreeListenerImpl(TreeViewer oppositeViewer) {
      this.oppositeViewer = oppositeViewer;
    }

    public void treeExpanded(TreeExpansionEvent event) {
      if (linkMonitor.isBlocked()) {
        return;
      }
      linkMonitor.block();
      try {
        oppositeViewer.expandToLevel(event.getElement(), 1);
      } finally {
        linkMonitor.unblock();
      }
    }

    public void treeCollapsed(TreeExpansionEvent event) {
      if (linkMonitor.isBlocked()) {
        return;
      }
      linkMonitor.block();
      try {
        oppositeViewer.collapseToLevel(event.getElement(), 1);
      } finally {
        linkMonitor.unblock();
      }
    }
  }

  /**
   * A monitor that helps in cross-tree synchronizations. Changes in one tree are propagated to
   * the other one, but this monitor helps block a recursive propagation.
   */
  private static class TreeLinkMonitor {
    private boolean blocked = false;
    private final Thread accessThread = Thread.currentThread();
    void block() {
      assert accessThread == Thread.currentThread();
      if (blocked) {
        throw new IllegalStateException();
      }
      blocked = true;
    }
    void unblock() {
      blocked = false;
    }
    boolean isBlocked() {
      return blocked;
    }
  }
}

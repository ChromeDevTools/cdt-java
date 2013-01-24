// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.liveedit;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.util.RangeBinarySearch;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.sdk.UpdatableScript;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextPresentation;
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
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
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
    UpdatableScript.TextualDiff getTextualDiff();
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
  private final Colors colors;
  private InputData currentInput = null;

  private LiveEditDiffViewer(Composite parent, Configuration configuration) {
    colors = new Colors(parent.getDisplay());

    FontMetrics defaultFontMetrics = PluginUtil.getFontMetrics(parent, null);

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
      gd.heightHint = defaultFontMetrics.getHeight() * 30;
      gd.widthHint = defaultFontMetrics.getAverageCharWidth() * 85;
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

    {
      functionStatusText = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
      Display display = composite.getDisplay();
      functionStatusText.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

      GridData gd = new GridData(GridData.FILL_BOTH);
      gd.minimumHeight = defaultFontMetrics.getHeight() * 3;
      gd.heightHint = gd.minimumHeight;
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

    configureSide(oldSideView, newSideView, Side.OLD);
    configureSide(newSideView, oldSideView, Side.NEW);

    mainControl = composite;
    mainControl.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent event) {
          handleDispose(event);
      }
    });
  }

  private void configureSide(SideControls sideControls, SideControls opposite, Side side) {
    configureTreeViewer(sideControls.treeViewer, opposite.treeViewer, side);
    configureSourceViewer(sideControls.sourceViewer, opposite.sourceViewer, side);
  }

  private void configureTreeViewer(TreeViewer treeViewer, TreeViewer opposite, Side side) {
    treeViewer.setContentProvider(new FunctionTreeContentProvider());
    treeViewer.setLabelProvider(new LabelProviderImpl(side));
    treeViewer.addSelectionChangedListener(new SelectionChangeListener(opposite));
    treeViewer.addTreeListener(new TreeListenerImpl(opposite));
    treeViewer.getTree().getVerticalBar().addListener(SWT.Selection,
        new TreeScrollBarListener(opposite));
  }

  private void configureSourceViewer(SourceViewer sourceViewer, SourceViewer opposite, Side side) {
    sourceViewer.getTextWidget().getVerticalBar().addListener(SWT.Selection,
        new SourceScrollBarListener(sourceViewer, opposite, side));

    sourceViewer.getTextWidget().addLineBackgroundListener(new LineBackgroundListenerImpl(side));
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

  private void handleDispose(DisposeEvent event) {
    colors.dispose();
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
      if (input != null) {
        applyDiffPresentation(oldSideView.sourceViewer, newSideView.sourceViewer,
            input.getTextualDiff());
      }
    } finally {
      linkMonitor.unblock();
    }

    currentInput = buildInputData(input);

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
        String name = functionNode.getName();
        if (name == null || name.trim().length() == 0) {
          return Messages.LiveEditDiffViewer_UNNAMED;
        } else {
          return name;
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

  private abstract class ScrollListenerBase implements Listener {
    public void handleEvent(Event e) {
      if (linkMonitor.isBlocked()) {
        return;
      }
      linkMonitor.block();
      try {
        handleScroll((ScrollBar)e.widget);
      } finally {
        linkMonitor.unblock();
      }
    }
    protected abstract void handleScroll(ScrollBar scrollBar);
  }

  private class TreeScrollBarListener extends ScrollListenerBase {
    private final TreeViewer oppositeViewer;

    TreeScrollBarListener(TreeViewer oppositeViewer) {
      this.oppositeViewer = oppositeViewer;
    }

    @Override
    protected void handleScroll(ScrollBar scrollBar) {
      int vpos = scrollBar.getSelection();
      oppositeViewer.getTree().getVerticalBar().setSelection(vpos);
    }
  }

  private class SourceScrollBarListener extends ScrollListenerBase {
    private final SourceViewer sourceViewer;
    private final SourceViewer opposite;
    private final Side side;

    SourceScrollBarListener(SourceViewer sourceViewer, SourceViewer opposite, Side side) {
      this.sourceViewer = sourceViewer;
      this.opposite = opposite;
      this.side = side;
    }

    @Override
    protected void handleScroll(ScrollBar scrollBar) {
      if (currentInput == null) {
        return;
      }
      int topPos = sourceViewer.getTopIndex();
      int bottomPos = sourceViewer.getBottomIndex();
      TextChangesMap changesMap = currentInput.getMap(side);
      int neededOppositeTopPos = changesMap.translateLineNumber(topPos, true);
      int neededOppositeBottomPos = changesMap.translateLineNumber(bottomPos, false);

      int actualOppositeTopPos = opposite.getTopIndex();
      int actualOppositeBottomPos = opposite.getBottomIndex();

      int topFreeSpace = actualOppositeTopPos - neededOppositeTopPos;
      int bottomFreeSpace = neededOppositeBottomPos - actualOppositeBottomPos;

      if (topFreeSpace > 0 && bottomFreeSpace < 0) {
        // Move up.
        int moveUpValue = Math.min(topFreeSpace, -bottomFreeSpace);
        opposite.setTopIndex(actualOppositeTopPos - moveUpValue);
      } else if (topFreeSpace < 0 && bottomFreeSpace > 0) {
        // Move down.
        int moveDownValue = Math.min(-topFreeSpace, bottomFreeSpace);
        opposite.setTopIndex(actualOppositeTopPos + moveDownValue);
      }
    }
  }

  private static InputData buildInputData(Input input) {
    if (input == null) {
      return null;
    }
    List<Long> chunkArray = input.getTextualDiff().getChunks();

    String oldText = input.getOldSource().getText();
    String newText = input.getNewSource().getText();

    int arrayLengthExpected = chunkArray.size() / 3;
    List<ChunkData> oldLineNumbers = new ArrayList<ChunkData>(arrayLengthExpected);
    List<ChunkData> newLineNumbers = new ArrayList<ChunkData>(arrayLengthExpected);

    {
      int oldPos = 0;
      int currentOldLineNumber = 0;
      int newPos = 0;
      int currentNewLineNumber = 0;

      for (int i = 0; i < chunkArray.size(); i += 3) {
        int oldStart = chunkArray.get(i + 0).intValue();
        int newStart = oldStart - oldPos + newPos;
        int oldEnd = chunkArray.get(i + 1).intValue();
        int newEnd = chunkArray.get(i + 2).intValue();

        currentOldLineNumber += countLineEnds(oldText, oldPos, oldStart);
        currentNewLineNumber += countLineEnds(newText, newPos, newStart);

        int oldLineStart = currentOldLineNumber;
        int newLineStart = currentNewLineNumber;

        currentOldLineNumber += countLineEnds(oldText, oldStart, oldEnd);
        currentNewLineNumber += countLineEnds(newText, newStart, newEnd);

        oldLineNumbers.add(new ChunkData(oldLineStart, currentOldLineNumber, oldStart, oldEnd));
        newLineNumbers.add(new ChunkData(newLineStart, currentNewLineNumber, newStart, newEnd));

        oldPos = oldEnd;
        newPos = newEnd;
      }
    }

    return new InputData(new TextChangesMap(oldLineNumbers, newLineNumbers),
        new TextChangesMap(newLineNumbers, oldLineNumbers));
  }

  private static int countLineEnds(String str, int start, int end) {
    int result = 0;
    for (int i = start; i < end; i++) {
      if (str.charAt(i) == '\n') {
        result++;
      }
    }
    return result;
  }

  private void applyDiffPresentation(SourceViewer oldViewer, SourceViewer newViewer,
      UpdatableScript.TextualDiff textualDiff) {
    TextPresentation oldPresentation = new TextPresentation();
    TextPresentation newPresentation = new TextPresentation();

    List<Long> chunkNumbers = textualDiff.getChunks();
    int posOld = 0;
    int posNew = 0;
    for (int i = 0; i < chunkNumbers.size(); i += 3) {
      int startOld = chunkNumbers.get(i + 0).intValue();
      int endOld = chunkNumbers.get(i + 1).intValue();
      int endNew = chunkNumbers.get(i + 2).intValue();
      int startNew = startOld - posOld + posNew;

      if (startOld == endOld) {
        // Add
        newPresentation.addStyleRange(new StyleRange(startNew, endNew - startNew,
            null, colors.get(ColorName.ADDED_BACKGROUND)));
      } else if (startNew == endNew) {
        // Remove
        oldPresentation.addStyleRange(new StyleRange(startOld, endOld - startOld,
            null, colors.get(ColorName.ADDED_BACKGROUND)));
      } else {
        // Replace
        newPresentation.addStyleRange(new StyleRange(startNew, endNew - startNew,
            null, colors.get(ColorName.CHANGED_BACKGROUND)));
        oldPresentation.addStyleRange(new StyleRange(startOld, endOld - startOld,
            null, colors.get(ColorName.CHANGED_BACKGROUND)));
      }

      posOld = endOld;
      posNew = endNew;
    }

    oldViewer.changeTextPresentation(oldPresentation, true);
    newViewer.changeTextPresentation(newPresentation, true);
  }

  private class LineBackgroundListenerImpl implements LineBackgroundListener {
    private final Side side;

    LineBackgroundListenerImpl(Side side) {
      this.side = side;
    }

    @Override
    public void lineGetBackground(LineBackgroundEvent event) {
      if (currentInput == null) {
        return;
      }
      TextChangesMap changesMap = currentInput.getMap(side);
      ColorName colorName =
          changesMap.getLineColorName(event.lineOffset, event.lineText.length() + 1);
      if (colorName != null) {
        event.lineBackground = colors.get(colorName);
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

  private static class InputData {
    private final Map<Side, TextChangesMap> sideToMap;

    InputData(TextChangesMap oldSideMap, TextChangesMap newSideMap) {
      this.sideToMap = new EnumMap<Side, TextChangesMap>(Side.class);
      sideToMap.put(Side.OLD, oldSideMap);
      sideToMap.put(Side.NEW, newSideMap);
    }

    TextChangesMap getMap(Side side) {
      return sideToMap.get(side);
    }
  }

  private static class TextChangesMap {
    private final List<ChunkData> sourceChunks;
    private final List<ChunkData> targetChunks;

    TextChangesMap(List<ChunkData> sourceChunks, List<ChunkData> targetChunks) {
      this.sourceChunks = sourceChunks;
      this.targetChunks = targetChunks;
    }

    public ColorName getLineColorName(int lineStartOffset, int lineLen) {
      if (isChangedLine(lineStartOffset, lineLen)) {
        return ColorName.CHANGED_LINE_BACKGROUND;
      } else {
        return null;
      }
    }

    private boolean isChangedLine(final int lineStartOffset, int lineLen) {
      RangeBinarySearch.Input searchInput = new RangeBinarySearch.Input() {
        @Override public int pinPointsNumber() {
          return sourceChunks.size();
        }

        @Override public boolean isPointXLessThanPinPoint(int pinPointIndex) {
          return lineStartOffset <= sourceChunks.get(pinPointIndex).endPosition;
        }
      };
      int chunkIndex = RangeBinarySearch.find(searchInput);
      if (chunkIndex == sourceChunks.size()) {
        return false;
      }
      return lineStartOffset + lineLen > sourceChunks.get(chunkIndex).startPosition;
    }

    int translateLineNumber(final int lineNumber, final boolean preferAboveNotBelow) {
      // Represents chunk starts and chunk ends as one list of pin-points.
      RangeBinarySearch.Input searchInput = new RangeBinarySearch.Input() {
        @Override public int pinPointsNumber() {
          return sourceChunks.size() * 2;
        }

        @Override public boolean isPointXLessThanPinPoint(int pinPointIndex) {
          int chunkIndex = pinPointIndex / 2;
          int number;
          if (pinPointIndex % 2 == 0) {
            number = sourceChunks.get(chunkIndex).startLineNumber;
          } else {
            number = sourceChunks.get(chunkIndex).endLineNumber;
          }
          return preferAboveNotBelow ? lineNumber <= number : lineNumber < number;
        }
      };

      int pointIndex = RangeBinarySearch.find(searchInput);
      int chunkIndex = pointIndex / 2;
      if (pointIndex % 2 == 0) {
        // Unmodified part of source.
        int diff;
        if (chunkIndex == 0) {
          diff = 0;
        } else {
          diff = targetChunks.get(chunkIndex - 1).endLineNumber -
            sourceChunks.get(chunkIndex - 1).endLineNumber;
        }
        return lineNumber + diff;
      } else {
        if (preferAboveNotBelow) {
          return targetChunks.get(chunkIndex).startLineNumber;
        } else {
          return targetChunks.get(chunkIndex).endLineNumber;
        }
      }
    }
  }

  private static class ChunkData {
    final int startLineNumber;
    final int endLineNumber;
    final int startPosition;
    final int endPosition;

    ChunkData(int startLineNumber, int endLineNumber,
        int startPosition, int endPosition) {
      this.startLineNumber = startLineNumber;
      this.endLineNumber = endLineNumber;
      this.startPosition = startPosition;
      this.endPosition = endPosition;
    }
  }

  private enum ColorName {
    ADDED_BACKGROUND(new RGB(220, 255, 220)),
    CHANGED_BACKGROUND(new RGB(220, 220, 255)),
    CHANGED_LINE_BACKGROUND(new RGB(240, 240, 240));

    private final RGB rgb;

    private ColorName(RGB rgb) {
      this.rgb = rgb;
    }

    public RGB getRgb() {
      return rgb;
    }
  }

  private static class Colors {
    private final Display display;
    private final Map<ColorName, Color> colorMap = new EnumMap<ColorName, Color>(ColorName.class);

    public Colors(Display display) {
      this.display = display;
    }

    Color get(ColorName name) {
      Color result = colorMap.get(name);
      if (result == null) {
        result = new Color(display, name.getRgb());
        colorMap.put(name, result);
      }
      return result;
    }

    void dispose() {
      for (Color color : colorMap.values()) {
        color.dispose();
      }
    }
  }
}

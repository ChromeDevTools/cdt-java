// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui.propertypages;

import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * UI control element that shows several last segments of the file path and allow to show more
 * or less. The segments is used in the system to match file in remote VM, thus showing more
 * segments makes matching more accurate. Accurateness value is the number of visible segments.
 */
class AccuratenessControl {
  private final Logic logic;

  AccuratenessControl(Composite composite, String[] pathSegments, int initialAccuratenessValue) {
    Composite inner;
    {
      inner = new Composite(composite, SWT.NONE);
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 1;
      inner.setLayout(topLayout);
      inner.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    final PathLabel pathLabel = new PathLabel(inner, pathSegments);
    pathLabel.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    pathLabel.setVisiblePart(initialAccuratenessValue);

    final Button less;
    final Button more;
    {
      Composite buttonsGroup = new Composite(inner, SWT.NONE);
      GridLayout topLayout = new GridLayout();
      topLayout.numColumns = 2;
      buttonsGroup.setLayout(topLayout);
      buttonsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

      less = new Button(buttonsGroup, SWT.NONE);
      less.setText(Messages.AccuratenessControl_LESS_BUTTON);
      more = new Button(buttonsGroup, SWT.NONE);
      more.setText(Messages.AccuratenessControl_MORE_BUTTON);
    }

    Elements elements = new Elements() {
      @Override
      public AccuratenessControl.PathLabel getPathLabel() {
        return pathLabel;
      }

      @Override
      public Button getMoreButton() {
        return more;
      }

      @Override
      public Button getLessButton() {
        return less;
      }
    };

    logic = createLogic(elements);

    logic.updateAll();
  }

  int getAccuratenessValue() {
    return logic.getAccuratenessValue();
  }

  private static Logic createLogic(final Elements elements) {
    // The logic is Updater-based.
    // Presented path of several visible segments influences whether 'more' or 'less'
    // buttons enabled.
    final Updater updater = new Updater();

    final AccuratenessControl.PathLabel pathElement = elements.getPathLabel();
    final ValueSource<Integer> visiblePartSource = new ValueSource<Integer>() {
      @Override
      public Integer getValue() {
        return pathElement.getVisiblePartSize();
      }
    };

    updater.addSource(updater.rootScope(), visiblePartSource);

    // Wraps button as ValueConsumer.
    abstract class ButtonController implements ValueConsumer {
      private final Button button;

      ButtonController(Button button) {
        this.button = button;

        button.addSelectionListener(new SelectionListener() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            int size = pathElement.getVisiblePartSize();
            if (!isEnabled(size)) {
              return;
            }
            int newSize = getNewValue(size);
            pathElement.setVisiblePart(newSize);
            updater.reportChanged(visiblePartSource);
            updater.update();
          }

          @Override
          public void widgetDefaultSelected(SelectionEvent e) {
          }
        });
      }

      @Override
      public void update(Updater updater) {
        int size = pathElement.getVisiblePartSize();
        boolean enabled = isEnabled(size);
        button.setEnabled(enabled);
      }

      // Defines button logic.
      protected abstract boolean isEnabled(int visiblePartSize);

      // Defines button logic.
      protected abstract int getNewValue(int currentValue);
    }

    ValueConsumer lessController = new ButtonController(elements.getLessButton()) {
      @Override
      protected boolean isEnabled(int visiblePartSize) {
        return visiblePartSize > 1;
      }

      @Override
      protected int getNewValue(int currentValue) {
        return currentValue - 1;
      }
    };

    updater.addConsumer(updater.rootScope(), lessController);
    updater.addDependency(lessController, visiblePartSource);

    final int size = elements.getPathLabel().size();
    ValueConsumer moreController = new ButtonController(elements.getMoreButton()) {
      @Override
      protected boolean isEnabled(int visiblePartSize) {
        return visiblePartSize < size;
      }

      @Override
      protected int getNewValue(int currentValue) {
        return currentValue + 1;
      }
    };

    updater.addConsumer(updater.rootScope(), moreController);
    updater.addDependency(moreController, visiblePartSource);

    return new Logic() {
      @Override
      public int getAccuratenessValue() {
        return elements.getPathLabel().getVisiblePartSize();
      }
      @Override
      public void updateAll() {
        updater.updateAll();
      }
    };
  }

  private interface Elements {
    AccuratenessControl.PathLabel getPathLabel();
    Button getLessButton();
    Button getMoreButton();
  }

  private interface Logic {
    int getAccuratenessValue();
    void updateAll();
  }

  /**
   * UI elements that shows several last segments of a particular file path
   * (called visible part size).
   * Last segment is always visible and is presented in bold font.
   */
  private static class PathLabel {
    private final String[] components;
    private final StyledText styledText;
    private int visiblePartSize = 1;

    /**
     * @param components input file path components
     */
    PathLabel(Composite composite, String[] components) {
      this.components = components;
      styledText = new StyledText(composite, SWT.NONE);
      styledText.setEditable(false);
      styledText.setCaret(null);
      styledText.setBackground(composite.getBackground());
    }

    public Control getControl() {
      return styledText;
    }

    int size() {
      return components.length;
    }

    int getVisiblePartSize() {
      return visiblePartSize;
    }

    void setVisiblePart(int visiblePartSize) {
      this.visiblePartSize = visiblePartSize;
      int size = components.length;
      StringBuilder builder = new StringBuilder();
      for (int i = size - visiblePartSize; i < size - 1; i++) {
        builder.append(components[i]);
        builder.append("/"); //$NON-NLS-1$
      }
      int lastComponentStart = builder.length();
      builder.append(components[size - 1]);
      int lastComponentEnd = builder.length();
      styledText.setText(builder.toString());
      StyleRange boldRange = new StyleRange(lastComponentStart,
          lastComponentEnd - lastComponentStart, null, null, SWT.BOLD);
      StyleRange[] ranges = { boldRange };
      styledText.setStyleRanges(ranges);
      styledText.setCaretOffset(lastComponentStart);
    }
  }
}
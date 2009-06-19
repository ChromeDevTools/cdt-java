// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.List;

import org.chromium.debug.core.model.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * A dialog where users select which Google Chrome tab to attach to.
 */
class ChromiumTabSelectionDialog extends Dialog {

  private final List<String> urls;

  private Table table;

  private int selectedLine = -1;

  ChromiumTabSelectionDialog(Shell shell, List<String> urls) {
    super(shell);
    this.urls = urls;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(Messages.ChromiumTabSelectionDialog_DialogTitle);
  }

  @Override
  public int open() {
    return super.open();
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);
    Label label = new Label(composite, SWT.NONE);
    label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    label.setText(Messages.ChromiumTabSelectionDialog_TableTitle);

    table = new Table(composite, SWT.VIRTUAL | SWT.BORDER | SWT.SINGLE);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setItemCount(10);
    final TableColumn urlColumn = new TableColumn(table, SWT.NONE);
    urlColumn.setText(Messages.ChromiumTabSelectionDialog_UrlColumnName);
    urlColumn.setWidth(400);

    synchronized (this) {
      table.addListener(SWT.SetData, new Listener() {
        public void handleEvent(Event event) {
          if (table.isDisposed()) {
            return;
          }
          processData(event);
        }

        private void processData(Event event) {
          TableItem item = (TableItem) event.item;
          int index = table.indexOf(item);
          if (index < urls.size()) {
            item.setText(urls.get(index));
            GridData data = new GridData();
            data.grabExcessHorizontalSpace = true;
            item.setData(data);
            if (index == 0) {
              table.select(0);
            }
          }
        }
      });
      table.setItemCount(urls.size());
      table.clearAll();
    }
    return composite;
  }

  @Override
  protected void okPressed() {
    selectedLine = table.getSelectionIndex();
    super.okPressed();
  }

  int getSelectedLine() {
    return selectedLine;
  }
}

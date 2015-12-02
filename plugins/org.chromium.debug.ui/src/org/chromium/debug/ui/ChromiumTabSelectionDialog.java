// Copyright (c) 2009 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui;

import java.util.List;

import org.chromium.debug.core.model.Messages;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * A dialog where users select which Chromium tab to attach to.
 */
class ChromiumTabSelectionDialog extends Dialog {

  private final List<String> urls;

  private Table table;

  private int selectedLine = -1;

  ChromiumTabSelectionDialog(Shell shell, List<String> urls) {
    super(shell);
    this.urls = urls;
  }

  @Override protected boolean isResizable() {
    return true;
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
  public void create() {
    super.create();
    updateOkButton();
  }


  @Override
  protected Control createDialogArea(Composite parent) {
    Composite composite = (Composite) super.createDialogArea(parent);
    Label label = new Label(composite, SWT.NONE);
    label.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    label.setText(Messages.ChromiumTabSelectionDialog_TableTitle);

    table = new Table(composite, SWT.VIRTUAL | SWT.BORDER | SWT.SINGLE);
    table.setLayoutData(new GridData(GridData.FILL_BOTH));
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    table.setItemCount(10);
    final TableColumn urlColumn = new TableColumn(table, SWT.NONE);
    urlColumn.setText(Messages.ChromiumTabSelectionDialog_UrlColumnName);
    urlColumn.setWidth(400);

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
    table.addSelectionListener(new SelectionListener() {
      public void widgetDefaultSelected(SelectionEvent e) {
        okPressed();
      }
      public void widgetSelected(SelectionEvent e) {
      }
    });

    table.setItemCount(urls.size());
    table.clearAll();

    return composite;
  }

  private void updateOkButton() {
    this.getButton(IDialogConstants.OK_ID).setEnabled(urls.size() != 0);
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

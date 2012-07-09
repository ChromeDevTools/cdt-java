// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.debug.ui;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A small set of utils for UI tables.
 */
public class TableUtils {
  /**
   * A generic label provider that runs over type-safe column providers.
   * @param <ROW> type of row elements
   */
  public static class ColumnBasedLabelProvider<ROW> implements ITableLabelProvider {
    private final List<? extends ColumnData<ROW, ?>> columns;
    private final ValueAdapter<Object, ROW> rowElementAdpater;

    public ColumnBasedLabelProvider(ValueAdapter<Object, ROW> rowElementAdpater,
        List<? extends ColumnData<ROW, ?>> columns) {
      this.rowElementAdpater = rowElementAdpater;
      this.columns = columns;
    }

    public Image getColumnImage(Object element, int columnIndex) {
      ROW rowElement = rowElementAdpater.convert(element);
      ColumnData<ROW, ?> data = columns.get(columnIndex);
      return getImageImpl(data, rowElement);
    }

    private <C> Image getImageImpl(ColumnData<ROW, C> data, ROW rowElement) {
      C columnValue = data.getColumnValueConverter().convert(rowElement);
      return data.getLabelProvider().getColumnImage(columnValue);
    }

    public String getColumnText(Object element, int columnIndex) {
      ROW rowElement = rowElementAdpater.convert(element);
      ColumnData<ROW, ?> data = columns.get(columnIndex);
      return getTextImpl(data, rowElement);
    }

    private <C> String getTextImpl(ColumnData<ROW, C> data, ROW rowElement) {
      C columnValue = data.getColumnValueConverter().convert(rowElement);
      return data.getLabelProvider().getColumnText(columnValue);
    }

    public void addListener(ILabelProviderListener listener) {}

    public boolean isLabelProperty(Object element, String property) { return false; }

    public void removeListener(ILabelProviderListener listener) {}

    public void dispose() {
      for (ColumnData<ROW, ?> column : columns) {
        column.getLabelProvider().dispose();
      }
    }
  }

  public interface ValueAdapter<F, T> {
    T convert(F from);
  }

  public static class TrivialAdapter<T> implements ValueAdapter<T, T> {
    public T convert(T from) {
      return from;
    }
  }

  /**
   * Creates a ValueAdapter that forces cast from Object to the type.
   */
  public static <T> ValueAdapter<Object, T> createCastAdapter(final Class<T> type) {
    return new ValueAdapter<Object, T>() {
      public T convert(Object from) {
        return type.cast(from);
      }
    };
  }

  /**
   * A small structure that holds column-related data: column label provider and
   * adapter that gets column-specific field from row element.
   * @param <R> type of row element
   * @param <C> type of this column element
   */
  public static class ColumnData<R, C> {
    private final ValueAdapter<R, C> columnValueConverter;
    private final ColumnLabelProvider<C> labelProvider;

    public static <R, C> ColumnData<R, C> create(ValueAdapter<R, C> columnValueConverter,
        ColumnLabelProvider<C> labelProvider) {
      return new ColumnData<R, C>(columnValueConverter, labelProvider);
    }

    public ColumnData(ValueAdapter<R, C> columnValueConverter,
        ColumnLabelProvider<C> labelProvider) {
      this.columnValueConverter = columnValueConverter;
      this.labelProvider = labelProvider;
    }

    public ValueAdapter<R, C> getColumnValueConverter() {
      return columnValueConverter;
    }

    public ColumnLabelProvider<C> getLabelProvider() {
      return labelProvider;
    }
  }

  /**
   * A type column label provider. After its use it will be disposed.
   * It is also responsible for creating a physical column in a {@link Table}.
   * @param <C> type of column element
   */
  public static abstract class ColumnLabelProvider<C> {
    public abstract Image getColumnImage(C element);
    public abstract String getColumnText(C element);
    public abstract TableColumn createColumn(Table table);
    public void dispose() {}
  }

}

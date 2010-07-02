package org.chromium.debug.ui;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableUtils {
  public static class ColumnBasedLabelProvider<T> implements ITableLabelProvider {
    private final List<? extends ColumnData<T, ?>> columns;
    private final ValueAdapter<Object, T> rowElementAdpater;

    public ColumnBasedLabelProvider(ValueAdapter<Object, T> rowElementAdpater, List<? extends ColumnData<T, ?>> columns) {
      this.rowElementAdpater = rowElementAdpater;
      this.columns = columns;
    }

    public Image getColumnImage(Object element, int columnIndex) {
      T rowElement = rowElementAdpater.convert(element);
      ColumnData<T, ?> data = columns.get(columnIndex);
      return getImageImpl(data, rowElement);
    }
    private <C> Image getImageImpl(ColumnData<T, C> data, T rowElement) {
      C columnValue = data.getColumnValueConverter().convert(rowElement);
      return data.getLabelProvider().getColumnImage(columnValue);
    }
    public String getColumnText(Object element, int columnIndex) {
      T rowElement = rowElementAdpater.convert(element);
      ColumnData<T, ?> data = columns.get(columnIndex);
      return getTextImpl(data, rowElement);
    }
    private <C> String getTextImpl(ColumnData<T, C> data, T rowElement) {
      C columnValue = data.getColumnValueConverter().convert(rowElement);
      return data.getLabelProvider().getColumnText(columnValue);
    }

    public void addListener(ILabelProviderListener listener) {}

    public boolean isLabelProperty(Object element, String property) { return false; }

    public void removeListener(ILabelProviderListener listener) {}

    public void dispose() {
      for (ColumnData<T, ?> column : columns) {
        column.getLabelProvider().dispose();
      }
    }
  }

  public static abstract class RowElementAdpater<T> {
    public abstract T cast(Object rowElement);

    public static <T> RowElementAdpater<T> create(final Class<T> type) {
      return new RowElementAdpater<T>() {
        @Override
        public T cast(Object rowElement) {
          return type.cast(rowElement);
        }
      };
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
  public static <T> ValueAdapter<Object, T> createCastAdapter(final Class<T> type) {
    return new ValueAdapter<Object, T>() {
      public T convert(Object from) {
        return type.cast(from);
      }
    };
  }

  public static class ColumnData<R, C> {
    public static <R, C> ColumnData<R, C> create(ValueAdapter<R, C> columnValueConverter, ColumnLabelProvider<C> labelProvider) {
      return new ColumnData<R, C>(columnValueConverter, labelProvider);
    }

    private final ValueAdapter<R, C> columnValueConverter;
    private final ColumnLabelProvider<C> labelProvider;

    public ColumnData(ValueAdapter<R, C> columnValueConverter, ColumnLabelProvider<C> labelProvider) {
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

//  public static abstract class ColumnGetter<T, C> {
//    private final ColumnLabelProvider<C> labelProvider;
//    public ColumnGetter(ColumnLabelProvider<C> labelProvider) {
//      this.labelProvider = labelProvider;
//    }
//    public abstract C getColumnValue(T rowElement);
//    public ColumnLabelProvider<C> getLabelProvider() {
//      return labelProvider;
//    }
//  }

  public static abstract class ColumnLabelProvider<C> {
    public abstract Image getColumnImage(C element);
    public abstract String getColumnText(C element);
    public abstract TableColumn createColumn(Table table);
    public void dispose() {}
  }

}

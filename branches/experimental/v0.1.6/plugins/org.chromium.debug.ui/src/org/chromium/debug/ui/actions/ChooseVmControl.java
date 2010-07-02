package org.chromium.debug.ui.actions;

import org.chromium.debug.core.ChromiumDebugPlugin;
import org.chromium.debug.core.model.DebugTargetImpl;
import org.chromium.debug.core.util.ScriptTargetMapping;
import org.chromium.debug.ui.TableUtils;
import org.chromium.debug.ui.TableUtils.ColumnBasedLabelProvider;
import org.chromium.debug.ui.TableUtils.ColumnData;
import org.chromium.debug.ui.TableUtils.ColumnLabelProvider;
import org.chromium.debug.ui.TableUtils.ValueAdapter;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChooseVmControl {
  public static Logic create(Composite parent) {
    final Table table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);

    table.setFont(parent.getFont());

    final CheckboxTableViewer tableViewer = new CheckboxTableViewer(table);

    table.setHeaderVisible(true);

    tableViewer.setContentProvider(new ContentProviderImpl());

    ValueAdapter<ScriptTargetMapping, DebugTargetImpl> pairToTargetAdapter =
        new ValueAdapter<ScriptTargetMapping, DebugTargetImpl>() {
          public DebugTargetImpl convert(ScriptTargetMapping from) {
            return from.getDebugTarget();
          }
    };


    List<ColumnData<ScriptTargetMapping, ?>> columnDataList = createLaunchTargetColumns(pairToTargetAdapter);

    for (ColumnData<?,?> data : columnDataList) {
      data.getLabelProvider().createColumn(table);
    }

    ValueAdapter<Object, ScriptTargetMapping> rowElementAdapter = TableUtils.createCastAdapter(ScriptTargetMapping.class);

    ColumnBasedLabelProvider<ScriptTargetMapping> labelProvider =
        new ColumnBasedLabelProvider<ScriptTargetMapping>(rowElementAdapter, columnDataList);

    tableViewer.setLabelProvider(labelProvider);

    final List<Logic.Listener> listeners = new ArrayList<Logic.Listener>(1);

    tableViewer.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        for (Logic.Listener listener : listeners) {
          listener.checkStateChanged();
        }
      }
    });

    return new Logic() {
      public Control getControl() {
        return table;
      }
      public void setData(List<? extends ScriptTargetMapping> targets) {
        TableData input = new TableData(targets);
        tableViewer.setInput(input);
      }
      public List<ScriptTargetMapping> getSelected() {
        final Object[] array = tableViewer.getCheckedElements();
        return new AbstractList<ScriptTargetMapping>() {
          @Override
          public ScriptTargetMapping get(int index) {
            return (ScriptTargetMapping) array[index];
          }
          @Override
          public int size() {
            return array.length;
          }
        };
      }
      public void selectAll() {
        tableViewer.setAllChecked(true);
      }
      public void addListener(Listener listener) {
        listeners.add(listener);
      }

      public void removeListener(Listener listener) {
        listeners.remove(listener);
      }
    };
  }

  public interface Logic {
    Control getControl();

    void setData(List<? extends ScriptTargetMapping> targets);
    List<ScriptTargetMapping> getSelected();

    void selectAll();

    void addListener(Listener listener);
    void removeListener(Listener listener);

    interface Listener {
      void checkStateChanged();
    }
  }

  private static class TableData {
    final List<? extends ScriptTargetMapping> targets;

    TableData(List<? extends ScriptTargetMapping> targets) {
      this.targets = targets;
    }
  }

  private static class ContentProviderImpl implements IStructuredContentProvider {
    public Object[] getElements(Object inputElement) {
      TableData input = (TableData) inputElement;
      return input.targets.toArray();
    }
    public void dispose() {}
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
  }

  public static class LaunchNameLabelProvider extends ColumnLabelProvider<DebugTargetImpl> {
    private Map<DebugTargetImpl, Image> createdImages = new HashMap<DebugTargetImpl, Image>();

    @Override
    public Image getColumnImage(DebugTargetImpl debugTarget) {
      Image result = createdImages.get(debugTarget);
      if (result == null) {
        ImageDescriptor imageDescriptor = DebugUITools.getDefaultImageDescriptor(debugTarget.getLaunch().getLaunchConfiguration());
        result = imageDescriptor.createImage();
        createdImages.put(debugTarget, result);
      }
      return result;
    }

    @Override
    public String getColumnText(DebugTargetImpl debugTarget) {
      return debugTarget.getLaunch().getLaunchConfiguration().getName();
    }
    @Override
    public TableColumn createColumn(Table table) {
      TableColumn launchCol = new TableColumn(table, SWT.NONE);
      launchCol.setText("Launch");
      launchCol.setWidth(200);
      return launchCol;
    }
    @Override
    public void dispose() {
      for (Image image : createdImages.values()) {
        image.dispose();
      }
    }
  }

  public static class TargetNameLabelProvider extends ColumnLabelProvider<DebugTargetImpl> {
    @Override
    public Image getColumnImage(DebugTargetImpl debugTarget) {
      return null;
    }

    @Override
    public String getColumnText(DebugTargetImpl debugTarget) {
      return debugTarget.getName();
    }
    @Override
    public TableColumn createColumn(Table table) {
      TableColumn targetCol = new TableColumn(table, SWT.NONE);
      targetCol.setText("Target");
      targetCol.setWidth(200);
      return targetCol;
    }
  }

  public static <R> List<ColumnData<R, ?>> createLaunchTargetColumns(ValueAdapter<R, DebugTargetImpl> rowValueAdapter) {
    List<ColumnData<R, ?>> result = new ArrayList<ColumnData<R, ?>>(2);

    result.add(ColumnData.create(rowValueAdapter, new LaunchNameLabelProvider()));
    result.add(ColumnData.create(rowValueAdapter, new TargetNameLabelProvider()));

    return result;
  }
}

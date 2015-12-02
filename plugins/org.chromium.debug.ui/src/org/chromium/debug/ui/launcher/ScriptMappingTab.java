// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui.launcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chromium.debug.core.model.IPredefinedSourceWrapProvider;
import org.chromium.debug.core.model.LaunchParams;
import org.chromium.debug.core.model.LaunchParams.LookupMode;
import org.chromium.debug.ui.PluginUtil;
import org.chromium.debug.ui.TableUtils;
import org.chromium.debug.ui.TableUtils.ColumnBasedLabelProvider;
import org.chromium.debug.ui.TableUtils.ColumnData;
import org.chromium.debug.ui.launcher.LaunchTabGroup.Params;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**
 * A launch configuration tab that holds source look-up and mapping parameters.
 */
public class ScriptMappingTab extends TabBase<ScriptMappingTab.Elements, Params>  {
  interface Elements {
    RadioButtonsLogic<LookupMode> getLookupMode();
    PredefinedWrapChooser getPredefinedWrapChooser();
  }

  ScriptMappingTab(Params params) {
    super(params);
  }

  @Override
  public Elements createElements(Composite parent, final Runnable modifyListener) {
    Composite composite = createDefaultComposite(parent);
    setControl(composite);

    LookupModeControl lookupModeControl =
        new LookupModeControl(composite, getParams().getScriptNameDescription());


    RadioButtonsLogic.Listener radioButtonsListener =
        new RadioButtonsLogic.Listener() {
          public void selectionChanged() {
            modifyListener.run();
          }
        };

    final RadioButtonsLogic<LookupMode> lookupModeLogic =
        lookupModeControl.createLogic(radioButtonsListener);

    Group group = new Group(composite, SWT.NONE);
    group.setText(Messages.ScriptMappingTab_RECOGNIZED_WRAPPING);
    {
      GridData data = new GridData();
      data.horizontalAlignment = GridData.FILL;
      group.setLayoutData(data);

      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      group.setLayout(layout);
    }

    final PredefinedWrapChooser wrapChooser = new PredefinedWrapChooser(group);

    {
      GridData data = new GridData();
      data.horizontalAlignment = GridData.FILL;
      wrapChooser.getControl().setLayoutData(data);
    }

    wrapChooser.addListener(new PredefinedWrapChooser.Listener() {
      @Override public void checkStateChanged() {
        modifyListener.run();
      }
    });

    return new Elements() {
      @Override public RadioButtonsLogic<LookupMode> getLookupMode() {
        return lookupModeLogic;
      }
      @Override public PredefinedWrapChooser getPredefinedWrapChooser() {
        return wrapChooser;
      }
    };
  }

  /**
   * UI control that allows to select supported wrappers from a predefined set.
   * It deals in terms {@link IPredefinedSourceWrapProvider.Entry} ids.
   * The control gives special treatment to ids that weren't resolved allowing user
   * to deselect them.
   */
  private static class PredefinedWrapChooser {
    private final Control root;
    private final CheckboxTableViewer tableViewer;
    private final List<Listener> listeners = new ArrayList<Listener>(1);

    PredefinedWrapChooser(Composite parent) {
      final Table table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.MULTI |
          SWT.FULL_SELECTION | SWT.V_SCROLL);
      root = table;

      table.setFont(parent.getFont());

      tableViewer = new CheckboxTableViewer(table);

      table.setHeaderVisible(false);

      tableViewer.setContentProvider(TableUtils.OBJECT_ARRAY_CONTENT_PROVIDER);

      TableUtils.ColumnLabelProvider<ItemData> columnOneLabelProvider =
          new TableUtils.ColumnLabelProvider<ItemData>() {
        @Override public Image getColumnImage(ItemData element) {
          return null;
        }
        @Override
        public String getColumnText(ItemData element) {
          return element.accept(columnTextVisitor);
        }
        private final ItemData.Visitor<String> columnTextVisitor = new ItemData.Visitor<String>() {
          @Override public String visitNormal(IPredefinedSourceWrapProvider.Entry entry) {
            return entry.getWrapper().getName();
          }
          @Override public String visitUnresolved(String id) {
            return NLS.bind(Messages.ScriptMappingTab_UNRESOVLED, id);
          }
        };

        @Override
        public TableColumn createColumn(Table table) {
          TableColumn statusCol = new TableColumn(table, SWT.NONE);
          int width = PluginUtil.getFontMetrics(table, table.getFont()).getAverageCharWidth() * 40;
          statusCol.setWidth(width);
          return statusCol;
        }
      };

      List<ColumnData<ItemData, ?>> columnList = new ArrayList<ColumnData<ItemData, ?>>(1);
      columnList.add(ColumnData.create(
          new TableUtils.TrivialAdapter<ItemData>(), columnOneLabelProvider));

      ColumnBasedLabelProvider<ItemData> labelProvider = new ColumnBasedLabelProvider<ItemData>(
          TableUtils.createCastAdapter(ItemData.class), columnList);

      labelProvider.setUpColumns(table);

      tableViewer.setLabelProvider(labelProvider);

      tableViewer.addDoubleClickListener(new IDoubleClickListener() {
        @Override
        public void doubleClick(final DoubleClickEvent event) {
          ISelection selection = event.getSelection();
          if (selection instanceof IStructuredSelection == false) {
            return;
          }
          IStructuredSelection structuredSelection = (IStructuredSelection) selection;
          if (structuredSelection.size() != 1) {
            return;
          }
          ItemData data = (ItemData) structuredSelection.getFirstElement();
          data.accept(new ItemData.Visitor<Void>() {
            @Override
            public Void visitNormal(IPredefinedSourceWrapProvider.Entry entry) {
              MessageBox messageBox =
                  new MessageBox(event.getViewer().getControl().getShell(), SWT.RESIZE);
              String description = entry.getHumanDescription();
              messageBox.setText(Messages.ScriptMappingTab_DESCRIPTION);
              messageBox.setMessage(description);
              messageBox.open();
              return null;
            }

            @Override
            public Void visitUnresolved(String id) {
              // Keep silent.
              return null;
            }
          });
        }
      });
      tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
          for (Listener listener : listeners) {
            listener.checkStateChanged();
          }
        }
      });
    }

    Control getControl() {
      return root;
    }

    void setData(Collection<String> wrapperIds,
        Map<String, IPredefinedSourceWrapProvider.Entry> entriesMap) {
      List<ItemData> itemList = new ArrayList<ItemData>(entriesMap.size());

      Map<String, Integer> idToPosition = new HashMap<String, Integer>(entriesMap.size());

      for (Map.Entry<String, IPredefinedSourceWrapProvider.Entry> en : entriesMap.entrySet()) {
        idToPosition.put(en.getKey(), itemList.size());
        itemList.add(ItemData.createNormal(en.getValue()));
      }

      List<ItemData> selected = new ArrayList<ItemData>(wrapperIds.size());

      for (String id : wrapperIds) {
        ItemData item;
        Integer pos = idToPosition.get(id);
        if (pos == null) {
          item = ItemData.createUnresolved(id);
          itemList.add(item);
        } else {
          item = itemList.get(pos);
        }
        selected.add(item);
      }

      tableViewer.setInput(itemList.toArray());
      tableViewer.setCheckedElements(selected.toArray());
    }

    Collection<String> getValue() {
      Object[] selection = tableViewer.getCheckedElements();
      List<String> result = new ArrayList<String>(selection.length);
      ItemData.Visitor<String> visitor = new ItemData.Visitor<String>() {
        @Override public String visitNormal(IPredefinedSourceWrapProvider.Entry entry) {
          return entry.getId();
        }
        @Override public String visitUnresolved(String id) {
          return id;
        }
      };
      for (Object selectionObject : selection) {
        ItemData item = (ItemData) selectionObject;
        result.add(item.accept(visitor));
      }
      return result;
    }

    void addListener(Listener listener) {
      listeners.add(listener);
    }

    static abstract class ItemData {
      interface Visitor<R> {
        R visitNormal(IPredefinedSourceWrapProvider.Entry entry);
        R visitUnresolved(String id);
      }
      abstract <R> R accept(Visitor<R> visitor);

      static ItemData createNormal(final IPredefinedSourceWrapProvider.Entry entry) {
        return new ItemData() {
          @Override <R> R accept(Visitor<R> visitor) {
            return visitor.visitNormal(entry);
          }
        };
      }

      static ItemData createUnresolved(final String id) {
        return new ItemData() {
          @Override <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnresolved(id);
          }
        };
      }
    }

    interface Listener {
      void checkStateChanged();
    }
  }

  /**
   * Dialog UI group of 2 radio buttons for lookup mode.
   */
  private static class LookupModeControl {
    private final Map<LookupMode, Button> buttons;
    LookupModeControl(Composite container, String scriptNameFormatDescription) {
      buttons = new LinkedHashMap<LookupMode, Button>();
      Group group = new Group(container, 0);
      group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      group.setText(Messages.ChromiumRemoteTab_LOOKUP_GROUP_TITLE);
      group.setLayout(new GridLayout(1, false));

      buttons.put(LookupMode.EXACT_MATCH, createButtonBlock(group,
          Messages.ChromiumRemoteTab_EXACT_MATCH, Messages.ChromiumRemoteTab_EXACT_MATCH_LINE1,
          Messages.ChromiumRemoteTab_EXACT_MATCH_LINE2));

      buttons.put(LookupMode.AUTO_DETECT, createButtonBlock(group,
          Messages.ChromiumRemoteTab_AUTODETECT, Messages.ChromiumRemoteTab_AUTODETECT_LINE1,
          Messages.ChromiumRemoteTab_AUTODETECT_LINE2 + scriptNameFormatDescription));

      addRadioButtonSwitcher(buttons.values());
    }

    RadioButtonsLogic<LookupMode> createLogic(RadioButtonsLogic.Listener listener) {
      return new RadioButtonsLogic<LookupMode>(buttons, listener);
    }

    private static Button createButtonBlock(Composite parent, String buttonLabel,
        String descriptionLine1, String descriptionLine2) {
      Composite buttonComposite = new Composite(parent, SWT.NONE);
      GridLayout gridLayout = createHtmlStyleGridLayout(3);
      buttonComposite.setLayout(gridLayout);
      Button button = new Button(buttonComposite, SWT.RADIO | SWT.NO_RADIO_GROUP);
      button.setText(buttonLabel);
      Label padding = new Label(buttonComposite, SWT.NONE);
      padding.setText("   "); //$NON-NLS-1$
      Label descriptionLine1Label = new Label(buttonComposite, SWT.NONE);
      descriptionLine1Label.setText(descriptionLine1);
      // Extra label to fill a grid in layout.
      new Label(buttonComposite, SWT.NONE);
      new Label(buttonComposite, SWT.NONE);
      Label descriptionLine2Label = new Label(buttonComposite, SWT.NONE);
      descriptionLine2Label.setText(descriptionLine2);
      return button;
    }
  }

  @Override
  public String getName() {
    return Messages.ScriptMappingTab_TAB_NAME;
  }

  @Override
  protected MessageData isValidImpl(ILaunchConfiguration config) {
    try {
      LaunchParams.PredefinedSourceWrapperIds.resolveEntries(config);
    } catch (CoreException e) {
      return new MessageData(false,
          NLS.bind(Messages.ScriptMappingTab_UNRESOLVED_ERROR_MESSAGE, e.getMessage()));
    }
    return null;
  }

  protected TabFieldList<Elements, Params> getTabFields() {
    return TAB_FIELDS;
  }

  static final TabFieldList<Elements, Params> TAB_FIELDS;
  static {
    List<TabField<?, ?, Elements, Params>> list =
        new ArrayList<ChromiumRemoteTab.TabField<?, ?, Elements, Params>>(4);

    list.add(new TabField<String, LookupMode, Elements, Params>(
        LaunchParams.SOURCE_LOOKUP_MODE, TypedMethods.STRING,
        new FieldAccess<LookupMode, Elements>() {
          @Override
          void setValue(LookupMode value, Elements tabElements) {
            tabElements.getLookupMode().select(value);
          }
          @Override
          LookupMode getValue(Elements tabElements) {
            return tabElements.getLookupMode().getSelected();
          }
        },
        new DefaultsProvider<LookupMode, Params>() {
          @Override LookupMode getFallbackValue() {
            // TODO: support default value from eclipse variables.
            return LookupMode.DEFAULT_VALUE;
          }
          @Override LookupMode getInitialConfigValue(Params context) {
            return LookupMode.AUTO_DETECT;
          }
        },
        LookupMode.STRING_CONVERTER));

    list.add(new TabField<String, List<String>, Elements, Params>(
        LaunchParams.PredefinedSourceWrapperIds.CONFIG_PROPERTY, TypedMethods.STRING,
            new FieldAccess<List<String>, Elements>() {
          @Override
          void setValue(List<String> value, Elements tabElements) {
            tabElements.getPredefinedWrapChooser().setData(value,
                IPredefinedSourceWrapProvider.Access.getEntries());
          }
          @Override
          List<String> getValue(Elements tabElements) {
            List<String> result =
                new ArrayList<String>(tabElements.getPredefinedWrapChooser().getValue());
            return result;
          }
        },
        new DefaultsProvider<List<String>, Params>() {
          @Override List<String> getFallbackValue() {
            return Collections.emptyList();
          }
          @Override
          List<String> getInitialConfigValue(Params context) {
            List<String> result;
            if (context.preEnableSourceWrapper()) {
              result = new ArrayList<String>(
                  IPredefinedSourceWrapProvider.Access.getEntries().keySet());
            } else {
              result = Collections.emptyList();
            }
            return result;
          }
        },
        LaunchParams.PredefinedSourceWrapperIds.CONVERTER));

    TAB_FIELDS = createFieldListImpl(list);
  }
}

// Copyright (c) 2010 The Chromium Authors. All rights reserved.
// This program and the accompanying materials are made available
// under the terms of the Eclipse Public License v1.0 which accompanies
// this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html

package org.chromium.debug.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.chromium.debug.ui.DialogUtils.Message;
import org.chromium.debug.ui.DialogUtils.Optional;
import org.chromium.debug.ui.DialogUtils.ScopeEnabler;
import org.chromium.debug.ui.DialogUtils.Updater;
import org.chromium.debug.ui.DialogUtils.ValueConsumer;
import org.chromium.debug.ui.DialogUtils.ValueSource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A set of utils for creating Eclipse Wizard that is compatible with {@link Updater}.
 */
public class WizardUtils {

  /**
   * A simple implementation of the wizard page that stores mutable pointers
   * to its next/previous pages and a pointer to page elements.
   * @param <PE> type of page elements object that keeps references to all page UI controls
   */
  public static class PageImpl<PE extends PageElements> extends WizardPage {
    private PE pageElements = null;
    private final PageElementsFactory<PE> factory;
    private PageImpl<?> previousPage = null;
    private PageImpl<?> nextPage = null;
    private List<PageListener> listeners = new ArrayList<PageListener>(0);

    public PageImpl(String pageName, PageElementsFactory<PE> factory, String title,
        String description) {
      super(pageName, title, null);
      this.factory = factory;
      setDescription(description);
    }

    public void createControl(Composite parent) {
      pageElements = factory.create(parent);
      setControl(pageElements.getMainControl());
    }

    public IWizardPage getPreviousPageImpl() {
      return previousPage;
    }

    public IWizardPage getNextPageImpl() {
      return nextPage;
    }

    @Override
    public void setVisible(boolean visible) {
      for (PageListener listener : listeners) {
        listener.onSetVisible(visible);
      }
      super.setVisible(visible);
    }

    public void addListener(PageListener listener) {
      listeners.add(listener);
    }

    public void removeListener(PageListener listener) {
      listeners.remove(listener);
    }

    public PE getPageElements() {
      return pageElements;
    }

    public void linkToNextPage(PageImpl<?> next) {
      if (next != null) {
        next.previousPage = this;
      }
      this.nextPage = next;

      LogicBasedWizard wizardImpl = (LogicBasedWizard) getWizard();
      wizardImpl.updateButtons();
    }

    public void unlinkFromNextPage(PageImpl<?> next) {
      if (next != null) {
        if (this.nextPage == next) {
          next.previousPage = null;
          this.nextPage = null;
        }
      }

      LogicBasedWizard wizardImpl = (LogicBasedWizard) getWizard();
      wizardImpl.updateButtons();
    }

    @Override
    public String getMessage() {
      LogicBasedWizard wizardImpl = (LogicBasedWizard) getWizard();
      return wizardImpl.getLogicMessage().getText();
    }

    @Override
    public int getMessageType() {
      LogicBasedWizard wizardImpl = (LogicBasedWizard) getWizard();
      return wizardImpl.getLogicMessage().getPriority().getMessageProviderType();
    }
  }

  /**
   * A base interface for page set object. Its sub-types should have getters to each page of
   * the wizard.
   */
  public interface WizardPageSet {
    /**
     * @return all pages that are accessible from this page set
     */
    List<? extends PageImpl<?>> getAllPages();

    WizardLogic createLogic(LogicBasedWizard wizardImpl);
  }

  /**
   * A base interface for page set object. Its sub-types should have getters to page-specific
   * elements.
   */
  public interface PageElements {
    Control getMainControl();
  }

  public interface PageElementsFactory<T extends PageElements> {
    T create(Composite parent);
  }

  /**
   * An accessor to a wizard logic system. Controls its basic life-cycle.
   */
  public interface WizardLogic {
    void updateAll();
    PageImpl<?> getStartingPage();
    void dispose();
  }

  public interface PageListener {
    void onSetVisible(boolean visible);
  }

  /**
   * A call-back that implements wizard 'finish' button action.
   */
  public interface WizardFinisher {
    boolean performFinish(IWizard wizard, IProgressMonitor monitor);
  }

  /**
   * A simple implementation of Eclipse {@link IWizard} that works with {@link WizardPageSet} and
   * {@link WizardLogic}. The logic is responsible for updating a {@link WizardFinisher} value
   * as user input changes.
   */
  public static class LogicBasedWizard extends Wizard {
    private final WizardPageSet pageSet;
    private WizardFinisher wizardFinisher = null;
    private UpdateButtonsState updateButtonsState = UpdateButtonsState.NOT_READY;
    private Message currentLogicMessage = DialogUtils.NULL_MESSAGE;
    private WizardLogic logic;
    private PageImpl<?> startingPage = null;

    private enum UpdateButtonsState {
      /**
       * Do not try to update buttons.
       */
      NOT_READY,

      /**
       * If you need to update buttons now, probably it's already available.
       */
      SHOULD_BE_READY
    }

    public LogicBasedWizard(WizardPageSet pageSet) {
      this.pageSet = pageSet;
      for (PageImpl<?> page : pageSet.getAllPages()) {
        addPage(page);
      }
    }

    Message getLogicMessage() {
      return currentLogicMessage;
    }

    @Override
    public void createPageControls(Composite pageContainer) {
      super.createPageControls(pageContainer);
      this.logic = pageSet.createLogic(this);
      this.startingPage = this.logic.getStartingPage();
      logic.updateAll();
      updateButtonsState = UpdateButtonsState.SHOULD_BE_READY;
    }

    @Override
    public boolean performFinish() {
      if (wizardFinisher == null) {
        return false;
      }
      final boolean[] result = { false };
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          result[0] = wizardFinisher.performFinish(LogicBasedWizard.this, monitor);
        }
      };
      try {
        getContainer().run(false, true, runnable);
        return result[0];
      } catch (InterruptedException e) {
        return false;
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean canFinish() {
      return wizardFinisher != null;
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
      PageImpl<?> pageImpl = (PageImpl<?>) page;
      return pageImpl.getNextPageImpl();
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
      PageImpl<?> pageImpl = (PageImpl<?>) page;
      return pageImpl.getPreviousPageImpl();
    }

    @Override
    public IWizardPage getStartingPage() {
      return startingPage;
    }

    public void setWizardFinisher(WizardFinisher wizardFinisher, Message message) {
      this.wizardFinisher = wizardFinisher;
      this.currentLogicMessage = message;
      updateButtons();
    }

    void updateButtons() {
      if (updateButtonsState == UpdateButtonsState.SHOULD_BE_READY) {
        getContainer().updateButtons();
        getContainer().updateMessage();
      }
    }

    @Override
    public void dispose() {
      if (logic != null) {
        logic.dispose();
      }
      super.dispose();
    }
  }

  /**
   * A utility class that ties {@link Updater} together with {@link LogicBasedWizard}.
   * It is fed with {@link WizardFinisher} value and optional warning value.
   * It controls messages and 'finish' button of the wizard.
   */
  public static class WizardFinishController implements ValueConsumer {
    private final ValueSource<? extends Optional<? extends WizardFinisher>> finisherValue;
    private final ValueSource<? extends Optional<?>> warningValue;
    private final LogicBasedWizard wizardImpl;

    public WizardFinishController(
        ValueSource<? extends Optional<? extends WizardFinisher>> finisherValue,
        ValueSource<? extends Optional<? extends Void>> warningValue,
        LogicBasedWizard wizardImpl) {
      this.finisherValue = finisherValue;
      this.warningValue = warningValue;
      this.wizardImpl = wizardImpl;
    }

    public void update(Updater updater) {
      WizardFinisher finisher;
      Set<Message> messages = new HashSet<Message>();
      if (warningValue != null) {
        Optional<?> warnings = warningValue.getValue();
        if (!warnings.isNormal()) {
          messages.addAll(warnings.errorMessages());
        }
      }
      if (finisherValue.getValue().isNormal()) {
        finisher = finisherValue.getValue().getNormal();
      } else {
        finisher = null;
        messages.addAll(finisherValue.getValue().errorMessages());
      }
      Message message = DialogUtils.chooseImportantMessage(messages);
      wizardImpl.setWizardFinisher(finisher, message);
    }
  }

  /**
   * A utility class that ties {@link Updater} together with {@link LogicBasedWizard}.
   * It is plugged into {@link Updater} as a {@link ScopeEnabler} and controls the
   * link to next page reference accordingly.
   */
  public static class NextPageEnabler implements ScopeEnabler {
    private final PageImpl<?> basePage;
    private final PageImpl<?> nextPage;
    public NextPageEnabler(PageImpl<?> basePage, PageImpl<?> nextPage) {
      this.basePage = basePage;
      this.nextPage = nextPage;
    }

    public void setEnabled(boolean enabled, boolean recursive) {
      if (enabled) {
        basePage.linkToNextPage(nextPage);
      } else {
        basePage.unlinkFromNextPage(nextPage);
      }
    }
  }
}

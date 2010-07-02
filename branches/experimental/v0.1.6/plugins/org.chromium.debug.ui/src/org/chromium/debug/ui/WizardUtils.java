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

public class WizardUtils {
  public static class PageImpl<T extends PageElements> extends WizardPage {
    private T pageElements = null;
    private final PageControlsFactory<T> factory;
    private PageImpl<?> previousPage = null;
    private PageImpl<?> nextPage = null;
    private List<PageListener> listeners = new ArrayList<PageListener>(0);

    public PageImpl(String pageName, PageControlsFactory<T> factory, String title,
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

    public T getPageElements() {
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

  public interface WizardPageSet {
    List<? extends PageImpl<?>> getAllPages();
    WizardLogic createLogic(LogicBasedWizard wizardImpl);
  }


  public interface PageListener {
    void onSetVisible(boolean visible);
  }

  public interface WizardFinisher {
    boolean performFinish(IWizard wizard, IProgressMonitor monitor);
  }

  public static class LogicBasedWizard extends Wizard {
    private final WizardPageSet pageSet;
    private WizardFinisher wizardFinisher = null;
    private UpdateButtonsState updateButtonsState = UpdateButtonsState.NOT_READY;
    private Message currentLogicMessage = DialogUtils.NULL_MESSAGE;
    private WizardLogic logic;
    private PageImpl<?> startingPage = null;

    private enum UpdateButtonsState {
      NOT_READY,
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
      final boolean [] result = { false };
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

  public interface PageElements {
    Control getMainControl();
  }

  public interface PageControlsFactory<T extends PageElements> {
    T create(Composite parent);
  }
  public interface WizardLogic {
    void updateAll();
    PageImpl<?> getStartingPage();
    void dispose();
  }
  public static class WizardFinishController implements ValueConsumer {
    private final ValueSource<? extends Optional<? extends WizardFinisher>> finisherValue;
    private final ValueSource<? extends Optional<Void>> warningValue;
    private final LogicBasedWizard wizardImpl;

    public WizardFinishController(ValueSource<? extends Optional<? extends WizardFinisher>> finisherValue,
        ValueSource<? extends Optional<Void>> warningValue,
        LogicBasedWizard wizardImpl) {
      this.finisherValue = finisherValue;
      this.warningValue = warningValue;
      this.wizardImpl = wizardImpl;
    }

    public void update(Updater updater) {
      WizardFinisher finisher;
      Set<Message> messages = new HashSet<Message>();
      if (warningValue != null) {
        Optional<Void> warnings = warningValue.getValue();
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

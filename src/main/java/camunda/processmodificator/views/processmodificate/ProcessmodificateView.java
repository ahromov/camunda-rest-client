package camunda.processmodificator.views.processmodificate;

import camunda.processmodificator.model.ModificateFormModel;
import camunda.processmodificator.model.MultipleModificateFormModel;
import camunda.processmodificator.service.impl.ProcessModificationRestService;
import camunda.processmodificator.service.impl.ProcessMultipleModificationRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import camunda.processmodificator.views.MainLayout;

import java.util.HashMap;
import java.util.Map;


@PageTitle("Process modificate")
@Route(value = "modificate", layout = MainLayout.class)
public class ProcessmodificateView extends HorizontalLayout {

    private MainForm modificateForm;
    private MainForm multipleModificateForm;
    private ModificateFormModel modificateFormModel = new ModificateFormModel();
    private MultipleModificateFormModel multipleModificateFormModel = new MultipleModificateFormModel();
    private Map<Tab, Component> tabsToPages;
    private Div pages;
    private Tabs tabs;
    private Div modificatePage;
    private Div multipleModificatePage;
    private Tab modificateTab;
    private Tab multipleModificateTab;

    public ProcessmodificateView(ProcessModificationRestService restService, ProcessMultipleModificationRestService restService2) {
        addClassName("processmodificate-view");
        this.modificateForm = new ProcessModificationForm(restService, this.modificateFormModel);
        this.multipleModificateForm = new ProcessMultipleModificationForm(restService2, this.multipleModificateFormModel);
        initTabs();
        add(tabs, pages);
    }

    private void initTabs() {
        modificateTab = new Tab("Moving token on single activity");
        multipleModificateTab = new Tab("Moving token by multiple activities");
        tabs = new Tabs(modificateTab, multipleModificateTab);
        initDivs();
        addTabs();
        tabs.addSelectedChangeListener(event -> {
            tabsToPages.values().forEach(page -> page.setVisible(false));
            Component selectedPage = tabsToPages.get(tabs.getSelectedTab());
            selectedPage.setVisible(true);
        });
    }

    private void addTabs() {
        tabsToPages = new HashMap<>();
        tabsToPages.put(modificateTab, modificatePage);
        tabsToPages.put(multipleModificateTab, multipleModificatePage);
    }

    private void initDivs() {
        modificatePage = new Div();
        modificatePage.add(modificateForm);
        multipleModificatePage = new Div();
        multipleModificatePage.add(multipleModificateForm);
        multipleModificatePage.setVisible(false);
        pages = new Div(modificatePage, multipleModificatePage);
    }
}

package camunda.processmodificator.views.main;

import camunda.processmodificator.model.MainFormModel;
import camunda.processmodificator.service.impl.ProcessMigrationRestService;
import camunda.processmodificator.views.BaseForm;
import camunda.processmodificator.views.MainLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

@PageTitle("Main")
@Route(value = "main", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class MainView extends HorizontalLayout {

    private BaseForm form;
    private MainFormModel formModel = new MainFormModel();

    public MainView(ProcessMigrationRestService restService) {
        addClassName("processmigrate-view");
        this.form = new MainForm(restService, this.formModel);
        add(form);
    }
}

package camunda.processmodificator.views.processmigrate;

import camunda.processmodificator.model.MigrateFormModel;
import camunda.processmodificator.service.impl.ProcessMigrationRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import camunda.processmodificator.views.MainLayout;
import com.vaadin.flow.router.RouteAlias;

@PageTitle("Process migrate")
@Route(value = "migrate", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class ProcessmigrateView extends HorizontalLayout {

    private MainForm form;
    private MigrateFormModel formModel = new MigrateFormModel();

    public ProcessmigrateView(ProcessMigrationRestService restService) {
        addClassName("processmigrate-view");
        this.form = new MigrationForm(restService, this.formModel);
        add(form);
    }
}

package camunda.processmodificator.views.processmodificate;

import camunda.processmodificator.service.impl.ProcessModificationRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import camunda.processmodificator.views.MainLayout;


@PageTitle("Process modificate")
@Route(value = "modificate", layout = MainLayout.class)
public class ProcessmodificateView extends HorizontalLayout {

    private MainForm form;

    public ProcessmodificateView(ProcessModificationRestService restService) {
        addClassName("processmodificate-view");
        this.form = new ProcessModificationForm(restService);
        add(form);
    }
}

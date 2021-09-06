package camunda.processmodificator.views.executionlocalvariable;

import camunda.processmodificator.service.impl.ExecutionLocalVariablesRestService;
import camunda.processmodificator.views.MainLayout;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;


@PageTitle("Execution variables")
@Route(value = "variables", layout = MainLayout.class)
public class ExecutionlocalvariableView extends HorizontalLayout {

    private MainForm form;

    public ExecutionlocalvariableView(ExecutionLocalVariablesRestService restService) {
        addClassName("executionvariables-view");
        this.form = new ExecutionVariablesForm(restService);
        add(form);
    }
}

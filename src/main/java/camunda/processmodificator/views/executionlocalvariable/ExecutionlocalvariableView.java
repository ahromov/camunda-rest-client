package camunda.processmodificator.views.executionlocalvariable;

import camunda.processmodificator.model.VariablesFormModel;
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
    private VariablesFormModel formModel = new VariablesFormModel();

    public ExecutionlocalvariableView(ExecutionLocalVariablesRestService restService) {
        addClassName("executionvariables-view");
        this.form = new ExecutionVariablesForm(restService, this.formModel);
        add(form);
    }
}

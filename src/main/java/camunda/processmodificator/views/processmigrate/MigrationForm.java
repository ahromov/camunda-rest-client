package camunda.processmodificator.views.processmigrate;

import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.textfield.TextField;

public class MigrationForm extends MainForm {

    private TextField targetProcessDefinition;

    public MigrationForm(CamundaRestService camundaRestService) {
        super(camundaRestService);
        this.targetProcessDefinition = initTargetProcessDefinition();
        super.initFormLayout(targetProcessDefinition);
        validateAndBind();
    }

    private void validateAndBind() {
        super.formBinder.forField(targetProcessDefinition).bind(formModel1 -> targetProcessDefinition.getValue(), FormModel::setTargetProcessDefinitionId);
    }

    private TextField initTargetProcessDefinition() {
        TextField finalActivity = new TextField();
        finalActivity.setLabel("Target process definition");
        finalActivity.setPlaceholder("b90b79fe-0fbf-11ec-8a14-005056973e87");
        finalActivity.setRequired(true);
        return finalActivity;
    }
}

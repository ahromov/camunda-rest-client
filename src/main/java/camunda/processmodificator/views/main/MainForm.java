package camunda.processmodificator.views.main;

import camunda.processmodificator.model.MainFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.BaseForm;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainForm extends BaseForm {

//    private TextField targetProcessDefinition;

    public MainForm(CamundaRestService camundaRestService, MainFormModel formModel) {
        super(camundaRestService, formModel);
//        this.targetProcessDefinition = initTargetProcessDefinition();
        super.initFormLayout(null);
        super.validateAndBindBean(this);
    }

//    private TextField initTargetProcessDefinition() {
//        TextField finalActivity = new TextField();
//        finalActivity.setLabel("Target process definition");
//        finalActivity.setPlaceholder("b90b79fe-0fbf-11ec-8a14-005056973e87");
//        finalActivity.setRequired(true);
//        return finalActivity;
//    }
}

package camunda.processmodificator.views.processmigrate;

import camunda.processmodificator.model.MigrateFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Setter
public class MigrationForm extends MainForm {

    private TextField targetProcessDefinition;

    public MigrationForm(CamundaRestService camundaRestService, MigrateFormModel formModel) {
        super(camundaRestService, formModel);
        this.targetProcessDefinition = initTargetProcessDefinition();
        super.initFormLayout(targetProcessDefinition);
        super.validateAndBindBean(this);
    }

    private TextField initTargetProcessDefinition() {
        TextField finalActivity = new TextField();
        finalActivity.setLabel("Target process definition");
        finalActivity.setPlaceholder("b90b79fe-0fbf-11ec-8a14-005056973e87");
        finalActivity.setRequired(true);
        return finalActivity;
    }
}

package camunda.processmodificator.views.main;

import camunda.processmodificator.model.MainFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.service.impl.MainRestService;
import camunda.processmodificator.views.BaseForm;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainForm extends BaseForm {

    public MainForm(MainRestService camundaRestService, MainFormModel formModel) {
        super(camundaRestService, formModel);
        super.initFormLayout(null);
        super.validateAndBindBean(this);
    }
}

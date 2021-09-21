package camunda.processmodificator.views.processmodificate;

import camunda.processmodificator.model.ModificateFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class ProcessModificationForm extends MainForm {

    private HorizontalLayout targetActivityLayout;
    private Select<String> position = new Select<>();
    private TextField targetActivity = new TextField();

    public ProcessModificationForm(CamundaRestService camundaRestService, ModificateFormModel formModel) {
        super(camundaRestService, formModel);
        this.targetActivityLayout = initFinalActivityLayout();
        super.initFormLayout(targetActivityLayout);
        super.validateAndBindBean(this);
    }

    private HorizontalLayout initFinalActivityLayout() {
        TextField finalActivity = initFinalActivityTextField();
        Select<String> position = initPositionsSelect();
        HorizontalLayout horizontalLayout = new HorizontalLayout(finalActivity, position);
        return horizontalLayout;
    }

    private Select<String> initPositionsSelect() {
        position.setLabel("Position");
        position.setItems(Arrays.asList("startAfterActivity", "startBeforeActivity"));
        return position;
    }

    private TextField initFinalActivityTextField() {
        targetActivity.setLabel("Final activity");
        targetActivity.setPlaceholder("Activity_0fm51ny");
        return targetActivity;
    }
}

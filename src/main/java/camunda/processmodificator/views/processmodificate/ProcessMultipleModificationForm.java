package camunda.processmodificator.views.processmodificate;

import camunda.processmodificator.model.MultipleModificateFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.views.MainForm;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class ProcessMultipleModificationForm extends MainForm {

    private HorizontalLayout targetActivityLayout;
    private TextArea activityIDs;
    private Select<String> targetActivityPosition;

    public ProcessMultipleModificationForm(CamundaRestService camundaRestService, MultipleModificateFormModel formModel) {
        super(camundaRestService, formModel);
        initFinalActivityLayout();
        super.initFormLayout(targetActivityLayout);
        super.validateAndBindBean(this);
    }

    private void initFinalActivityLayout() {
        targetActivityLayout = new HorizontalLayout();
        getArea();
        initPositionsSelect();
        targetActivityLayout.add(activityIDs, targetActivityPosition);
    }

    private void getArea() {
        activityIDs = new TextArea();
        activityIDs.setLabel("List activity IDs currentActivity>>targetActivity");
        activityIDs.setPlaceholder("Activity_0fm51ny>>Event_1ahky9z");
        activityIDs.setRequired(true);
    }

    private void initPositionsSelect() {
        targetActivityPosition = new Select<>();
        targetActivityPosition.setLabel("Position");
        targetActivityPosition.setItems(Arrays.asList("startAfterActivity", "startBeforeActivity"));
    }
}

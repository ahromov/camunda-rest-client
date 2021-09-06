package camunda.processmodificator.views.processmodificate;

import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.ProcessModificationRestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import camunda.processmodificator.views.MainLayout;

import java.util.List;

@PageTitle("Process modificate")
@Route(value = "modificate", layout = MainLayout.class)
public class ProcessmodificateView extends HorizontalLayout {

    private static ProcessModificationRestService restService;

    private FormLayout formLayout;
    private Notification notification;
    private TextField serverAddress;
    private TextArea taxIds;
    private HorizontalLayout finalActivityLayout;
    private TextField login;
    private TextField password;
    private Button move;
    private ProgressBar progressBar;

    public ProcessmodificateView(ProcessModificationRestService restService) {
        addClassName("processmodificate-view");
        this.restService = restService;
        formLayout = initFormLayout();
        notification = initNotification();
        add(formLayout);
    }

    private FormLayout initFormLayout() {
        FormLayout formLayout = new FormLayout();
        serverAddress = initServerAddressField();
        taxIds = initTaxIDsTextArea();
        finalActivityLayout = initFinalActivityLayout();
        login = initLoginField();
        password = initPasswordField();
        move = initMoveButton();
        progressBar = initProgressBar();
        formLayout.add(serverAddress, taxIds, finalActivityLayout, login, password, move, progressBar);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("1px", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("700px", 3));
        formLayout.setWidth("400px");
        return formLayout;
    }

    private Notification initNotification() {
        return new Notification(
                "All instance tokens moved!", 3000,
                Notification.Position.MIDDLE);
    }

    private TextField initServerAddressField() {
        TextField serverAddress = new TextField();
        serverAddress.setLabel("Server address");
        serverAddress.setPlaceholder("http://localhost:8080");
        serverAddress.setValue("https://kpayegapu01.alfa.bank.int:8343");
        return serverAddress;
    }

    private TextArea initTaxIDsTextArea() {
        TextArea taxIds = new TextArea();
        taxIds.setLabel("Contragents tax IDs (or full buiseness keys)");
        return taxIds;
    }

    private HorizontalLayout initFinalActivityLayout() {
        TextField finalActivity = initFinalActivityTextField();
        Select<String> position = initPositionsSelect();
        HorizontalLayout horizontalLayout = new HorizontalLayout(finalActivity, position);
        return horizontalLayout;
    }

    private Select<String> initPositionsSelect() {
        Select<String> position = new Select<>();
        final List<String> ACTIVITY_POSITIONS = List.of("startBeforeActivity", "startAfterActivity");
        position.setLabel("Position");
        position.setItems(ACTIVITY_POSITIONS);
        position.setValue(ACTIVITY_POSITIONS.get(1));
        return position;
    }

    private TextField initFinalActivityTextField() {
        TextField finalActivity = new TextField();
        finalActivity.setLabel("Final activity");
        finalActivity.setPlaceholder("activity ID");
        finalActivity.setValue("Activity_0a1lhwp");
        return finalActivity;
    }

    private TextField initLoginField() {
        TextField login = new TextField();
        login.setLabel("Login");
        login.setPlaceholder("user");
        return login;
    }

    private TextField initPasswordField() {
        TextField password = new TextField();
        password.setLabel("Password");
        password.setPlaceholder("password");
        return password;
    }

    private Button initMoveButton() {
        Button move = new Button("Move");
        move.addClickListener(buttonClickEvent -> {
            moveTokens();
        });
        return move;
    }

    private ProgressBar initProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setVisible(false);
        return progressBar;
    }

    private void moveTokens() {
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        FormModel formModel = FormModel.builder()
                .serverAddress(serverAddress.getValue())
                .finalActivityID(((TextField) finalActivityLayout.getComponentAt(0)).getValue())
                .finalActivityPosition(((Select<String>) finalActivityLayout.getComponentAt(1)).getValue())
                .engineLogin(login.getValue())
                .enginePassword(password.getValue())
                .build();
        formModel.setTaxIDs(taxIds.getValue());
        restService.moveTokens(formModel);
        progressBar.setVisible(false);
        notification.open();
    }
}

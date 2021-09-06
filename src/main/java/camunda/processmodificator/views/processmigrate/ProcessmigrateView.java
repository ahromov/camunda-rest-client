package camunda.processmodificator.views.processmigrate;

import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.ProcessMigrationRestService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import camunda.processmodificator.views.MainLayout;
import com.vaadin.flow.router.RouteAlias;

import java.util.List;

@PageTitle("Process migrate")
@Route(value = "migrate", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class ProcessmigrateView extends HorizontalLayout {

    private static ProcessMigrationRestService restService;

    private FormLayout formLayout;
    private Notification notification;
    private TextField serverAddress;
    private TextArea taxIds;
    private TextField targetProcessDefinition;
    private TextField login;
    private TextField password;
    private Button migrate;
    private ProgressBar progressBar;

    public ProcessmigrateView(ProcessMigrationRestService restService) {
        addClassName("processmigrate-view");
        this.restService = restService;
        formLayout = initFormLayout();
        notification = initNotification();
        add(formLayout);
    }

    private FormLayout initFormLayout() {
        FormLayout formLayout = new FormLayout();
        serverAddress = initServerAddressField();
        taxIds = initTaxIDsTextArea();
        targetProcessDefinition = initTargetProcessDefinition();
        login = initLoginField();
        password = initPasswordField();
        migrate = initMoveButton();
        progressBar = initProgressBar();
        formLayout.add(serverAddress, taxIds, targetProcessDefinition, login, password, migrate, progressBar);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("1px", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("700px", 3));
        formLayout.setWidth("400px");
        return formLayout;
    }

    private Notification initNotification() {
        return new Notification(
                "All process instances migrated!", 3000,
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
        taxIds.setLabel("Contragents tax IDs");
        return taxIds;
    }

    private TextField initTargetProcessDefinition() {
        TextField finalActivity = new TextField();
        finalActivity.setLabel("Target process definition");
        finalActivity.setPlaceholder("definition ID");
        finalActivity.setValue("604e6999-0e40-11ec-8a14-005056973e87");
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
        Button move = new Button("Migrate");
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
                .targetProcessDefinitionId(targetProcessDefinition.getValue())
                .engineLogin(login.getValue())
                .enginePassword(password.getValue())
                .build();
        formModel.setTaxIDs(taxIds.getValue());
        restService.migrateProcessInstance(formModel);
        progressBar.setVisible(false);
        notification.open();
    }
}

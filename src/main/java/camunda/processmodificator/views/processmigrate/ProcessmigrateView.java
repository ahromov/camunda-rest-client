package camunda.processmodificator.views.processmigrate;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import camunda.processmodificator.views.MainLayout;
import com.vaadin.flow.router.RouteAlias;

@PageTitle("Process migrate")
@Route(value = "migrate", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class ProcessmigrateView extends HorizontalLayout {

    private TextField name;
    private Button sayHello;

    public ProcessmigrateView() {
        addClassName("processmigrate-view");
        name = new TextField("Your name");
        sayHello = new Button("Say hello");
        add(name, sayHello);
        setVerticalComponentAlignment(Alignment.END, name, sayHello);
        sayHello.addClickListener(e -> {
            Notification.show("Hello " + name.getValue());
        });
    }

}

package camunda.processmodificator.service.impl;

import camunda.processmodificator.model.BaseFormModel;
import camunda.processmodificator.service.CamundaRestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MainRestService implements CamundaRestService {

    private final ProcessMigrationRestService processMigrationRestService;
    private final ProcessMultipleModificationRestService processMultipleModificationRestService;

    public MainRestService(ProcessMigrationRestService processMigrationRestService,
                           ProcessMultipleModificationRestService processMultipleModificationRestService) {
        this.processMigrationRestService = processMigrationRestService;
        this.processMultipleModificationRestService = processMultipleModificationRestService;
    }

    public void send(BaseFormModel formModel) {
        processMigrationRestService.send(formModel);
        processMultipleModificationRestService.send(formModel);
    }
}

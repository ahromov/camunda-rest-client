package camunda.processmodificator.service.impl;

import camunda.processmodificator.configuration.ProcessConstants;
import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessMigrationRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceMigrationResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
import camunda.processmodificator.model.BaseFormModel;
import camunda.processmodificator.service.CamundaRestService;
import camunda.processmodificator.service.routes.CamundaApiRoutes;
import camunda.processmodificator.service.utils.CamundaApiUtils;
import camunda.processmodificator.service.utils.FileLoader;
import camunda.processmodificator.service.utils.IdStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Service
@Slf4j
public class ProcessMigrationRestService implements CamundaRestService {

    private final RestTemplate restTemplate;
    private final CamundaApiUtils camundaApiUtils;
    private final FileLoader fileLoader;
    private final IdStorage idStorage;

    public ProcessMigrationRestService(RestTemplate restTemplate,
                                       CamundaApiUtils camundaApiUtils,
                                       FileLoader fileLoader,
                                       IdStorage idStorage) {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
        this.fileLoader = fileLoader;
        this.idStorage = idStorage;
    }

    public void send(BaseFormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, formModel);

        List<String[]> taxIDs = camundaApiUtils.parse(fileLoader.getIpns());

        for (String[] tax : taxIDs) {
            int counter = 0;

            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            List<CamundaProcessInstanceResponse> camundaProcessInstanceResponse = camundaApiUtils.getObjects(processInstanceResponse);
            if (!camundaProcessInstanceResponse.isEmpty()) {
                log.info("Contragent ID: " + tax[0] + ". His applications = " + camundaProcessInstanceResponse.size() + ". Attempt migration the all applications!");

                for (CamundaProcessInstanceResponse processInstance : camundaProcessInstanceResponse) {
                    try {
                        if (camundaApiUtils.isProcessInstanceIncidents(formModel, headers, restTemplate, processInstance)) {
                            break;
                        }

                        idStorage.getIds().put(processInstance.getId(), tax[0]);

                        HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstance);

                        ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                                restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                        HttpEntity<CamundaProcessMigrationRequest> processInstanceMigrationRequestHttpEntity = prepareProcessInstanceMigrationRequestHttpEntity(headers, processInstance, activityInstanceResponse);

                        ResponseEntity<CamundaProcessInstanceMigrationResponse> processInstanceMigrationResponse =
                                restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_MIGRATION_RESOURCE_PATH), HttpMethod.POST, processInstanceMigrationRequestHttpEntity, CamundaProcessInstanceMigrationResponse.class);

                        logResponse(processInstanceResponse, processInstanceMigrationResponse.getStatusCodeValue());

                        counter++;
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }

                log.info("Applications migration is done!. " + counter + " successfull operations");
            } else {
                camundaApiUtils.logOperationException(tax[0], ProcessConstants.PROCESS_DEFINITION_KEY);
            }
        }
    }

    public HttpEntity<CamundaProcessMigrationRequest> prepareProcessInstanceMigrationRequestHttpEntity(HttpHeaders headers, CamundaProcessInstanceResponse processInstanceResponse, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        String processInstanceId = processInstanceResponse.getId();
        String currentProcessDefinitionId = processInstanceResponse.getDefinitionId();
        CamundaProcessMigrationRequest.Instructions instructions = getInstructions(activityInstanceResponse);
        CamundaProcessMigrationRequest.MigrationPlan migrationPlan = CamundaProcessMigrationRequest.MigrationPlan.builder()
                .sourceProcessDefinitionId(currentProcessDefinitionId)
                .targetProcessDefinitionId(fileLoader.getTargetProcessDefinitionId())
                .instructions(Arrays.asList(instructions))
                .build();
        CamundaProcessMigrationRequest camundaProcessInstanceMigrationRequest = CamundaProcessMigrationRequest.builder()
                .migrationPlan(migrationPlan)
                .processInstanceIds(Arrays.asList(processInstanceId))
                .skipCustomListeners(false)
                .build();
        HttpEntity<CamundaProcessMigrationRequest> activityInstanceRequestHttpEntity = new HttpEntity<>(camundaProcessInstanceMigrationRequest, headers);
        return activityInstanceRequestHttpEntity;
    }

    private CamundaProcessMigrationRequest.Instructions getInstructions(ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        List<String> sourceActivityIds = new LinkedList<>();
        CamundaActivityInstanceResponse camundaActivityInstanceResponse = camundaApiUtils.getObject(activityInstanceResponse).get();
        sourceActivityIds.add(camundaActivityInstanceResponse.getActivityId());
        List<String> targetActivityIds = new LinkedList<>();
        targetActivityIds.add(camundaActivityInstanceResponse.getActivityId());
        CamundaProcessMigrationRequest.Instructions instructions = CamundaProcessMigrationRequest.Instructions.builder()
                .sourceActivityIds(sourceActivityIds)
                .targetActivityIds(targetActivityIds)
                .updateEventTrigger(false)
                .build();
        return instructions;
    }

    private void logResponse(ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, Integer statusCode) {
        if (statusCode == 204) {
            CamundaProcessInstanceResponse camundaProcessInstanceResponse = camundaApiUtils.getObject(processInstanceResponse).get();
            log.debug("Process instance={}:{} moved from definition={} to definition={}",
                    camundaProcessInstanceResponse.getId(),
                    camundaProcessInstanceResponse.getBusinessKey(),
                    camundaProcessInstanceResponse.getDefinitionId(),
                    fileLoader.getTargetProcessDefinitionId());
        }
    }
}

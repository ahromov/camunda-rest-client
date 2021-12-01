package camunda.processmodificator.service.impl;

import camunda.processmodificator.configuration.Constants;
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
import java.util.Optional;

@Service
@Slf4j
public class ProcessMigrationRestService implements CamundaRestService {



    private final RestTemplate restTemplate;
    private final CamundaApiUtils camundaApiUtils;

    public ProcessMigrationRestService(RestTemplate restTemplate, CamundaApiUtils camundaApiUtils) {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
    }

    public void send(BaseFormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, formModel);

        List<String[]> taxIDs = camundaApiUtils.parse(Constants.IPNS);

        Integer counter = 0;

        log.info(taxIDs.size() + " list items. Start applications migration");

        for (String[] tax : taxIDs) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            Optional<CamundaProcessInstanceResponse> camundaProcessInstanceResponse = camundaApiUtils.getObject(processInstanceResponse);
            if (camundaProcessInstanceResponse.isPresent()) {
                CamundaProcessInstanceResponse processInstance = camundaProcessInstanceResponse.get();

                if (camundaApiUtils.isProcessInstanceIncidents(formModel, headers, restTemplate, processInstance)) {
                    break;
                }

                HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstanceResponse);

                ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                HttpEntity<CamundaProcessMigrationRequest> processInstanceMigrationRequestHttpEntity = prepareProcessInstanceMigrationRequestHttpEntity(headers, processInstanceResponse, activityInstanceResponse);

                ResponseEntity<CamundaProcessInstanceMigrationResponse> processInstanceMigrationResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_MIGRATION_RESOURCE_PATH), HttpMethod.POST, processInstanceMigrationRequestHttpEntity, CamundaProcessInstanceMigrationResponse.class);

                logResponse(processInstanceResponse, processInstanceMigrationResponse.getStatusCodeValue());

                counter++;
            } else {
                camundaApiUtils.logOperationException(tax[0], Constants.PROCESS_DEFINITION_KEY);
            }
        }

        log.info("Done applications migration. " + counter + " successfull operations");
    }

    public HttpEntity<CamundaProcessMigrationRequest> prepareProcessInstanceMigrationRequestHttpEntity(HttpHeaders headers, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        CamundaProcessInstanceResponse camundaProcessInstanceResponse = camundaApiUtils.getObject(processInstanceResponse).get();
        String processInstanceId = camundaProcessInstanceResponse.getId();
        String processDefinitionId = camundaProcessInstanceResponse.getDefinitionId();
        CamundaProcessMigrationRequest.Instructions instructions = getInstructions(activityInstanceResponse);
        CamundaProcessMigrationRequest.MigrationPlan migrationPlan = CamundaProcessMigrationRequest.MigrationPlan.builder()
                .sourceProcessDefinitionId(processDefinitionId)
                .targetProcessDefinitionId(Constants.TARGET_PROCESS_DEFINITION_ID)
                .instructions(Arrays.asList(instructions))
                .build();
        CamundaProcessMigrationRequest camundaProcessInstanceMigrationRequest = CamundaProcessMigrationRequest.builder()
                .migrationPlan(migrationPlan)
                .processInstanceIds(Arrays.asList(processInstanceId))
                .skipCustomListeners(true)
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
            log.info("Process instance={}:{} moved from definition={} to definition={}",
                    camundaProcessInstanceResponse.getId(),
                    camundaProcessInstanceResponse.getBusinessKey(),
                    camundaProcessInstanceResponse.getDefinitionId(),
                    "ce473fa-5150-11ec-9ca9-0050569796a7");
        }
    }
}

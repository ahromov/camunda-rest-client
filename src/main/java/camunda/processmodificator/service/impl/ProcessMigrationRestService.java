package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessMigrationRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceMigrationResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
import camunda.processmodificator.model.FormModel;
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

@Service
@Slf4j
public class ProcessMigrationRestService implements CamundaRestService {

    private RestTemplate restTemplate;
    private CamundaApiUtils camundaApiUtils;

    public ProcessMigrationRestService(RestTemplate restTemplate, CamundaApiUtils camundaApiUtils) {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
    }

    public void send(FormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, formModel);

        for (String[] tax : formModel.getTaxIDs()) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax, formModel);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            if (camundaApiUtils.getObject(processInstanceResponse).isPresent()) {
                CamundaProcessInstanceResponse processInstance = camundaApiUtils.getObject(processInstanceResponse).get();

                if (camundaApiUtils.isProcessInstanceIncidents(formModel, headers, restTemplate, processInstance)) {
                    break;
                }

                HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstanceResponse);

                ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                HttpEntity<CamundaProcessMigrationRequest> processInstanceMigrationRequestHttpEntity = prepareProcessInstanceMigrationRequestHttpEntity(headers, formModel, processInstanceResponse, activityInstanceResponse);

                ResponseEntity<CamundaProcessInstanceMigrationResponse> processInstanceMigrationResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_MIGRATION_RESOURCE_PATH), HttpMethod.POST, processInstanceMigrationRequestHttpEntity, CamundaProcessInstanceMigrationResponse.class);

                logResponse(formModel, processInstanceResponse, processInstanceMigrationResponse.getStatusCodeValue());
            }

        }
    }

    public HttpEntity<CamundaProcessMigrationRequest> prepareProcessInstanceMigrationRequestHttpEntity(HttpHeaders headers, FormModel formModel, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse) {
        String processInstanceId = camundaApiUtils.getObject(processInstanceResponse).get().getId();
        String processDefinitionId = camundaApiUtils.getObject(processInstanceResponse).get().getProcessDefinitionId();
        CamundaProcessMigrationRequest.Instructions instructions = getInstructions(activityInstanceResponse);
        CamundaProcessMigrationRequest.MigrationPlan migrationPlan = CamundaProcessMigrationRequest.MigrationPlan.builder()
                .sourceProcessDefinitionId(processDefinitionId)
                .targetProcessDefinitionId(formModel.getTargetProcessDefinitionId())
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
        sourceActivityIds.add(camundaApiUtils.getObject(activityInstanceResponse).get().getActivityId());
        List<String> targetActivityIds = new LinkedList<>();
        targetActivityIds.add(camundaApiUtils.getObject(activityInstanceResponse).get().getActivityId());
        CamundaProcessMigrationRequest.Instructions instructions = CamundaProcessMigrationRequest.Instructions.builder()
                .sourceActivityIds(sourceActivityIds)
                .targetActivityIds(targetActivityIds)
                .updateEventTrigger(false)
                .build();
        return instructions;
    }

    private void logResponse(FormModel formModel, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse, Integer statusCode) {
        if (statusCode == 204) {
            log.info("Process instance={}:{} moved from definition={} to definition={}",
                    camundaApiUtils.getObject(processInstanceResponse).get().getId(),
                    camundaApiUtils.getObject(processInstanceResponse).get().getBusinessKey(),
                    camundaApiUtils.getObject(processInstanceResponse).get().getProcessDefinitionId(),
                    formModel.getTargetProcessDefinitionId());
        }
    }
}

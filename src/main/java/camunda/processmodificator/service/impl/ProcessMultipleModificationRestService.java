package camunda.processmodificator.service.impl;

import camunda.processmodificator.configuration.Constants;
import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceModificationRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceModificationResponse;
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

import java.util.*;

@Service
@Slf4j
public class ProcessMultipleModificationRestService implements CamundaRestService {

    private final RestTemplate restTemplate;
    private final CamundaApiUtils camundaApiUtils;


    public ProcessMultipleModificationRestService(RestTemplate restTemplate, CamundaApiUtils camundaApiUtils) {
        this.restTemplate = restTemplate;
        this.camundaApiUtils = camundaApiUtils;
    }

    public void send(BaseFormModel formModel) {
        HttpHeaders headers = new HttpHeaders();

        camundaApiUtils.authenticate(headers, formModel);

        List<String[]> taxItems = camundaApiUtils.parse(Constants.IPNS);
        List<String[]> activityItems = camundaApiUtils.parse(Constants.ACTIVITIES);

        Integer counter = 0;

        log.info(taxItems.size() + " list items. Start applications moving");

        for (String[] tax : taxItems) {
            HttpEntity<CamundaProcessInstanceRequest> processInstanceRequestHttpEntity = CamundaApiUtils.prepareProcessInstanceRequestHttpEntity(headers, tax);

            ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse =
                    restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.PROCESS_INSTANCE_RESOURCE_PATH), HttpMethod.POST, processInstanceRequestHttpEntity, CamundaProcessInstanceResponse[].class);

            Optional<CamundaProcessInstanceResponse> camundaProcessInstanceResponse = camundaApiUtils.getObject(processInstanceResponse);
            if (camundaProcessInstanceResponse.isPresent()) {
                CamundaProcessInstanceResponse processInstance = camundaProcessInstanceResponse.get();

                if (camundaApiUtils.isProcessInstanceIncidents(formModel, headers, restTemplate, processInstance)) {
                    continue;
                }

                HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, processInstanceResponse);

                ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                        restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                Optional<CamundaActivityInstanceResponse> camundaActivityInstanceResponse = camundaApiUtils.getObject(activityInstanceResponse);
                if (camundaActivityInstanceResponse.isPresent()) {
                    CamundaActivityInstanceResponse camundaActivityInstance = camundaActivityInstanceResponse.get();

                    String targetActivity = checkTargetActivity(processInstance, activityItems, camundaActivityInstance);
                    if (targetActivity == null) continue;

                    HttpEntity<CamundaProcessInstanceModificationRequest> camundaProcessInstanceModificationRequestHttpEntity = prepareProcessInstanceModificationRequestHttpEntity(formModel, headers, activityInstanceResponse, targetActivity);

                    String path = constructProcessInstanceModificationPath(processInstanceResponse);
                    String url = camundaApiUtils.getUrl(formModel, path);

                    ResponseEntity<CamundaProcessInstanceModificationResponse> camundaProcessInstanceModificationResponseResponse =
                            restTemplate.exchange(url, HttpMethod.POST, camundaProcessInstanceModificationRequestHttpEntity, CamundaProcessInstanceModificationResponse.class);

                    logResponse(processInstance, camundaActivityInstance, camundaProcessInstanceModificationResponseResponse.getStatusCodeValue(), targetActivity);

                    counter++;
                }
            } else {
                camundaApiUtils.logOperationException(tax[0], Constants.PROCESS_DEFINITION_KEY);
            }
        }

        log.info("Done applications moving. " + counter + " successfull operations");
    }

    private String constructProcessInstanceModificationPath(ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse) {
        Optional<CamundaProcessInstanceResponse> camundaProcessInstanceResponse = camundaApiUtils.getObject(processInstanceResponse);
        if (camundaProcessInstanceResponse.isPresent())
            return CamundaApiRoutes.PROCESS_INSTANCE_RESOURCE_PATH + camundaProcessInstanceResponse.get().getId() + "/modification";
        return null;
    }

    private HttpEntity<CamundaProcessInstanceModificationRequest> prepareProcessInstanceModificationRequestHttpEntity(BaseFormModel formModel, HttpHeaders headers, ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse, String targetActivity) {
        List<Map<String, String>> instructions = getInstruction(activityInstanceResponse, targetActivity);
        if (instructions != null) {
            CamundaProcessInstanceModificationRequest camundaProcessInstanceModificationRequest = CamundaProcessInstanceModificationRequest.builder()
                    .skipCustomListeners(false)
                    .skipIoMappings(false)
                    .instructions(instructions)
                    .build();
            return new HttpEntity<>(camundaProcessInstanceModificationRequest, headers);
        }
        return null;
    }

    private List<Map<String, String>> getInstruction(ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse, String targetActivity) {
        Map<String, String> currentActivity = new HashMap<>();
        currentActivity.put("type", "cancel");
        if (activityInstanceResponse.hasBody()) {
            Optional<CamundaActivityInstanceResponse> camundaActivityInstanceResponse = camundaApiUtils.getObject(activityInstanceResponse);
            currentActivity.put("activityId", camundaActivityInstanceResponse.get().getActivityId());
        } else {
            log.warn("Can`t to get current activity ID");
            return null;
        }
        Map<String, String> finalActivity = new HashMap<>();
        finalActivity.put("type", "startBeforeActivity");
        finalActivity.put("activityId", targetActivity);
        return Arrays.asList(currentActivity, finalActivity);
    }

    private void logResponse(CamundaProcessInstanceResponse processInstanceResponse, CamundaActivityInstanceResponse activityInstanceResponse, Integer statusCode, String targetActivity) {
        if (statusCode == 204) {
            log.info("Token of process={}:{} moved from activity={}:{} on activity id={}",
                    processInstanceResponse.getId(),
                    processInstanceResponse.getBusinessKey(),
                    activityInstanceResponse.getActivityId(),
                    activityInstanceResponse.getActivityName(),
                    targetActivity);
        }
    }

    private String checkTargetActivity(CamundaProcessInstanceResponse processInstance, List<String[]> activityIDs, CamundaActivityInstanceResponse camundaActivityInstanceResponse) {
        String targetActivity = getTargetActivity(activityIDs, camundaActivityInstanceResponse);
        if (targetActivity == null) {
            log.info("Process instance {}:{} was skipped, beacause his position are on another activity", processInstance.getId(), processInstance.getBusinessKey());
        }
        return targetActivity;
    }

    private String getTargetActivity(List<String[]> activityIDs, CamundaActivityInstanceResponse camundaActivityInstance) {
        for (String[] activity : activityIDs) {
            String activityId = activity[0].trim();
            String responseActivityId = camundaActivityInstance.getActivityId().trim();
            if (responseActivityId.equals(activityId)) {
                return activity[1].trim();
            }
        }
        return null;
    }
}

package camunda.processmodificator.service.impl;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceModificationRequest;
import camunda.processmodificator.dto.response.CamundaActivityInstanceResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceModificationResponse;
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

import java.util.*;

@Service
@Slf4j
public class ProcessMultipleModificationRestService implements CamundaRestService {

    private final RestTemplate restTemplate;
    private final CamundaApiUtils camundaApiUtils;
    private final FileLoader fileLoader;
    private final IdStorage idStorage;

    public ProcessMultipleModificationRestService(RestTemplate restTemplate,
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

        if (!idStorage.getIds().isEmpty()) {
            List<String[]> activityItems = camundaApiUtils.parse(fileLoader.getActivities());
            int counter = 0;

            for (Map.Entry<String, String> entry : idStorage.getIds().entrySet()) {
                try {
                    String applicationId = entry.getKey();

                    log.info("Contragent ID: " + entry.getValue() + ". Attempt moving his application: " + applicationId + " on stage 22");

                    HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = camundaApiUtils.prepareActivityInstanceRequestHttpEntity(headers, applicationId);

                    ResponseEntity<CamundaActivityInstanceResponse[]> activityInstanceResponse =
                            restTemplate.exchange(camundaApiUtils.getUrl(formModel, CamundaApiRoutes.HISTORY_ACTIVITY_RESOURCE_PATH), HttpMethod.POST, activityInstanceRequestHttpEntity, CamundaActivityInstanceResponse[].class);

                    Optional<CamundaActivityInstanceResponse> camundaActivityInstanceResponse = camundaApiUtils.getObject(activityInstanceResponse);
                    if (camundaActivityInstanceResponse.isPresent()) {
                        CamundaActivityInstanceResponse camundaActivityInstance = camundaActivityInstanceResponse.get();

                        String targetActivity = checkTargetActivity(applicationId, activityItems, camundaActivityInstance);
                        if (targetActivity == null) continue;

                        HttpEntity<CamundaProcessInstanceModificationRequest> camundaProcessInstanceModificationRequestHttpEntity = prepareProcessInstanceModificationRequestHttpEntity(formModel, headers, activityInstanceResponse, targetActivity);

                        String path = constructProcessInstanceModificationPath(applicationId);
                        String url = camundaApiUtils.getUrl(formModel, path);

                        restTemplate.exchange(url, HttpMethod.POST, camundaProcessInstanceModificationRequestHttpEntity, CamundaProcessInstanceModificationResponse.class);

                        logResponse(applicationId);

                        counter++;
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }

            log.info("Applications moving is done!. " + counter + " successfull operations");
        }
    }

    private String constructProcessInstanceModificationPath(String applicationId) {
        return CamundaApiRoutes.PROCESS_INSTANCE_PATH + applicationId + "/modification";
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

    private void logResponse(String applicationId) {
        log.info("Token of process = {} moved!", applicationId);
    }

    private String checkTargetActivity(String applicationId, List<String[]> activityIDs, CamundaActivityInstanceResponse camundaActivityInstanceResponse) {
        String targetActivity = getTargetActivity(activityIDs, camundaActivityInstanceResponse);
        if (targetActivity == null) {
            log.warn("Skipped! Beacause his position are on another activity", applicationId);
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

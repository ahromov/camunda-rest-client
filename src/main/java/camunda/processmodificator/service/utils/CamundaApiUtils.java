package camunda.processmodificator.service.utils;

import camunda.processmodificator.dto.request.CamundaActivityInstanceRequest;
import camunda.processmodificator.dto.request.CamundaProcessInstanceRequest;
import camunda.processmodificator.dto.response.CamundaProcessIncidentsCountResponse;
import camunda.processmodificator.dto.response.CamundaProcessInstanceResponse;
import camunda.processmodificator.model.FormModel;
import camunda.processmodificator.service.routes.CamundaApiRoutes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CamundaApiUtils {

    public void authenticate(HttpHeaders headers, FormModel formModel) {
        if (formModel.getEngineLogin() != null && formModel.getEnginePassword() != null) {
            headers.setBasicAuth(formModel.getEngineLogin(), formModel.getEnginePassword());
        }
    }

    public String getUrl(FormModel formModel, String path) {
        return formModel.getServerAddress() + path;
    }

    public static HttpEntity<CamundaProcessInstanceRequest> prepareProcessInstanceRequestHttpEntity(HttpHeaders headers, String[] tax) {
        CamundaProcessInstanceRequest requestBody = CamundaProcessInstanceRequest.builder()
                .processInstanceBusinessKeyLike("%" + tax[0] + "%")
                .processDefinitionKey("CreditConveyorSmallBusiness")
                .finished(false)
                .build();
        return new HttpEntity<>(requestBody, headers);
    }

    public Boolean isProcessInstanceIncidents(FormModel formModel, HttpHeaders headers, RestTemplate restTemplate, CamundaProcessInstanceResponse processInstanceResponse) {
        String processInstanceId = processInstanceResponse.getId();
        ResponseEntity<CamundaProcessIncidentsCountResponse> processInstanceIncidentsCountResponse =
                restTemplate.exchange(getUrl(formModel, CamundaApiRoutes.PROCESS_INSTANCE_INCIDENTS_COUNT + processInstanceId), HttpMethod.GET, new HttpEntity(headers), CamundaProcessIncidentsCountResponse.class);
        int incidentsCount = processInstanceIncidentsCountResponse.getBody().getCount().intValue();
        if (incidentsCount > 0) {
            log.info("Process instance {}:{} has a {} incidents and was be skipped", processInstanceId, processInstanceResponse.getBusinessKey(), incidentsCount);
            return true;
        }
        return false;
    }

    public HttpEntity<CamundaActivityInstanceRequest> prepareActivityInstanceRequestHttpEntity(HttpHeaders headers, ResponseEntity<CamundaProcessInstanceResponse[]> processInstanceResponse) {
        List<Map<String, String>> sortingParams = getSortingParams();
        String id = getObject(processInstanceResponse).get().getId();
        CamundaActivityInstanceRequest camundaActivityInstanceRequest = CamundaActivityInstanceRequest.builder()
                .processInstanceId(id)
                .sorting(sortingParams)
                .build();
        HttpEntity<CamundaActivityInstanceRequest> activityInstanceRequestHttpEntity = new HttpEntity<>(camundaActivityInstanceRequest, headers);
        return activityInstanceRequestHttpEntity;
    }

    private List<Map<String, String>> getSortingParams() {
        Map<String, String> sortBy = new HashMap<>();
        sortBy.put("sortBy", "endTime");
        Map<String, String> sortOrder = new HashMap<>();
        sortOrder.put("sortOrder", "desc");
        return Arrays.asList(sortBy, sortOrder);
    }

    public <T> Optional<T> getObject(ResponseEntity<T[]> processInstanceResponse) {
        if (processInstanceResponse.getBody().length > 0) {
            return Optional.of(Arrays.stream(processInstanceResponse.getBody()).collect(Collectors.toList()).get(0));
        } else {
            return Optional.empty();
        }
    }
}

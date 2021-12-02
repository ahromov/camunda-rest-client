package camunda.processmodificator.service.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Getter
@Setter
public class IdStorage {

    private Map<String, String> ids = new HashMap<>();
}

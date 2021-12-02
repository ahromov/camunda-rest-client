package camunda.processmodificator.service.utils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Getter()
@Slf4j
public class FileLoader {

    private final String TAXES_FILE = "IPN.txt";
    private final String ACTIMITIES_FILE = "ACTIVITIES.txt";
    private final String TARGET_PROCESS_DEFINITION_ID_FILE = "TARGET_DIFINITION_ID.txt";

    private String targetProcessDefinitionId = null;
    private String activities = null;
    private String ipns = null;

    @PostConstruct
    private void load(){
        try {
            ipns = fromFile(TAXES_FILE);
            activities = fromFile(ACTIMITIES_FILE);
            targetProcessDefinitionId = fromFile(TARGET_PROCESS_DEFINITION_ID_FILE);
        } catch (IOException e) {
            log.error("Connot load file");
        }
    }

    private String fromFile(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n"));
        lines.close();
        return data;
    }
}

package camunda.processmodificator.configuration;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Constants {

    private static final String TAXES_FILE = "IPN.txt";
    private static final String ACTIMITIES_FILE = "ACTIVITIES.txt";
    private static final String TARGET_PROCESS_DEFINITION_ID_FILE = "TARGET_DIFINITION_ID.txt";

    public static final String PROCESS_DEFINITION_KEY = "CreditConveyorSmallBusiness";

    public static String targetProcessDefinitionId = null;
    public static String activities = null;
    public static String ipns = null;

    static {
        try {
            ipns = fromFile(TAXES_FILE);
            activities = fromFile(ACTIMITIES_FILE);
            targetProcessDefinitionId = fromFile(TARGET_PROCESS_DEFINITION_ID_FILE);
        } catch (IOException e) {
            log.error("Connot load file");
        }
    }

    private static String fromFile(String fileName) throws IOException {
        Path path = Paths.get(fileName);

        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n"));
        lines.close();

        return data;
    }
}

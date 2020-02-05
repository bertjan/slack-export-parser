package nl.revolution.slackexportparser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;

public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper(new JsonFactory());

    private int yearToFilterOn;

    public static void main(String[] args) throws IOException {
        new Main().getFilesFromSlackExport(2019, "/path/to/your/slack/export");
    }

    private void getFilesFromSlackExport(int yearToFilterOn, String baseDir) throws IOException {
        this.yearToFilterOn = yearToFilterOn;

        // Walk through all directories in the export root
        Files.walk(Paths.get(baseDir)).filter(Files::isDirectory)
                .map(Path::toString)
                .forEach(this::processDirectory);
    }

    private void processDirectory(String dir) {
        try {
            // Walk through all files in this directory
            Files.walk(Paths.get(dir)).filter(Files::isRegularFile)
                    .map(Path::toString)
                    .forEach(this::processFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processFile(String filePath) {
        try {
            // Process a slack export JSON file
            String json = FileUtils.readFileToString(new File(filePath));
            JsonNode rootNode = MAPPER.readTree(json);

            // Walk through all root level elements
            Iterator<JsonNode> elements = rootNode.elements();
            while (elements.hasNext()) {
                processSlackMessage(elements.next());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processSlackMessage(JsonNode element) {
        // Get uploads (files) from slack message.
        JsonNode filesElem = element.get("files");
        if (filesElem != null) {
            Iterator<JsonNode> files = filesElem.elements();
            while (files.hasNext()) {
                processSlackFile(files.next());
            }
        }
    }

    private void processSlackFile(JsonNode file) {
        if (file.get("timestamp") == null) {
            return;
        }
        long timestamp = file.get("timestamp").asLong();
        String name = file.get("name").asText();

        String downloadUrl = null;
        if (file.get("url_private_download") != null) {
            downloadUrl = file.get("url_private_download").asText();
        } else if (file.get("url_private") != null) {
            downloadUrl = file.get("url_private").asText();
        }
        LocalDate date = Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("CET")).toLocalDate();
        String uniqueName = date + "-" + timestamp + "-" + name;
        int year = date.getYear();
        if (year == yearToFilterOn) {
            // just print the wget commands - let wget to the heavylifting in a terminal session ;-)
            System.out.println("wget -O '" + uniqueName + "' " + downloadUrl);
        }

    }
}

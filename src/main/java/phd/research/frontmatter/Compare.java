package phd.research.frontmatter;

import com.jayway.jsonpath.JsonPath;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.*;

public class Compare {

    JsonObject obj1;
    JsonObject obj2;
    JsonArray diff;

    public Compare(JsonObject obj1, JsonObject obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    private static JsonPatch findDifferences(JsonObject obj1, JsonObject obj2) {
        return Json.createDiff(obj1, obj2);
    }

    private static String format(JsonValue json) {
        StringWriter stringWriter = new StringWriter();
        prettyPrint(json, stringWriter);
        return stringWriter.toString();
    }

    private static void prettyPrint(JsonValue json, Writer writer) {
        Map<String, Object> config = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        try (JsonWriter jsonWriter = writerFactory.createWriter(writer)) {
            jsonWriter.write(json);
        }
    }

    private static String getFormattedPath(String path) {
        List<String> pathList = new ArrayList<>(Arrays.asList(path.substring(1).split("/")));

        StringBuilder builder = new StringBuilder();
        builder.append("$");

        for (String pathItem : pathList) {
            if (isNumeric(pathItem))
                builder.append("[").append(pathItem).append("]");
            else
                builder.append("['").append(pathItem).append("']");
        }

        return builder.toString();
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  // Match a number with optional '-' and decimal.
    }

    private static String getValueAtPath(JsonObject json, String path) {
        Object value = JsonPath.read(json, path);
        return value.toString();
    }

    private static JsonArray removeGuidChanges(JsonArray json) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (JsonValue obj : json) {
            if (!((JsonObject) obj).getString("path").contains("guid")) {
                builder.add(obj);
            }
        }

        return builder.build();
    }

    public void findDiff() {
        this.diff = findDifferences(this.obj1, this.obj2).toJsonArray();
        this.diff = removeGuidChanges(this.diff);
        this.diff = addOldValuesToDiff(this.diff);
    }

    public String getDiffJsonString() {
        return format(this.diff);
    }

    public void writeDiffToFile(String fileName) throws FileNotFoundException {
        OutputStream outputStream = new FileOutputStream(Main.getOutputDirectory() + fileName);
        JsonWriter jsonWriter = Json.createWriter(outputStream);
        jsonWriter.write(this.diff);
    }

    private JsonArray addOldValuesToDiff(JsonArray jsonDiff) {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (JsonValue obj : jsonDiff) {
            String formattedPath = getFormattedPath(((JsonObject) obj).getString("path"));
            String oldValue = getValueAtPath(this.obj1, formattedPath).replace("\"", "");
            JsonObject newObj = getJsonBuilder(((JsonObject) obj)).add("old", oldValue).build();
            builder.add(newObj);
        }

        return builder.build();
    }

    private JsonObjectBuilder getJsonBuilder(JsonObject jsonObject) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }

        return builder;
    }
}

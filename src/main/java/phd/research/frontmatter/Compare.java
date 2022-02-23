package phd.research.frontmatter;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.util.Collections;
import java.util.Map;

public class Compare {

    JsonObject obj1;
    JsonObject obj2;
    JsonPatch diff;

    public Compare(JsonObject obj1, JsonObject obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    public void findDiff() {
        this.diff = findDifferences(this.obj1, this.obj2);
    }

    public String getDiffJsonString() {
        return format(this.diff.toJsonArray());
    }

    public void writeDiffToFile(String fileName) throws FileNotFoundException {
        OutputStream outputStream = new FileOutputStream(Main.getOutputDirectory() + fileName);
        JsonWriter jsonWriter = Json.createWriter(outputStream);
        jsonWriter.write(diff.toJsonArray());
    }

    public static JsonPatch findDifferences(JsonObject obj1, JsonObject obj2) {
        return Json.createDiff(obj1, obj2);
    }

    public static String format(JsonValue json) {
        StringWriter stringWriter = new StringWriter();
        prettyPrint(json, stringWriter);
        return stringWriter.toString();
    }

    public static void prettyPrint(JsonValue json, Writer writer) {
        Map<String, Object> config = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        try (JsonWriter jsonWriter = writerFactory.createWriter(writer)) {
            jsonWriter.write(json);
        }
    }
}

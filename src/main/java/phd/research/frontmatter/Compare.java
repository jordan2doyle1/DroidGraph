package phd.research.frontmatter;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

public class Compare {

    JsonObject obj1;
    JsonObject obj2;

    public Compare(JsonObject obj1, JsonObject obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    public void findDiff() {
        JsonPatch diff = Json.createDiff(obj1, obj2);
        System.out.println(format(diff.toJsonArray()) + "\n");
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

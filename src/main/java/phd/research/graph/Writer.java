package phd.research.graph;

import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.json.JSONExporter;
import phd.research.enums.Format;
import phd.research.jGraph.Vertex;

import java.io.*;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class Writer {

    public Writer() {
    }

    public static void cleanDirectory(String directory) throws IOException {
        FileUtils.cleanDirectory(new File(directory));
    }

    public static void writeGraph(Format format, String directory, String name, Graph<Vertex, DefaultEdge> graph) throws Exception {
        String file = getFileName(name);

        switch (format) {
            case dot:
                exportDOT(directory, file, graph);
                break;
            case json:
                exportJSON(directory, file, graph);
                break;
            case all:
                exportDOT(directory, file, graph);
                exportJSON(directory, file, graph);
                break;
        }
    }

    public static void writeContent(String directory, String name, Set<?> list) throws IOException {
        String outputLocation = directory + "CONTENT/";
        BufferedWriter writer = openFile(outputLocation, getFileName(name) + ".txt");

        if (writer != null) {
            for (Object item : list) {
                writer.write(item.toString() + "\n");
            }

            writer.close();
        }
    }

    private static void exportDOT(String directory, String file, Graph<Vertex, DefaultEdge> graph) throws Exception {
        String outputLocation = directory + "DOT/";
        BufferedWriter writer = openFile(outputLocation, file + ".dot");

        if (writer != null) {
            DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(v -> String.valueOf(v.getID()));
            exporter.setVertexAttributeProvider(Vertex::getAttributes);
            exporter.exportGraph(graph, writer);

            writer.close();

            String command = "dot -T png " + outputLocation + file + ".dot" + " -o " + outputLocation + file + ".png";
            executeCommand(command);
        }
    }

    private static void exportJSON(String directory, String file, Graph<Vertex, DefaultEdge> graph) throws IOException {
        String outputLocation = directory + "JSON/";
        BufferedWriter writer = openFile(outputLocation, file + ".json");

        if (writer != null) {
            JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>(v -> String.valueOf(v.getID()));
            exporter.setVertexAttributeProvider(Vertex::getAttributes);
            exporter.exportGraph(graph, writer);

            writer.close();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static String executeCommand(String command) throws Exception {
        StringBuilder output = new StringBuilder();
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // "Error executing command \"" + command + "\": " + e.getMessage()
        return output.toString();
    }

    private static String getFileName(String name) {
        // Windows does not allow filenames with these characters: / \ : * ? " < > |
        return name.replaceAll(":", "_");
    }

    private static BufferedWriter openFile(String location, String file) throws IOException {
        File directory = new File(location);

        if (!directory.exists()) {
            if (!directory.mkdirs()) return null;
        }

        return new BufferedWriter(new java.io.FileWriter(location + file));
    }
}
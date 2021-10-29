package phd.research.graph;

import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.json.JSONExporter;
import phd.research.core.FrameworkMain;
import phd.research.enums.Format;
import phd.research.jGraph.Vertex;

import java.io.*;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class GraphWriter {

    public GraphWriter() {
    }

    public static void cleanDirectory(String directory) throws IOException {
        FileUtils.cleanDirectory(new File(directory));
    }

    public void writeGraph(Format format, String name, Graph<Vertex, DefaultEdge> graph) throws Exception {
        String file = getFileName(name);

        switch (format) {
            case dot:
                exportDOT(file, graph);
                break;
            case json:
                exportJSON(file, graph);
                break;
            case all:
                exportDOT(file, graph);
                exportJSON(file, graph);
                break;
        }
    }

    public void writeContent(String name, Set<?> list) throws IOException {
        String outputLocation = FrameworkMain.getOutputDirectory() + "CONTENT/";
        BufferedWriter writer = openFile(outputLocation, getFileName(name) + ".txt");

        if (writer != null) {
            for (Object item : list) {
                writer.write(item.toString() + "\n");
            }

            writer.close();
        }
    }

    private void exportDOT(String file, Graph<Vertex, DefaultEdge> graph) throws Exception {
        String outputLocation = FrameworkMain.getOutputDirectory() + "DOT/";
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

    private void exportJSON(String file, Graph<Vertex, DefaultEdge> graph) throws IOException {
        String outputLocation = FrameworkMain.getOutputDirectory() + "JSON/";
        BufferedWriter writer = openFile(outputLocation, file + ".json");

        if (writer != null) {
            JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>(v -> String.valueOf(v.getID()));
            exporter.setVertexAttributeProvider(Vertex::getAttributes);
            exporter.exportGraph(graph, writer);

            writer.close();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private String executeCommand(String command) throws Exception {
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

    private String getFileName(String name) {
        // Windows does not allow filenames with these characters: / \ : * ? " < > |
        return name.replaceAll("[:]", "_");
    }

    private BufferedWriter openFile(String location, String file) throws IOException {
        File directory = new File(location);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                return null;
            }
        }
        return new BufferedWriter(new java.io.FileWriter(location + file));
    }
}
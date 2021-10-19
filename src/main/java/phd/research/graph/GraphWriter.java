package phd.research.graph;

import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.json.JSONExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FrameworkMain;
import phd.research.jGraph.Vertex;

import java.io.*;

/**
 * @author Jordan Doyle
 */
public class GraphWriter {

    private static final Logger logger = LoggerFactory.getLogger(GraphWriter.class);

    private BufferedWriter writer;

    public GraphWriter() {

    }

    public static void cleanDirectory(String directory) {
        try {
            FileUtils.cleanDirectory(new File(directory));
        } catch (IOException e) {
            logger.error("Failed to clean directory: " + e.getMessage());
        }
    }

    public void writeGraph(String format, String name, Graph<Vertex, DefaultEdge> graph) {
        String file = getFileName(name);

        switch (format) {
            case "DOT":
                exportDOT(file, graph);
                break;
            case "JSON":
                exportJSON(file, graph);
                break;
            case "ALL":
                exportDOT(file, graph);
                exportJSON(file, graph);
                break;
            default:
                logger.error("Error Unknown Output Format.");
        }
    }

    private void exportDOT(String file, Graph<Vertex, DefaultEdge> graph) {
        String outputLocation = FrameworkMain.getOutputDirectory() + "DOT/";
        openFile(outputLocation, file + ".dot");

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(v -> String.valueOf(v.getID()));
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        closeFile();

        String command = "dot -T png " + outputLocation + file + ".dot" + " -o " + outputLocation + file + ".png";
        executeCommand(command);
    }

    private void exportJSON(String file, Graph<Vertex, DefaultEdge> graph) {
        String outputLocation = FrameworkMain.getOutputDirectory() + "JSON/";
        openFile(outputLocation, file + ".json");

        JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>(v -> String.valueOf(v.getID()));
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        closeFile();
    }

    @SuppressWarnings("UnusedReturnValue")
    private String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        } catch (Exception e) {
            logger.error("Error executing command \"" + command + "\": " + e.getMessage());
        }
        return output.toString();
    }

    private String getFileName(String name) {
        // Windows does not allow filenames with these characters: / \ : * ? " < > |
        return name.replaceAll("[:]", "_");
    }

    private void openFile(String location, String file) {
        try {
            File directory = new File(location);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    return;
                }
            }
            writer = new BufferedWriter(new java.io.FileWriter(location + file));
        } catch (IOException e) {
            logger.error("Failure opening file: " + e.getMessage());
        }
    }

    private void closeFile() {
        try {
            writer.close();
        } catch (IOException e) {
            logger.error("Failure closing file: " + e.getMessage());
        }
    }
}
package phd.research.graph;

import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FrameworkMain;
import phd.research.jGraph.Vertex;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

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

    public void writeDotGraph(String name, Graph<Vertex, DefaultEdge> graph) {
        String outputLocation = FrameworkMain.getOutputDirectory() + "dot/";
        String file = getFileName(name);

        ComponentNameProvider<Vertex> vertexIdProvider = new ComponentNameProvider<Vertex>() {
            public String getName(Vertex component) {
                return String.valueOf(component.getID());
            }
        };

        ComponentNameProvider<Vertex> vertexLabelProvider = new ComponentNameProvider<Vertex>() {
            public String getName(Vertex component) {
                return component.getLabel();
            }
        };

        ComponentAttributeProvider<Vertex> vertexAttributeProvider = new ComponentAttributeProvider<Vertex>() {
            @Override
            public Map<String, Attribute> getComponentAttributes(Vertex component) {
                HashMap<String, Attribute> attributes = new HashMap<>();
                attributes.put("color", DefaultAttribute.createAttribute(component.getColor().name()));
                attributes.put("shape", DefaultAttribute.createAttribute(component.getShape().name()));
                attributes.put("style", DefaultAttribute.createAttribute(component.getStyle().name()));
                attributes.put("type", DefaultAttribute.createAttribute(component.getType().name()));
                return attributes;
            }
        };

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(vertexIdProvider,
                vertexLabelProvider, null, vertexAttributeProvider, null);
        openFile(outputLocation, file + ".dot");
        exporter.exportGraph(graph, writer);
        closeFile();

        String command = "dot -T png " + outputLocation + file + ".dot" + " -o " + outputLocation + file + ".png";
        executeCommand(command);
    }

    public void writeJSONGraph(String name, Graph<Vertex, DefaultEdge> graph) {
        String outputLocation = FrameworkMain.getOutputDirectory() + "json/";
        String file = getFileName(name) + ".json";

        ComponentNameProvider<Vertex> vertexIdProvider = new ComponentNameProvider<Vertex>() {
            public String getName(Vertex component) {
                return String.valueOf(component.getID());
            }
        };

        ComponentAttributeProvider<DefaultEdge> edgeAttributeProvider = new ComponentAttributeProvider<DefaultEdge>() {
            @Override
            public Map<String, Attribute> getComponentAttributes(DefaultEdge defaultEdge) {
                return new HashMap<>();
            }
        };

        ComponentAttributeProvider<Vertex> vertexAttributeProvider = new ComponentAttributeProvider<Vertex>() {
            @Override
            public Map<String, Attribute> getComponentAttributes(Vertex component) {
                HashMap<String, Attribute> attributes = new HashMap<>();
                attributes.put("label", DefaultAttribute.createAttribute(component.getLabel()));
                attributes.put("color", DefaultAttribute.createAttribute(component.getColor().name()));
                attributes.put("shape", DefaultAttribute.createAttribute(component.getShape().name()));
                attributes.put("style", DefaultAttribute.createAttribute(component.getStyle().name()));
                attributes.put("type", DefaultAttribute.createAttribute(component.getType().name()));
                return attributes;
            }
        };

        try {
            JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>(vertexIdProvider, vertexAttributeProvider,
                    null, edgeAttributeProvider);
            openFile(outputLocation, file);
            exporter.exportGraph(graph, writer);
        } catch (ExportException e) {
            logger.error("Failed to export graph as JSON file: " + e.getMessage());
        }
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
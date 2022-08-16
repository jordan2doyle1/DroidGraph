package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.json.JSONExporter;
import phd.research.enums.Format;
import phd.research.vertices.Vertex;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.io.*;
import java.util.Collection;

/**
 * @author Jordan Doyle
 */

public class Writer {

    private static final String CONTENT_DIRECTORY_NAME = "CONTENT";
    private static final String DOT_DIRECTORY = "DOT";
    private static final String DOT_EXTENSION = ".dot";
    private static final String PNG_EXTENSION = ".png";
    private static final String JSON_DIRECTORY = "JSON";
    private static final String JSON_EXTENSION = ".json";


    public static void writeGraph(Format format, File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException, InterruptedException {
        switch (format) {
            case dot:
                exportDOT(directory, fileName, graph);
                break;
            case json:
                exportJSON(directory, fileName, graph);
                break;
            case all:
                exportDOT(directory, fileName, graph);
                exportJSON(directory, fileName, graph);
                break;
        }
    }

    private static void createFile(File file) throws IOException {
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IOException("Failed to create directory: " + file.getParentFile());
            }
        }

        if (!file.createNewFile()) {
            if (!file.exists()) {
                throw new IOException("Failed to create output file: " + file);
            }
        }
    }

    public static void writeContent(File directory, String fileName, Collection<?> collection) throws IOException {
        File outputFile = new File(directory + File.separator + CONTENT_DIRECTORY_NAME + File.separator + fileName);
        createFile(outputFile);

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
        for (Object item : collection) {
            writer.write(item.toString() + "\n");
        }
        writer.close();
    }

    public static void outputMethods(File directory, Format format) throws Exception {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (Filter.isValidClass(null, null, sootClass)) {
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String name = sootClass.getName().substring(sootClass.getName().lastIndexOf(".") + 1) + "_" +
                                method.getName();

                        name = name.replaceAll(":", "_");
                        Writer.writeGraph(format, directory, name, unitGraph.getGraph());
                    }
                }
            }
        }
    }

    private static void exportDOT(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException, InterruptedException {
        File dotFile = new File(directory + File.separator + DOT_DIRECTORY + File.separator + fileName + DOT_EXTENSION);
        createFile(dotFile);
        BufferedWriter writer = new BufferedWriter(new FileWriter(dotFile));

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(v -> String.valueOf(v.hashCode()));
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();

        File pngFile = new File(directory + File.separator + DOT_DIRECTORY + File.separator + fileName + PNG_EXTENSION);
        String command = "dot -T png " + dotFile + " -o " + pngFile;
        executeCommand(command);
    }

    private static void exportJSON(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        File jsonFile =
                new File(directory + File.separator + JSON_DIRECTORY + File.separator + fileName + JSON_EXTENSION);
        createFile(jsonFile);
        BufferedWriter writer = new BufferedWriter(new FileWriter(jsonFile));

        JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>(v -> String.valueOf(v.hashCode()));
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();
    }

    @SuppressWarnings("UnusedReturnValue")
    private static String executeCommand(String command) throws IOException, InterruptedException {
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
}
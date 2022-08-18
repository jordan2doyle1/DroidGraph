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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Jordan Doyle
 */

public class Writer {

    public static void writeGraph(Format format, File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
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

    public static void writeString(File directory, String fileName, String content) throws IOException {
        File file = new File(directory + File.separator + fileName);
        createFile(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(content);
        writer.close();
    }

    public static void writeCollection(File directory, String fileName, Collection<?> collection) throws IOException {
        File file = new File(directory + File.separator + fileName);
        createFile(file);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        for (Object item : collection) {
            writer.write(item.toString() + "\n");
        }
        writer.close();
    }

    public static void outputMethods(File directory, Format format) throws IOException {
        for (SootClass clazz : Scene.v().getClasses()) {
            if (Filter.isValidClass(clazz)) {
                for (SootMethod method : clazz.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String fileName = clazz.getShortName() + "_" + method.getName();
                        Writer.writeGraph(format, directory, fileName, unitGraph.getGraph());
                    }
                }
            }
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

    private static void exportDOT(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        File file = new File(directory + File.separator + "DOT" + File.separator + fileName + ".dot");
        createFile(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>(v -> String.valueOf(v.hashCode()));
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();
    }

    private static void exportJSON(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        File file = new File(directory + File.separator + "JSON" + File.separator + fileName + ".json");
        createFile(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>(v -> String.valueOf(v.hashCode()));
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();
    }
}
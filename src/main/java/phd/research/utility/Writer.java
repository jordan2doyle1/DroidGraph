package phd.research.utility;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.dot.DOTExporter;
import org.jgrapht.nio.gml.GmlExporter;
import org.jgrapht.nio.json.JSONExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.enums.Format;
import phd.research.vertices.Vertex;
import soot.util.MultiMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Jordan Doyle
 */

public class Writer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Writer.class);

    public static void writeGraph(File directory, String fileName, Format format, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        switch (format) {
            case DOT:
                exportDOT(directory, fileName, graph);
                break;
            case JSON:
                exportJSON(directory, fileName, graph);
                break;
            case GML:
                exportGML(directory, fileName, graph);
                break;
            case ALL:
                exportDOT(directory, fileName, graph);
                exportJSON(directory, fileName, graph);
                exportGML(directory, fileName, graph);
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
        writer.write("Found " + collection.size() + " item(s).\n\n");
        for (Object item : collection) {
            writer.write(item.toString() + "\n");
        }
        writer.close();
    }

    public static void writeMap(File directory, String fileName, Map<?, ?> map) throws IOException {
        File file = new File(directory + File.separator + fileName);
        createFile(file);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write("Found " + map.size() + " item(s).\n\n");
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            writer.write(entry.getKey().toString() + ": " + entry.getValue().toString() + "\n");
        }
        writer.close();
    }

    public static void writeMultiMap(File directory, String fileName, MultiMap<?, ?> map) throws IOException {
        List<String> lines = new ArrayList<>();
        map.forEach(pair -> lines.add(pair.getO1().toString() + ": " + pair.getO2().toString()));
        Writer.writeCollection(directory, fileName, lines);
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

        LOGGER.info("Created file '" + file.getAbsolutePath() + "'.");
    }

    private static void exportDOT(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        File file = new File(directory + File.separator + fileName + ".dot");
        createFile(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        DOTExporter<Vertex, DefaultEdge> exporter = new DOTExporter<>();
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();
    }

    private static void exportJSON(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        File file = new File(directory + File.separator + fileName + ".json");
        createFile(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        JSONExporter<Vertex, DefaultEdge> exporter = new JSONExporter<>();
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();
    }

    //TODO: Exported GML graphs do not include the vertex attributes, only the vertex ID.
    private static void exportGML(File directory, String fileName, Graph<Vertex, DefaultEdge> graph)
            throws IOException {
        File file = new File(directory + File.separator + fileName + ".gml");
        createFile(file);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        GmlExporter<Vertex, DefaultEdge> exporter = new GmlExporter<>();
        exporter.setVertexAttributeProvider(Vertex::getAttributes);
        exporter.exportGraph(graph, writer);

        writer.close();
    }
}
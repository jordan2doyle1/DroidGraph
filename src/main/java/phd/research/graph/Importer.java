package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.nio.gml.GmlImporter;
import org.jgrapht.nio.json.JSONImporter;
import org.jgrapht.util.SupplierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Pair;
import phd.research.core.DroidGraph;
import phd.research.helper.BytecodeConverter;
import phd.research.vertices.MethodVertex;
import phd.research.vertices.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

public class Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DroidGraph.class);

    public static Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> importGML(File graphFile) {
        Graph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        GmlImporter<String, DefaultEdge> importer = new GmlImporter<>();
        Map<String, Map<String, Attribute>> attributes = new HashMap<>();
        importer.addVertexAttributeConsumer(attributeConsumer(attributes));
        importer.importGraph(graph, graphFile);

        return new Pair<>(graph, attributes);
    }

    public static Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> importDOT(File graphFile) {
        Graph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        DOTImporter<String, DefaultEdge> importer = new DOTImporter<>();
        Map<String, Map<String, Attribute>> attributes = new HashMap<>();
        importer.addVertexAttributeConsumer(attributeConsumer(attributes));
        importer.importGraph(graph, graphFile);

        return new Pair<>(graph, attributes);
    }

    public static Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> importJSON(File graphFile) {
        Graph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        JSONImporter<String, DefaultEdge> importer = new JSONImporter<>();
        Map<String, Map<String, Attribute>> attributes = new HashMap<>();
        importer.addVertexAttributeConsumer(attributeConsumer(attributes));
        importer.importGraph(graph, graphFile);

        return new Pair<>(graph, attributes);
    }

    private static BiConsumer<org.jgrapht.alg.util.Pair<String, String>, Attribute> attributeConsumer(
            Map<String, Map<String, Attribute>> attributes) {
        return (p, a) -> {
            Map<String, Attribute> map = attributes.computeIfAbsent(p.getFirst(), k -> new HashMap<>());
            map.put(p.getSecond(), a);
        };
    }


    public static Graph<Vertex, DefaultEdge> importAndroGuardCallGraph(File graphFile) {
        Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> tempImport =
                Importer.importGML(graphFile);
        Graph<String, DefaultEdge> tempGraph = tempImport.getLeft();
        Map<String, Map<String, Attribute>> attributes = tempImport.getRight();

        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        tempGraph.vertexSet().forEach(v -> {
            boolean internal = attributes.get(v).get("external").getValue().equals("0");
            if (internal) {
                Vertex vertex = Importer.convertBytecodeLabelToVertex(attributes.get(v).get("label").getValue());
                if (vertex != null) {
                    graph.addVertex(vertex);
                }
            }
        });
        tempGraph.edgeSet().forEach(e -> {
            boolean internal =
                    attributes.get(tempGraph.getEdgeSource(e)).get("external").getValue().equals("0") &&
                            attributes.get(tempGraph.getEdgeTarget(e)).get("external").getValue().equals("0");
            if (internal) {
                Vertex src = Importer.convertBytecodeLabelToVertex(
                        attributes.get(tempGraph.getEdgeSource(e)).get("label").getValue());
                Vertex tgt = Importer.convertBytecodeLabelToVertex(
                        attributes.get(tempGraph.getEdgeTarget(e)).get("label").getValue());
                if (src != null && tgt != null) {
                    graph.addEdge(src, tgt);
                }
            }
        });

        return graph;
    }

    public static Graph<Vertex, DefaultEdge> importDotDroidGraph(File graphFile) {
        return new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    public static Graph<Vertex, DefaultEdge> importJsonDroidGraph(File graphFile) {
        return new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    private static Graph<Vertex, DefaultEdge> importGmlDroidGraph(File graphFile) {
        return new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    private static Vertex convertBytecodeLabelToVertex(String label) throws RuntimeException {
        String methodSignature = BytecodeConverter.signatureToJimple(label);
        SootClass clazz = Scene.v().getSootClass(Scene.signatureToClass(methodSignature));
        if (Filter.isValidClass(clazz)) {
            SootMethod method = Scene.v().grabMethod(methodSignature);
            if (method == null) {
                LOGGER.warn("Failed to find method: " + methodSignature);
                return null;
            }

            return MethodVertex.createMethodVertex(method);
        }

        return null;
    }
}

package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.GraphImporter;
import org.jgrapht.nio.dot.DOTImporter;
import org.jgrapht.nio.gml.GmlImporter;
import org.jgrapht.nio.json.JSONImporter;
import org.jgrapht.util.SupplierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Pair;
import phd.research.core.DroidGraph;
import phd.research.enums.Format;
import phd.research.enums.Type;
import phd.research.helper.BytecodeConverter;
import phd.research.vertices.MethodVertex;
import phd.research.vertices.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DroidGraph.class);

    public static Graph<Vertex, DefaultEdge> importDroidGraph(Format format, File graphFile) throws RuntimeException {
        Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> graphImport =
                Importer.importGraph(format, graphFile);
        return Importer.convertStringGraphToVertexGraph(graphImport.getLeft(), graphImport.getRight());
    }

    public static Graph<Vertex, DefaultEdge> importAndroGuardCallGraph(File graphFile) {
        Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> graphImport =
                Importer.importGraph(Format.GML, graphFile);
        Graph<String, DefaultEdge> stringGraph = graphImport.getLeft();
        Map<String, Map<String, Attribute>> attributes = graphImport.getRight();

        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        stringGraph.vertexSet().forEach(stringVertex -> {
            boolean internal = attributes.get(stringVertex).get("external").getValue().equals("0");
            if (internal) {
                String label = attributes.get(stringVertex).get("label").getValue();
                Vertex vertex = Importer.convertBytecodeLabelToVertex(label);
                if (vertex != null) {
                    graph.addVertex(vertex);
                }
            }
        });
        stringGraph.edgeSet().forEach(edge -> {
            boolean internal = attributes.get(stringGraph.getEdgeSource(edge)).get("external").getValue().equals("0") &&
                    attributes.get(stringGraph.getEdgeTarget(edge)).get("external").getValue().equals("0");
            if (internal) {
                String srcLabel = attributes.get(stringGraph.getEdgeSource(edge)).get("label").getValue();
                Vertex src = Importer.convertBytecodeLabelToVertex(srcLabel);
                String tgtLabel = attributes.get(stringGraph.getEdgeTarget(edge)).get("label").getValue();
                Vertex tgt = Importer.convertBytecodeLabelToVertex(tgtLabel);
                if (src != null && tgt != null) {
                    graph.addEdge(src, tgt);
                }
            }
        });

        return graph;
    }

    // TODO: Imported JSON graph doesn't have the same number of vertices or edges as the origin graph.
    private static Pair<Graph<String, DefaultEdge>, Map<String, Map<String, Attribute>>> importGraph(Format format,
            File graphFile) throws RuntimeException {
        Graph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        GraphImporter<String, DefaultEdge> importer;
        Map<String, Map<String, Attribute>> attributes = new HashMap<>();
        switch (format) {
            case DOT:
                importer = new DOTImporter<>();
                ((DOTImporter<String, DefaultEdge>) importer).addVertexAttributeConsumer(attributeConsumer(attributes));
                break;
            case JSON:
                importer = new JSONImporter<>();
                ((JSONImporter<String, DefaultEdge>) importer).addVertexAttributeConsumer(
                        attributeConsumer(attributes));
                break;
            case GML:
                importer = new GmlImporter<>();
                ((GmlImporter<String, DefaultEdge>) importer).addVertexAttributeConsumer(attributeConsumer(attributes));
                break;
            case ALL:
                throw new RuntimeException("Unsupported format: " + format);
            default:
                throw new RuntimeException("Unrecognised format: " + format);
        }

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

    private static Graph<Vertex, DefaultEdge> convertStringGraphToVertexGraph(Graph<String, DefaultEdge> stringGraph,
            Map<String, Map<String, Attribute>> attributes) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        stringGraph.vertexSet().forEach(stringVertex -> {
            Vertex vertex = Importer.convertImportAttributesToVertex(attributes.get(stringVertex));
            graph.addVertex(vertex);
        });
        stringGraph.edgeSet().forEach(edge -> {
            Vertex src = Importer.convertImportAttributesToVertex(attributes.get(stringGraph.getEdgeSource(edge)));
            Vertex tgt = Importer.convertImportAttributesToVertex(attributes.get(stringGraph.getEdgeTarget(edge)));
            graph.addEdge(src, tgt);
        });

        return graph;
    }

    // TODO: Need to return proper ControlVertex and UnitVertex objects.
    private static Vertex convertImportAttributesToVertex(Map<String, Attribute> attributes) throws RuntimeException {
        Attribute attr = attributes.get("type");
        String type = attr.getValue();
        Type vertexType = Type.valueOf(type);
        switch (vertexType) {
            case METHOD:
            case LISTENER:
            case LIFECYCLE:
            case CALLBACK:
            case DUMMY:
                String methodSignature = attributes.get("method").getValue();
                SootMethod method = Scene.v().grabMethod(methodSignature);
                if (method == null) {
                    throw new RuntimeException("Failed to find method: " + methodSignature);
                } else {
                    return new MethodVertex(method);
                }
            case CONTROL:
                return new Vertex(Type.CONTROL, attributes.get("label").getValue());
            case UNIT:
                return new Vertex(Type.UNIT, attributes.get("label").getValue());
            default:
                throw new RuntimeException("Unrecognised type: " + vertexType);
        }
    }

    private static Vertex convertBytecodeLabelToVertex(String label) throws NullPointerException {
        String methodSignature = BytecodeConverter.signatureToJimple(label);
        SootClass clazz = Scene.v().getSootClass(Scene.signatureToClass(methodSignature));
        if (Filter.isValidClass(clazz)) {
            SootMethod method = Scene.v().grabMethod(methodSignature);
            if (method == null) {
                LOGGER.warn("Failed to find method signature: " + methodSignature);
            } else {
                return MethodVertex.createMethodVertex(method);
            }
        }
        return null;
    }
}

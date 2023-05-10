package phd.research.utility;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.gml.GmlImporter;
import org.jgrapht.nio.json.JSONImporter;
import org.jgrapht.util.SupplierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.singletons.FlowDroidAnalysis;
import phd.research.vertices.AndroGuardVertex;
import phd.research.vertices.Vertex;
import phd.research.vertices.VertexFactory;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author Jordan Doyle
 */

public class Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

    public static Graph<AndroGuardVertex, DefaultEdge> importAndroGuardGraph(File graphFile) {
        LOGGER.info("Importing AndroGuard call graph from " + graphFile);

        Graph<Integer, DefaultEdge> tempGraph =
                new DefaultDirectedGraph<>(SupplierUtil.createIntegerSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        GmlImporter<Integer, DefaultEdge> importer = new GmlImporter<>();
        Map<Integer, Map<String, Attribute>> attributes = new HashMap<>();
        importer.addVertexAttributeConsumer(createAttributeConsumer(attributes));
        importer.importGraph(tempGraph, graphFile);

        Graph<AndroGuardVertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Warning: Only looping edges, isolated vertices (no edges) will be excluded.
        tempGraph.edgeSet().forEach(edgeId -> {
            Map<String, Attribute> sourceAttributes = attributes.get(tempGraph.getEdgeSource(edgeId));
            AndroGuardVertex sourceVertex =
                    new AndroGuardVertex(tempGraph.getEdgeSource(edgeId), sourceAttributes.get("label").getValue(),
                            "1".equals(sourceAttributes.get("external").getValue()),
                            "1".equals(sourceAttributes.get("entrypoint").getValue())
                    );
            graph.addVertex(sourceVertex);

            Map<String, Attribute> targetAttributes = attributes.get(tempGraph.getEdgeTarget(edgeId));
            AndroGuardVertex targetVertex =
                    new AndroGuardVertex(tempGraph.getEdgeTarget(edgeId), targetAttributes.get("label").getValue(),
                            "1".equals(targetAttributes.get("external").getValue()),
                            "1".equals(targetAttributes.get("entrypoint").getValue())
                    );
            graph.addVertex(targetVertex);

            graph.addEdge(sourceVertex, targetVertex);
        });

        LOGGER.info("AndroGuard call graph contains " + graph.vertexSet().size() + " vertices and " +
                graph.edgeSet().size() + " edges.");
        return graph;
    }

    public static Graph<Vertex, DefaultEdge> convertAndFilterAndroGuardGraph(
            Graph<AndroGuardVertex, DefaultEdge> androGuardGraph) {
        LOGGER.info("Converting and filtering AndroGuard call graph...");

        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        // Warning: Only looping edges, isolated vertices (vertex with no edge) will be excluded.
        androGuardGraph.edgeSet().forEach(edge -> {
            Vertex source = null;
            AndroGuardVertex sourceAndroGuardVertex = androGuardGraph.getEdgeSource(edge);
            if (!sourceAndroGuardVertex.isExternal()) {
                source = Importer.convertVertex(sourceAndroGuardVertex);
                if (source != null) {
                    graph.addVertex(source);
                }
            }

            Vertex target = null;
            AndroGuardVertex targetAndroGuardVertex = androGuardGraph.getEdgeTarget(edge);
            if (!targetAndroGuardVertex.isExternal()) {
                target = Importer.convertVertex(targetAndroGuardVertex);
                if (target != null) {
                    graph.addVertex(target);
                }
            }
            boolean external = sourceAndroGuardVertex.isExternal() || targetAndroGuardVertex.isExternal();
            if (!external) {
                if (source != null && target != null) {
                    graph.addEdge(source, target);
                }
            }
        });

        LOGGER.info("Converted and filtered call graph contains " + graph.vertexSet().size() + " vertices and " +
                graph.edgeSet().size() + " edges.");
        return graph;
    }

    public static Graph<Vertex, DefaultEdge> importDroidGraph(File graphFile) throws RuntimeException {
        LOGGER.info("Importing control flow graph from " + graphFile);

        Graph<Integer, DefaultEdge> tempGraph =
                new DefaultDirectedGraph<>(SupplierUtil.createIntegerSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        JSONImporter<Integer, DefaultEdge> importer = new JSONImporter<>();
        Map<Integer, Map<String, Attribute>> attributes = new HashMap<>();
        importer.addVertexAttributeConsumer(createAttributeConsumer(attributes));
        importer.importGraph(tempGraph, graphFile);

        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        VertexFactory factory = new VertexFactory();

        // Loop through vertices first so that isolated vertices (vertex with no edge) are included in the graph.
        tempGraph.vertexSet().forEach(vertexId -> {
            Vertex sourceVertex = factory.createVertex(vertexId, attributes.get(vertexId));
            graph.addVertex(sourceVertex);
        });
        tempGraph.edgeSet().forEach(edgeId -> {
            int sourceId = tempGraph.getEdgeSource(edgeId);
            Vertex sourceVertex = factory.createVertex(sourceId, attributes.get(sourceId));
            graph.addVertex(sourceVertex);
            int targetId = tempGraph.getEdgeTarget(edgeId);
            Vertex targetVertex = factory.createVertex(targetId, attributes.get(targetId));
            graph.addVertex(targetVertex);
            graph.addEdge(sourceVertex, targetVertex);
        });

        LOGGER.info("Imported control flow graph contains " + graph.vertexSet().size() + " vertices and " +
                graph.edgeSet().size() + " edges.");
        return graph;
    }

    private static BiConsumer<Pair<Integer, String>, Attribute> createAttributeConsumer(
            Map<Integer, Map<String, Attribute>> attr) {
        return (p, a) -> {
            Map<String, Attribute> map = attr.computeIfAbsent(p.getFirst(), k -> new HashMap<>());
            map.put(p.getSecond(), a);
        };
    }

    private static Vertex convertVertex(AndroGuardVertex androGuardVertex) {
        if (androGuardVertex.getJimpleSignature() == null) {
            LOGGER.warn("Bytecode not converted to Jimple signature: " + androGuardVertex.getBytecodeSignature());
            return null;
        }

        if (!FlowDroidAnalysis.v().isSootInitialised()) {
            FlowDroidAnalysis.v().initializeSoot();
        }

        VertexFactory factory = new VertexFactory();
        SootClass clazz = Scene.v().getSootClass(Scene.signatureToClass(androGuardVertex.getJimpleSignature()));
        if (Filter.isValidClass(clazz)) {
            SootMethod method = Scene.v().grabMethod(androGuardVertex.getJimpleSignature());
            if (method == null) {
                for (SootMethod classMethod : clazz.getMethods()) {
                    String methodSignature = classMethod.getSignature().replace("'", "");
                    if (androGuardVertex.getJimpleSignature().equals(methodSignature)) {
                        method = classMethod;
                        break;
                    }
                }
            }

            if (method == null) {
                LOGGER.warn("Failed to find method signature: " + androGuardVertex.getJimpleSignature());
            } else if (Filter.isValidMethod(method)) {
                return factory.createVertex(androGuardVertex.getId(), method);
            }
        }

        return null;
    }
}

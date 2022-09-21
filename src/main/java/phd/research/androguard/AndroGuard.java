package phd.research.androguard;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.gml.GmlImporter;
import org.jgrapht.util.SupplierUtil;
import phd.research.enums.Type;
import phd.research.vertices.Vertex;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AndroGuard {

    private final File androGuardGraphFile;
    private Graph<Vertex, DefaultEdge> graph;

    public AndroGuard(File file) {
        this.androGuardGraphFile = file;
        this.graph = importGMLGraph(this.androGuardGraphFile);
    }

    public Graph<Vertex, DefaultEdge> getCallGraph() {
        if (this.graph == null) {
            this.graph = importGMLGraph(this.androGuardGraphFile);
        }

        return this.graph;
    }

    private Graph<Vertex, DefaultEdge> importGMLGraph(File gmlFile) {
        Graph<String, DefaultEdge> tempGraph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        // Import the graph; the vertex names are stored as 'ID' and the label stores as 'label' in the attribute map.
        GmlImporter<String, DefaultEdge> importer = new GmlImporter<>();
        Map<String, Map<String, Attribute>> attrs = new HashMap<>();
        importer.addVertexAttributeConsumer((p, a) -> {
            Map<String, Attribute> map = attrs.computeIfAbsent(p.getFirst(), k -> new HashMap<>());
            map.put(p.getSecond(), a);
        });
        importer.importGraph(tempGraph, gmlFile);

        // Create a new graph, thereby creating String vertices equal to the label attribute values.
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (String v : tempGraph.vertexSet()) {
            graph.addVertex(new Vertex(Type.other, attrs.get(v).get("label").getValue()));
        }
        for (DefaultEdge e : tempGraph.edgeSet()) {
            String source = tempGraph.getEdgeSource(e);
            String target = tempGraph.getEdgeTarget(e);
            Vertex sourceID = new Vertex(Type.other, attrs.get(source).get("label").getValue());
            Vertex targetID = new Vertex(Type.other, attrs.get(target).get("label").getValue());
            graph.addEdge(sourceID, targetID);
        }

        System.out.println(graph);
        return graph;
    }

    public void outputGMLGraph(File outputDirectory) {
        // TODO: Implement
    }
}

package phd.research.jGraph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
@SuppressWarnings("unused")
public class JGraph {

    private final Set<Edge> edges;
    private Set<Vertex> vertices;

    public JGraph() {
        vertices = new HashSet<>();
        edges = new HashSet<>();
    }

    public static JGraph convert(Graph<Vertex, DefaultEdge> graph) {
        JGraph jGraph = new JGraph();
        jGraph.vertices = graph.vertexSet();

        for (DefaultEdge edge : graph.edgeSet()) {
            jGraph.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
        }

        return jGraph;
    }

    public Set<Vertex> vertexSet() {
        return this.vertices;
    }

    public void addVertex(Vertex vertex) {
        this.vertices.add(vertex);
    }

    public Vertex getVertex(int id) {
        for (Vertex vertex : this.vertices) {
            if (vertex.getID() == id)
                return vertex;
        }
        return null;
    }

    public boolean contains(Vertex vertex) {
        return this.vertices.contains(vertex);
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
    }

    public void addEdge(Vertex source, Vertex target) {
        Edge edge = new Edge(source, target);
        addEdge(edge);
    }

    public boolean contains(Edge edge) {
        return this.edges.contains(edge);
    }

    public void addGraph(JGraph subGraph) {
        this.vertices.addAll(subGraph.vertices);
        this.edges.addAll(subGraph.edges);
    }
}

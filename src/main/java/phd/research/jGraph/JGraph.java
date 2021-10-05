package phd.research.jGraph;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class JGraph {

    private final Set<Vertex> vertices;
    private final Set<Edge> edges;

    public JGraph() {
        vertices = new HashSet<>();
        edges = new HashSet<>();
    }

    public Set<Vertex> vertexSet() {
        return this.vertices;
    }

    public Set<Edge> edgeSet() {
        return this.edges;
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

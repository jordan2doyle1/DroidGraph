package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.vertices.UnitVertex;
import phd.research.vertices.Vertex;
import soot.Body;
import soot.Unit;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jordan Doyle
 */

public class UnitGraph extends BriefUnitGraph {

    private Graph<Vertex, DefaultEdge> graph;

    public UnitGraph(Body body) {
        super(body);
    }

    public Graph<Vertex, DefaultEdge> getGraph() {
        if (this.graph == null) {
            this.graph = generateGraph();
        }

        return this.graph;
    }

    public Set<Vertex> getRoots() {
        Set<Vertex> rootVertices = new HashSet<>();

        if (this.graph == null) {
            this.graph = generateGraph();
        }

        for (Vertex vertex : this.graph.vertexSet()) {
            if (this.graph.inDegreeOf(vertex) == 0) {
                rootVertices.add(vertex);
            }
        }

        return rootVertices;
    }

    private Graph<Vertex, DefaultEdge> generateGraph() {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Unit unit : super.unitChain) {
            UnitVertex vertex = new UnitVertex(unit);
            graph.addVertex(vertex);

            List<Unit> successors = super.getSuccsOf(unit);
            for (Unit nextUnit : successors) {
                UnitVertex nextVertex = new UnitVertex(nextUnit);
                graph.addVertex(nextVertex);
                graph.addEdge(vertex, nextVertex);
            }
        }

        return graph;
    }
}
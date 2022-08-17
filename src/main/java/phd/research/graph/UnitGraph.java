package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.vertices.UnitVertex;
import phd.research.vertices.Vertex;
import soot.Body;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.Collection;
import java.util.stream.Collectors;

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

    public Collection<Vertex> getRoots() {
        if (this.graph == null) {
            this.graph = generateGraph();
        }

        return this.graph.vertexSet().stream().filter(v -> this.graph.inDegreeOf(v) == 0).collect(Collectors.toSet());
    }

    private Graph<Vertex, DefaultEdge> generateGraph() {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        super.unitChain.forEach(unit -> {
            UnitVertex vertex = new UnitVertex(unit);
            graph.addVertex(vertex);

            super.getSuccsOf(unit).forEach(nextUnit -> {
                UnitVertex nextVertex = new UnitVertex(nextUnit);
                graph.addVertex(nextVertex);
                graph.addEdge(vertex, nextVertex);
            });
        });

        return graph;
    }
}
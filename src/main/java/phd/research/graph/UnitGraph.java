package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.vertices.UnitVertex;
import phd.research.vertices.Vertex;
import soot.Body;
import soot.Unit;
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

        for (Unit unit : super.unitChain) {
            UnitVertex vertex = new UnitVertex(super.method.getSignature(), unit.toString());
            boolean found = false;
            for (Vertex v : graph.vertexSet()) {
                UnitVertex unitVertex = (UnitVertex) v;
                if (unitVertex.equals(vertex)) {
                    vertex = unitVertex;
                    found = true;
                    break;
                }
            }

            if (!found) {
                graph.addVertex(vertex);
            }

            for (Unit nextUnit : super.getSuccsOf(unit)) {
                UnitVertex nextVertex = new UnitVertex(super.method.getSignature(), nextUnit.toString());
                found = false;
                for (Vertex v : graph.vertexSet()) {
                    UnitVertex unitVertex = (UnitVertex) v;
                    if (unitVertex.equals(nextVertex)) {
                        nextVertex = unitVertex;
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    graph.addVertex(nextVertex);
                }

                graph.addEdge(vertex, nextVertex);
            }
        }

        return graph;
    }
}
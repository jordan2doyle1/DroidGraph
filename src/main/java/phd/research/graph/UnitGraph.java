package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.enums.Type;
import phd.research.jGraph.JGraph;
import phd.research.jGraph.Vertex;
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

    private Graph<Vertex, DefaultEdge> jUnitGraphT;
    private JGraph jUnitGraph;

    public UnitGraph(Body body) {
        super(body);
    }

    public Graph<Vertex, DefaultEdge> getJUnitGraphT() {
        if (jUnitGraphT == null) {
            generateJGraphT();
        }
        return this.jUnitGraphT;
    }

    public JGraph getJUnitGraph() {
        if (jUnitGraph == null) {
            generateJGraph();
        }
        return this.jUnitGraph;
    }

    public Set<Vertex> getRoots() {
        Set<Vertex> rootVertices = new HashSet<>();

        if (jUnitGraphT == null) {
            generateJGraphT();
        }

        for (Vertex vertex : this.jUnitGraphT.vertexSet()) {
            if (this.jUnitGraphT.inDegreeOf(vertex) == 0) {
                rootVertices.add(vertex);
            }
        }

        return rootVertices;
    }

    private void generateJGraphT() {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Unit unit : super.unitChain) {
            Vertex vertex = new Vertex(unit.hashCode(), unit.toString(), Type.statement);
            graph.addVertex(vertex);

            List<Unit> successors = super.getSuccsOf(unit);
            for (Unit nextUnit : successors) {
                Vertex nextVertex = new Vertex(nextUnit.hashCode(), nextUnit.toString(), Type.statement);
                graph.addVertex(nextVertex);
                graph.addEdge(vertex, nextVertex);
            }
        }

        this.jUnitGraphT = graph;
    }

    private void generateJGraph() {
        JGraph graph = new JGraph();

        for (Unit unit : super.unitChain) {
            Vertex vertex = new Vertex(unit.hashCode(), unit.toString(), Type.statement);
            graph.addVertex(vertex);

            List<Unit> successors = super.getSuccsOf(unit);
            for (Unit nextUnit : successors) {
                Vertex nextVertex = new Vertex(nextUnit.hashCode(), nextUnit.toString(), Type.statement);
                graph.addVertex(nextVertex);
                graph.addEdge(vertex, nextVertex);
            }
        }

        this.jUnitGraph = graph;
    }
}
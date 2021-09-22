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

    private final String name;
    private final Graph<Vertex, DefaultEdge> jUnitGraphT;
    private final JGraph jUnitGraph;

    public UnitGraph(Body body) {
        super(body);
        this.name = generateGraphName();
        this.jUnitGraphT = generateJGraphT();
        this.jUnitGraph = generateJGraph();
    }

    public Graph<Vertex, DefaultEdge> getJUnitGraphT() {
        return this.jUnitGraphT;
    }

    public JGraph getJUnitGraph() {
        return this.jUnitGraph;
    }

    public Set<Vertex> getRoots() {
        Set<Vertex> rootVertices = new HashSet<>();

        for (Vertex vertex : this.jUnitGraphT.vertexSet()) {
            if (this.jUnitGraphT.inDegreeOf(vertex) == 0) {
                rootVertices.add(vertex);
            }
        }

        return rootVertices;
    }

    public void outputGraph(String format) {
        GraphWriter writer = new GraphWriter();
        switch (format) {
            case "DOT":
                writer.writeDotGraph(this.name, this.jUnitGraphT);
                break;
            case "JSON":
                writer.writeJSONGraph(this.name, this.jUnitGraphT);
                break;
        }
    }

    private String generateGraphName() {
        String className = super.method.getDeclaringClass().getName();
        return className.substring(className.lastIndexOf(".") + 1) + "_" + super.method.getName();
    }

    private Graph<Vertex, DefaultEdge> generateJGraphT() {
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
        return graph;
    }

    private JGraph generateJGraph() {
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

        return graph;
    }
}
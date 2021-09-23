package phd.research.graph;

//import org.jgrapht.Graph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.core.GraphManager;
import phd.research.core.InterfaceManager;
import phd.research.core.MethodManager;
import phd.research.enums.Type;
import phd.research.jGraph.JGraph;
import phd.research.jGraph.Vertex;
import soot.MethodOrMethodContext;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.Iterator;

/**
 * @author Jordan Doyle
 */
public class Callgraph {

    private final CallGraph sootCallGraph;
    private final Graph<Vertex, DefaultEdge> jCallGraphT;
    private final JGraph jCallGraph;

    public Callgraph(CallGraph sootCallGraph) {
        this.sootCallGraph = sootCallGraph;
        this.jCallGraphT = generateJGraphT();
        this.jCallGraph = generateJGraph();
    }

    public Graph<Vertex, DefaultEdge> getJGraphTCallGraph() {
        return this.jCallGraphT;
    }

    public Composition getJGraphTComposition() {
        return new Composition(this.jCallGraphT);
    }

    public JGraph getJGraphCallGraph() {
        return this.jCallGraph;
    }

    public Composition getJGraphComposition() {
        return new Composition(this.jCallGraph);
    }

    public void outputGraph(String format) {
        GraphManager graphManager = GraphManager.getInstance();
        GraphWriter writer = new GraphWriter();
        switch (format) {
            case "DOT":
                writer.writeDotGraph(graphManager.getAppName() + "_CallGraph", this.jCallGraphT);
                break;
            case "JSON":
                writer.writeJSONGraph(graphManager.getAppName() + "_CallGraph", this.jCallGraphT);
                break;
        }
    }

    public String getGraphCompositionTable() {
        StringBuilder stringBuilder = new StringBuilder();
        String separator = "--------------------------------------------------------------------------------\n";
        String format = "\t%-20s\t%-20s\t%-20s\n";

        Composition jGraphT = this.getJGraphTComposition();
        Composition jGraph = this.getJGraphComposition();

        stringBuilder.append(separator)
                .append(String.format(format, "Count", "JCallGraphT", "JCallGraph")).append(separator)
                .append(String.format(format, "Vertex", jGraphT.getVertexCount(), jGraph.getVertexCount()))
                .append(String.format(format, "Edge", jGraphT.getEdgeCount(), jGraph.getEdgeCount()))
                .append(String.format(format, "System", jGraphT.getCallbackCount(), jGraph.getCallbackCount()))
                .append(String.format(format, "Callback", jGraphT.getListenerCount(), jGraph.getListenerCount()))
                .append(String.format(format, "Lifecycle", jGraphT.getLifecycleCount(), jGraph.getLifecycleCount()))
                .append(String.format(format, "Method", jGraphT.getMethodCount(), jGraph.getMethodCount()))
                .append(String.format(format, "Dummy", jGraphT.getDummyCount(), jGraph.getDummyCount()))
                .append(separator);

        return stringBuilder.toString();
    }

    private Graph<Vertex, DefaultEdge> generateJGraphT() {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        MethodManager methodManager = MethodManager.getInstance();
        InterfaceManager interfaceManager = InterfaceManager.getInstance();

        Iterator<MethodOrMethodContext> sourceItr = this.sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (methodManager.isFiltered(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = interfaceManager.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), srcMethod.toString(), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = this.sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (methodManager.isFiltered(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = interfaceManager.getMethodType(tgtMethod);
                        Vertex tgtVertex = new Vertex(tgtMethod.hashCode(), tgtMethod.toString(), methodType,
                                tgtMethod);
                        graph.addVertex(tgtVertex);

                        graph.addEdge(srcVertex, tgtVertex);
                    }
                }
            }
        }

        return graph;
    }

    private JGraph generateJGraph() {
        JGraph graph = new JGraph();
        MethodManager methodManager = MethodManager.getInstance();
        InterfaceManager interfaceManager = InterfaceManager.getInstance();

        Iterator<MethodOrMethodContext> sourceItr = this.sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (methodManager.isFiltered(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = interfaceManager.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), srcMethod.toString(), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = this.sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (methodManager.isFiltered(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = interfaceManager.getMethodType(tgtMethod);
                        Vertex tgtVertex = new Vertex(tgtMethod.hashCode(), tgtMethod.toString(), methodType,
                                tgtMethod);
                        graph.addVertex(tgtVertex);

                        graph.addEdge(srcVertex, tgtVertex);
                    }
                }
            }
        }

        return graph;
    }
}
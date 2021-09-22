package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.GraphManager;
import phd.research.core.InterfaceManager;
import phd.research.enums.Type;
import phd.research.helper.Control;
import phd.research.jGraph.JGraph;
import phd.research.jGraph.Vertex;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class ControlFlowGraph {

    private static final Logger logger = LoggerFactory.getLogger(ControlFlowGraph.class);

    private final Graph<Vertex, DefaultEdge> jControlFlowGraphT;
    private final JGraph jControlFlowGraph;

    public ControlFlowGraph(Callgraph callGraph) {
        this.jControlFlowGraphT = generateJGraphT(callGraph);
        this.jControlFlowGraph = generateJGraph(callGraph);
    }

    public Graph<Vertex, DefaultEdge> getJGraphTControlFlowGraph() {
        return this.jControlFlowGraphT;
    }

    public Composition getJGraphTComposition() {
        return new Composition(jControlFlowGraphT);
    }

    public Composition getJGraphComposition() {
        return new Composition(jControlFlowGraph);
    }

    public void outputGraph(String format) {
        GraphManager graphManager = GraphManager.getInstance();
        GraphWriter writer = new GraphWriter();
        switch (format) {
            case "DOT":
                writer.writeDotGraph(graphManager.getAppName() + "_ControlFlowGraph",
                        jControlFlowGraphT);
                break;
            case "JSON":
                writer.writeJSONGraph(graphManager.getAppName() + "_ControlFlowGraph",
                        jControlFlowGraphT);
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
                .append(String.format(format, "Interface", jGraphT.getControlCount(), jGraph.getControlCount()))
                .append(separator);

        return stringBuilder.toString();
    }

    /**
     * For some reason JGraphT does not have a method to retrieve individual vertices that already exist in the graph.
     * Instead this method searched a list of all the vertices contained within the graph for the required
     * {@link Vertex} object and returns it.
     *
     * @param id  the ID of the {@link Vertex} object being searched for.
     * @param set the set of all vertices to search.
     * @return The {@link Vertex} object from the vertex set with the given ID.
     */
    private Vertex getVertex(int id, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getID() == id) {
                return vertex;
            }
        }

        return null;
    }

    private Vertex getInterfaceControl(Vertex vertex) {
        InterfaceManager interfaceManager = InterfaceManager.getInstance();

        Control control = interfaceManager.getControl(vertex.getSootMethod());
        if (control != null) {
            return new Vertex(control.hashCode(), String.valueOf(control.getId()), Type.control, vertex.getSootMethod());
        } else {
            logger.error("No control for " + vertex.getLabel());
        }

        return null;
    }

    private Graph<Vertex, DefaultEdge> generateJGraphT(Callgraph callGraph) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(graph, callGraph.getJGraphTCallGraph());
        JimpleBasedInterproceduralCFG jimpleCFG = new JimpleBasedInterproceduralCFG();

        Set<Vertex> vertexSet = new HashSet<>(graph.vertexSet());
        for (Vertex vertex : vertexSet) {
            if (vertex.getType() == Type.listener) {
                Vertex interfaceVertex = getInterfaceControl(vertex);
                if (interfaceVertex != null) {
                    graph.addVertex(interfaceVertex);
                    graph.addEdge(interfaceVertex, vertex);
                } else {
                    logger.error("Failed to find interface control for vertex: \"" + vertex.getLabel() + "\". ");
                }
            }

            if (vertex.getType() == Type.listener || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.method) {
                UnitGraph unitGraph = new UnitGraph(vertex.getSootMethod().getActiveBody());
                Graph<Vertex, DefaultEdge> methodSubGraph = unitGraph.getJUnitGraphT();
                Graphs.addGraph(graph, methodSubGraph);

                Set<Vertex> roots = unitGraph.getRoots();
                for (Vertex root : roots) {
                    graph.addEdge(vertex, root);
                }

                //TODO: Don't forget to link the return statement back to the calling statement.
                Set<Unit> callStatements = jimpleCFG.getCallsFromWithin(vertex.getSootMethod());
                for (Unit callStatement : callStatements) {
                    Collection<SootMethod> calledMethods = jimpleCFG.getCalleesOfCallAt(callStatement);
                    for (SootMethod calledMethod : calledMethods) {
                        Vertex callVertex = getVertex(callStatement.hashCode(), graph.vertexSet());
                        Vertex calledVertex = getVertex(calledMethod.hashCode(), graph.vertexSet());
                        if (callVertex != null && calledVertex != null) {
                            graph.addEdge(callVertex, calledVertex);
                        }
                    }
                }
            } else if (vertex.getType() != Type.statement && vertex.getType() != Type.control
                    && vertex.getType() != Type.dummyMethod) {
                logger.error("Found unknown vertex type \"" + vertex.getType() + "\": " + vertex.getLabel());
            }
        }

        return graph;
    }

    private JGraph generateJGraph(Callgraph callGraph) {
        JGraph graph = new JGraph();
        graph.addGraph(callGraph.getJGraphCallGraph());
        JimpleBasedInterproceduralCFG jimpleBasedInterproceduralCFG = new JimpleBasedInterproceduralCFG();

        Set<Vertex> vertexSet = new HashSet<>(graph.vertexSet());
        for (Vertex vertex : vertexSet) {
            if (vertex.getType() == Type.listener) {
                Vertex interfaceVertex = getInterfaceControl(vertex);
                if (interfaceVertex != null) {
                    graph.addVertex(interfaceVertex);
                    graph.addEdge(interfaceVertex, vertex);
                } else {
                    logger.error("Failed to find interface control for vertex: \"" + vertex.getLabel() + "\". ");
                }
            }

            if (vertex.getType() == Type.listener || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.method) {
                UnitGraph unitGraph = new UnitGraph(vertex.getSootMethod().getActiveBody());
                JGraph methodSubGraph = unitGraph.getJUnitGraph();
                graph.addGraph(methodSubGraph);

                Set<Vertex> roots = unitGraph.getRoots();
                for (Vertex root : roots) {
                    graph.addEdge(vertex, root);
                }

                //TODO: Don't forget to link the return statement back to the calling statement.
                Set<Unit> callStatements = jimpleBasedInterproceduralCFG.getCallsFromWithin(vertex.getSootMethod());
                for (Unit callStatement : callStatements) {
                    Collection<SootMethod> calledMethods = jimpleBasedInterproceduralCFG.getCalleesOfCallAt(callStatement);
                    for (SootMethod calledMethod : calledMethods) {
                        Vertex callVertex = graph.getVertex(callStatement.hashCode());
                        Vertex calledVertex = graph.getVertex(calledMethod.hashCode());
                        if (callVertex != null && calledVertex != null) {
                            graph.addEdge(callVertex, calledVertex);
                        }
                    }
                }
            } else if (vertex.getType() != Type.statement && vertex.getType() != Type.control
                    && vertex.getType() != Type.dummyMethod) {
                logger.error("Found unknown vertex type \"" + vertex.getType() + "\": " + vertex.getLabel());
            }
        }

        return graph;
    }
}

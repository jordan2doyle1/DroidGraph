package phd.research.core;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.Pair;
import phd.research.enums.Type;
import phd.research.graph.Control;
import phd.research.graph.Filter;
import phd.research.graph.UnitGraph;
import phd.research.helper.API;
import phd.research.vertices.*;
import soot.*;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.Chain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class DroidGraph {

    private static final Logger logger = LoggerFactory.getLogger(DroidGraph.class);

    private final File collectedCallbacksFile;
    private final UiControls uiControls;

    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;


    public DroidGraph(File collectedCallbacksFile, UiControls controls) {
        if (!collectedCallbacksFile.exists()) {
            logger.error("Collected Callbacks File Does Not Exist!:" + collectedCallbacksFile);
        }

        this.collectedCallbacksFile = collectedCallbacksFile;
        this.uiControls = controls;
    }

    @API
    public DroidGraph(File collectedCallbacksFile, File apk) throws XmlPullParserException, IOException {
        if (!collectedCallbacksFile.exists()) {
            logger.error("Collected Callbacks File Does Not Exist!:" + collectedCallbacksFile);
        }

        this.collectedCallbacksFile = collectedCallbacksFile;
        this.uiControls = new UiControls(this.collectedCallbacksFile, apk);
    }


    public static Vertex getUnitVertex(Unit unit, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getType() == Type.unit) {
                if (((UnitVertex) vertex).getUnit().equals(unit)) {
                    return vertex;
                }
            }
        }

        return null;
    }

    public static Vertex getMethodVertex(Type type, SootMethod method, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getType() == type) {
                if (((MethodVertex) vertex).getMethod().equals(method)) {
                    return vertex;
                }
            }
        }

        return null;
    }

    public static Collection<Vertex> getControlsNotVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof ControlVertex && !v.hasVisit()).collect(Collectors.toSet());
    }

    public static Collection<Vertex> getMethodsNotVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof MethodVertex && v.getType() != Type.dummy && !v.hasVisit())
                .collect(Collectors.toSet());
    }

    public static Collection<Vertex> getControlVertices(SootClass activity, Collection<Integer> controlIds,
            Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v.getType() == Type.control).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getControlActivity().equals(activity) &&
                        controlIds.contains(v.getControl().getControlResource().getResourceID()))
                .collect(Collectors.toList());
    }


    @API
    public static Vertex getControlVertex(SootClass activity, int controlId, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getType() == Type.control) {
                Control currentControl = ((ControlVertex) vertex).getControl();
                if (currentControl.getControlActivity().equals(activity) &&
                        currentControl.getControlResource().getResourceID() == controlId) {
                    return vertex;
                }
            }
        }

        return null;
    }

    //TODO: Which getControlVertex method is being used (above or below)?

    @API
    public static Vertex getControlVertex(SootClass activity, String controlName, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getType() == Type.control) {
                Control currentControl = ((ControlVertex) vertex).getControl();
                if (currentControl.getControlActivity().equals(activity) &&
                        currentControl.getControlResource().getResourceName().equals(controlName)) {
                    return vertex;
                }
            }
        }

        return null;
    }

    @API
    public static void resetLocalVisits(Graph<Vertex, DefaultEdge> graph) {
        for (Vertex vertex : graph.vertexSet()) {
            vertex.localVisitReset();
        }
    }

    public static void resetVisits(Graph<Vertex, DefaultEdge> graph) {
        for (Vertex vertex : graph.vertexSet()) {
            vertex.visitReset();
            vertex.localVisitReset();
        }
    }

    public static Pair<Float, Float> calculateGraphCoverage(Graph<Vertex, DefaultEdge> graph) {
        float interfaceCoverage, interfaceTotal, methodCoverage, methodTotal;
        interfaceCoverage = interfaceTotal = methodCoverage = methodTotal = 0;

        for (Vertex vertex : graph.vertexSet()) {
            switch (vertex.getType()) {
                case control:
                    interfaceTotal += 1;
                    if (vertex.hasVisit()) {
                        interfaceCoverage += 1;
                    }
                    break;
                case method:
                case listener:
                case lifecycle:
                    methodTotal += 1;
                    if (vertex.hasVisit()) {
                        methodCoverage += 1;
                    }
                    break;
            }
        }

        return new Pair<>((interfaceCoverage / interfaceTotal) * 100, (methodCoverage / methodTotal) * 100);
    }

    @API
    public Collection<Vertex> getCFGControlsNotVisited() throws IOException, XmlPullParserException {
        return DroidGraph.getControlsNotVisited(this.getControlFlowGraph().vertexSet());
    }

    @API
    public Collection<Vertex> getCFGMethodsNotVisited() throws IOException, XmlPullParserException {
        return DroidGraph.getMethodsNotVisited(this.getControlFlowGraph().vertexSet());
    }

    private Type getMethodType(SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(this.collectedCallbacksFile);

        if (method.getDeclaringClass().getName().equals("dummyMainClass")) {
            return Type.dummy;
        } else if (Filter.isListenerMethod(callbacks.getCallbackMethods(), method)) {
            return Type.listener;
        } else if (Filter.isLifecycleMethod(method)) {
            return Type.lifecycle;
        } else if (Filter.isOtherCallbackMethod(callbacks.getCallbackMethods(), method)) {
            return Type.other;
        } else {
            return Type.method;
        }
    }

    public Graph<Vertex, DefaultEdge> getCallGraph() throws FileNotFoundException {
        if (this.callGraph == null) {
            this.callGraph = generateGraph(Scene.v().getCallGraph());
        }

        return this.callGraph;
    }

    @API
    public Graph<Vertex, DefaultEdge> getControlFlowGraph() throws IOException, XmlPullParserException {
        if (this.controlFlowGraph == null) {
            this.controlFlowGraph = generateGraph(this.getCallGraph());
        }

        return this.controlFlowGraph;
    }

    public void setControlFlowGraph(Graph<Vertex, DefaultEdge> graph) {
        this.controlFlowGraph = graph;
    }

    @API
    public void generateGraphs() throws IOException, XmlPullParserException {
        this.callGraph = generateGraph(Scene.v().getCallGraph());
        this.controlFlowGraph = generateGraph(this.callGraph);
    }

    public void resetCFGLocalVisits() throws IOException, XmlPullParserException {
        DroidGraph.resetLocalVisits(this.getControlFlowGraph());
    }

    @API
    public void resetCFGVisits() throws IOException, XmlPullParserException {
        DroidGraph.resetVisits(this.getControlFlowGraph());
    }

    @API
    public Pair<Float, Float> calculateCFGCoverage() throws IOException, XmlPullParserException {
        return calculateGraphCoverage(this.getControlFlowGraph());
    }

    @API
    public boolean visitMethod(SootMethod method) throws IOException, XmlPullParserException {
        logger.debug("Looking for method: " + method.getSignature());
        for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
            if (vertex.getType() == Type.method || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.listener || vertex.getType() == Type.dummy) {
                SootMethod currentMethod = ((MethodVertex) vertex).getMethod();
                if (currentMethod != null) {
                    logger.debug("Found method: " + currentMethod.getSignature());
                    if (currentMethod.equals(method)) {
                        vertex.visit();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private ControlVertex getInterfaceControl(ListenerVertex vertex) {
        Control control = this.uiControls.getListenerControl(vertex.getMethod());
        if (control != null) {
            return new ControlVertex(control);
        } else {
            logger.error("No control for " + vertex.getLabel());
        }

        return null;
    }

    private Set<SootMethod> checkGraph(Graph<Vertex, DefaultEdge> graph) throws IOException, XmlPullParserException {
        // TODO: Verify graph is complete and correct (all methods present?, all vertices have input edges?, etc.)
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> notInGraph = new HashSet<>();

        if (graph == null) {
            graph = this.getControlFlowGraph();
        }


        for (SootClass sootClass : classes) {
            if (Filter.isValidClass(sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    Type methodType = this.getMethodType(method);
                    MethodVertex vertex = new MethodVertex(methodType, method);
                    if (!graph.containsVertex(vertex)) {
                        notInGraph.add(method);
                    }
                }
            }
        }

        if (notInGraph.isEmpty()) {
            logger.info("All methods in the graph.");
        } else {
            logger.error(notInGraph.size() + " methods are not in the graph. ");

            for (SootMethod method : notInGraph) {
                logger.info(method.toString());
            }
        }

        return notInGraph;
    }

    // TODO: Confirm Graph Generation is correct?
    private Graph<Vertex, DefaultEdge> generateGraph(CallGraph sootCallGraph) throws FileNotFoundException {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        Iterator<MethodOrMethodContext> sourceItr = sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (Filter.isValidMethod(srcMethod) || srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = this.getMethodType(srcMethod);
                Vertex srcVertex = null;
                switch (methodType) {
                    case method:
                        srcVertex = new MethodVertex(srcMethod);
                        break;
                    case lifecycle:
                        srcVertex = new LifecycleVertex(srcMethod);
                        break;
                    case listener:
                        srcVertex = new ListenerVertex(srcMethod);
                        break;
                    case dummy:
                        srcVertex = new DummyVertex(srcMethod);
                        break;
                    default:
                        logger.error("Found unknown method type.");
                        break;
                }

                if (srcVertex != null) {
                    graph.addVertex(srcVertex);

                    Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                    while (edgeItr.hasNext()) {
                        SootMethod tgtMethod = edgeItr.next().tgt();

                        if (Filter.isValidMethod(tgtMethod) ||
                                srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                            methodType = this.getMethodType(tgtMethod);
                            Vertex tgtVertex = null;
                            switch (methodType) {
                                case method:
                                    tgtVertex = new MethodVertex(tgtMethod);
                                    break;
                                case lifecycle:
                                    tgtVertex = new LifecycleVertex(tgtMethod);
                                    break;
                                case listener:
                                    tgtVertex = new ListenerVertex(tgtMethod);
                                    break;
                                case dummy:
                                    tgtVertex = new DummyVertex(tgtMethod);
                                    break;
                                default:
                                    logger.error("Found unknown method type.");
                                    break;
                            }

                            if (tgtVertex != null) {
                                graph.addVertex(tgtVertex);
                                graph.addEdge(srcVertex, tgtVertex);
                            }
                        }
                    }
                }
            }
        }

        // checkGraph(graph);
        return graph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(Graph<Vertex, DefaultEdge> callGraph)
            throws IOException, XmlPullParserException {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(graph, callGraph);
        JimpleBasedInterproceduralCFG jimpleCFG = new JimpleBasedInterproceduralCFG();

        Set<Vertex> vertexSet = new HashSet<>(graph.vertexSet());
        for (Vertex vertex : vertexSet) {
            if (vertex.getType() == Type.listener) {
                ControlVertex interfaceVertex = getInterfaceControl((ListenerVertex) vertex);
                if (interfaceVertex != null) {
                    graph.addVertex(interfaceVertex);
                    graph.addEdge(interfaceVertex, vertex);
                } else {
                    logger.error("Failed to find interface control for vertex: \"" + vertex.getLabel() + "\". ");
                }
            }

            if (vertex.getType() == Type.listener || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.method) {

                if (((MethodVertex) vertex).getMethod().hasActiveBody()) {
                    UnitGraph unitGraph = new UnitGraph(((MethodVertex) vertex).getMethod().getActiveBody());
                    Graph<Vertex, DefaultEdge> methodSubGraph = unitGraph.getGraph();
                    Graphs.addGraph(graph, methodSubGraph);

                    Collection<Vertex> roots = unitGraph.getRoots();
                    for (Vertex root : roots) {
                        graph.addEdge(vertex, root);
                    }

                    //TODO: Don't forget to link the return statement back to the calling statement.
                    Set<Unit> callStatements = jimpleCFG.getCallsFromWithin(((MethodVertex) vertex).getMethod());
                    for (Unit callStatement : callStatements) {
                        Collection<SootMethod> calledMethods = jimpleCFG.getCalleesOfCallAt(callStatement);
                        for (SootMethod calledMethod : calledMethods) {
                            Type methodType = getMethodType(calledMethod);
                            Vertex callVertex = getUnitVertex(callStatement, graph.vertexSet());
                            Vertex calledVertex = getMethodVertex(methodType, calledMethod, graph.vertexSet());
                            if (callVertex != null && calledVertex != null) {
                                graph.addEdge(callVertex, calledVertex);
                            }
                        }
                    }
                }
            } else if (vertex.getType() != Type.unit && vertex.getType() != Type.control &&
                    vertex.getType() != Type.dummy) {
                logger.error("Found unknown vertex type \"" + vertex.getType() + "\": " + vertex.getLabel());
            }
        }

        for (Control control : uiControls.getControls()) {
            ControlVertex interfaceVertex = new ControlVertex(control);
            graph.addVertex(interfaceVertex);
        }

        // checkGraph(graph);

        for (Vertex v : graph.vertexSet()) {
            if (v.getType() == Type.method || v.getType() == Type.lifecycle || v.getType() == Type.listener ||
                    v.getType() == Type.dummy) {
                SootMethod m = ((MethodVertex) v).getMethod();
                if (m.getDeclaringClass().getName().contains("FragmentB")) {
                    System.out.println(m.getSignature());
                }
            }
        }

        // TODO: Get all methods and controls and check if they are in the graph. If they are not, then add
        //  standalone vertices for each of them.

        return graph;
    }
}
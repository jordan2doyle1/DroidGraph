package phd.research.core;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Pair;
import phd.research.enums.Type;
import phd.research.graph.*;
import phd.research.helper.API;
import phd.research.vertices.ControlVertex;
import phd.research.vertices.MethodVertex;
import phd.research.vertices.UnitVertex;
import phd.research.vertices.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class DroidGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(DroidGraph.class);

    @NotNull
    private final DroidControls droidControls;

    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;

    public DroidGraph(DroidControls droidControls, File callGraphFile) throws IOException {
        this(droidControls, callGraphFile, true);
    }

    public DroidGraph(DroidControls droidControls, File callGraphFile, boolean addMissingComponents)
            throws IOException {
        this.droidControls = Objects.requireNonNull(droidControls);
        this.generateGraphs(callGraphFile, addMissingComponents);
    }

    public static void outputCGDetails(File directory, Graph<Vertex, DefaultEdge> graph) throws IOException {
        Writer.writeString(directory, "CG_Composition", new Composition(graph).toTableString());
    }

    public static void outputCFGDetails(File directory, Graph<Vertex, DefaultEdge> graph) throws IOException {
        Writer.writeString(directory, "CFG_Composition", new Composition(graph).toTableString());
    }

    public static Collection<Vertex> getControlsVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof ControlVertex && v.hasVisit()).collect(Collectors.toSet());
    }

    public static Collection<Vertex> getControlsNotVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof ControlVertex && !v.hasVisit()).collect(Collectors.toSet());
    }

    public static Collection<Vertex> getMethodsVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof MethodVertex && v.getType() != Type.DUMMY && v.hasVisit())
                .collect(Collectors.toSet());
    }

    public static Collection<Vertex> getMethodsNotVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof MethodVertex && v.getType() != Type.DUMMY && !v.hasVisit())
                .collect(Collectors.toSet());
    }

    public static void resetVisits(Graph<Vertex, DefaultEdge> graph) {
        graph.vertexSet().forEach(vertex -> {
            vertex.visitReset();
            vertex.localVisitReset();
        });
    }

    @API
    public static void resetLocalVisits(Graph<Vertex, DefaultEdge> graph) {
        graph.vertexSet().forEach(Vertex::localVisitReset);
    }

    public static Vertex getUnitVertex(Unit unit, Set<Vertex> vertices) {
        return vertices.stream().filter(v -> v.getType() == Type.UNIT && ((UnitVertex) v).getUnit().equals(unit))
                .findFirst().orElse(null);
    }

    public static Vertex getMethodVertex(SootMethod method, Set<Vertex> vertices) {
        return vertices.stream().filter(v ->
                        (v.getType() == Type.LISTENER || v.getType() == Type.LIFECYCLE || v.getType() == Type.DUMMY ||
                                v.getType() == Type.METHOD) && ((MethodVertex) v).getMethod().equals(method)).findFirst()
                .orElse(null);
    }

    @API
    public static Vertex getControlVertex(SootClass activity, String controlName, Set<Vertex> vertices) {
        return vertices.stream().filter(v -> v.getType() == Type.CONTROL).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getControlActivity().equals(activity) &&
                        v.getControl().getControlResource().getResourceName().equals(controlName)).findFirst()
                .orElse(null);
    }

    @API
    public static Vertex getControlVertex(SootClass activity, int controlId, Set<Vertex> vertices) {
        return vertices.stream().filter(v -> v.getType() == Type.CONTROL).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getControlActivity().equals(activity) &&
                        v.getControl().getControlResource().getResourceID() == controlId).findFirst().orElse(null);
    }

    public static Pair<Float, Float> calculateGraphCoverage(Graph<Vertex, DefaultEdge> graph) {
        float interfaceCoverage, interfaceTotal, methodCoverage, methodTotal;
        interfaceCoverage = interfaceTotal = methodCoverage = methodTotal = 0;

        for (Vertex vertex : graph.vertexSet()) {
            switch (vertex.getType()) {
                case CONTROL:
                    interfaceTotal += 1;
                    if (vertex.hasVisit()) {
                        interfaceCoverage += 1;
                    }
                    break;
                case METHOD:
                case LISTENER:
                case LIFECYCLE:
                    methodTotal += 1;
                    if (vertex.hasVisit()) {
                        methodCoverage += 1;
                    }
                    break;
            }
        }

        return new Pair<>((interfaceCoverage / interfaceTotal) * 100, (methodCoverage / methodTotal) * 100);
    }

    @Nonnull
    public Graph<Vertex, DefaultEdge> getCallGraph() {
        return this.callGraph;
    }

    @Nonnull
    @API
    public Graph<Vertex, DefaultEdge> getControlFlowGraph() {
        return this.controlFlowGraph;
    }

    public void generateGraphs(File callGraphFile, boolean addMissingVertices) throws IOException {
        this.callGraph = Importer.importAndroGuardCallGraph(callGraphFile);
        this.controlFlowGraph = generateGraph(this.callGraph, addMissingVertices);
    }

    @API
    public Collection<Vertex> getCFGControlsVisited() {
        return DroidGraph.getControlsVisited(this.getControlFlowGraph().vertexSet());
    }

    @API
    public Collection<Vertex> getCFGMethodsVisited() {
        return DroidGraph.getMethodsVisited(this.getControlFlowGraph().vertexSet());
    }

    @API
    public Collection<Vertex> getCFGControlsNotVisited() {
        return DroidGraph.getControlsNotVisited(this.getControlFlowGraph().vertexSet());
    }

    @API
    public Collection<Vertex> getCFGMethodsNotVisited() {
        return DroidGraph.getMethodsNotVisited(this.getControlFlowGraph().vertexSet());
    }

    @API
    public void resetCFGVisits() {
        DroidGraph.resetVisits(this.getControlFlowGraph());
    }

    @API
    public void resetCFGLocalVisits() {
        DroidGraph.resetLocalVisits(this.getControlFlowGraph());
    }

    @API
    public Pair<Float, Float> calculateCFGCoverage() {
        return calculateGraphCoverage(this.getControlFlowGraph());
    }

    @Deprecated
    public boolean visitCFGMethodVertex(SootMethod method) {
        // TODO: Should this be replaced with a find method and then .visit method call?
        for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
            if ((vertex.getType() == Type.METHOD || vertex.getType() == Type.LIFECYCLE ||
                    vertex.getType() == Type.LISTENER || vertex.getType() == Type.DUMMY ||
                    vertex.getType() == Type.CALLBACK) && ((MethodVertex) vertex).getMethod().equals(method)) {
                vertex.visit();
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Deprecated
    private Graph<Vertex, DefaultEdge> generateGraph(CallGraph sootCallGraph) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        sootCallGraph.forEach(edge -> {
            String dummyMain = "dummyMainClass";
            if ((Filter.isValidMethod(edge.src()) || edge.src().getDeclaringClass().getName().equals(dummyMain)) &&
                    (Filter.isValidMethod(edge.tgt()) || edge.tgt().getDeclaringClass().getName().equals(dummyMain))) {
                Vertex srcVertex = MethodVertex.createMethodVertex(edge.src());
                graph.addVertex(srcVertex);
                Vertex tgtVertex = MethodVertex.createMethodVertex(edge.tgt());
                graph.addVertex(tgtVertex);
                graph.addEdge(srcVertex, tgtVertex);
            }
        });

        return graph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(Graph<Vertex, DefaultEdge> callGraph, boolean addMissingVertices) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(graph, callGraph);

        this.droidControls.getControls().forEach(control -> {
            Vertex controlVertex = new ControlVertex(control);
            graph.addVertex(controlVertex);
            control.getClickListeners().forEach(method -> {
                Vertex listenerVertex = getMethodVertex(method, graph.vertexSet());
                if (listenerVertex != null) {
                    graph.addEdge(controlVertex, listenerVertex);
                } else {
                    LOGGER.error(String.format("Listener method %s not found in the graph.", method));
                }
            });
        });

        JimpleBasedInterproceduralCFG jimpleCFG = new JimpleBasedInterproceduralCFG();
        Set<Vertex> graphVertices = new HashSet<>(graph.vertexSet());
        graphVertices.stream().filter(vertex -> vertex.getType() != Type.CONTROL).forEach(vertex -> {
            SootMethod method = ((MethodVertex) vertex).getMethod();
            if (method.hasActiveBody()) {
                UnitGraph unitGraph = new UnitGraph(method.getActiveBody());
                Graph<Vertex, DefaultEdge> methodSubGraph = unitGraph.getGraph();
                Graphs.addGraph(graph, methodSubGraph);
                unitGraph.getRoots().forEach(root -> graph.addEdge(vertex, root));

                // TODO: Fix - jimpleCFG.getCalleesOfCallAt(caller) produces error method is referenced but has no body
                jimpleCFG.getCallsFromWithin(method).forEach(
                        caller -> jimpleCFG.getCalleesOfCallAt(caller).stream().filter(Filter::isValidMethod)
                                .forEach(callee -> {
                                    Vertex callerVertex = getUnitVertex(caller, graph.vertexSet());
                                    if (callerVertex == null) {
                                        LOGGER.error(String.format("Caller %s not found in the graph.", caller));
                                    }
                                    Vertex calleeVertex = getMethodVertex(callee, graph.vertexSet());
                                    if (calleeVertex == null) {
                                        LOGGER.error(String.format("Callee %s not found in the graph.", callee));
                                        if (addMissingVertices) {
                                            LOGGER.info(String.format("Adding %s method into the graph.", callee));
                                            graph.addVertex(MethodVertex.createMethodVertex(callee));
                                        }
                                    }
                                    if (callerVertex != null && calleeVertex != null) {
                                        graph.addEdge(callerVertex, calleeVertex);
                                    }
                                }));
                //TODO: Link method return unit back to the calling unit.
            }
        });

        // TODO: Should missing methods be added to graph or has FlowDroid left them out for a reason?
        return verifyGraphContents(graph, addMissingVertices);
    }

    private Graph<Vertex, DefaultEdge> verifyGraphContents(Graph<Vertex, DefaultEdge> graph, boolean addVertices) {
        Objects.requireNonNull(graph);

        Collection<Control> missingControls = new HashSet<>();
        for (Control control : droidControls.getControls()) {
            if (!graph.containsVertex(new ControlVertex(control))) {
                missingControls.add(control);
            }
        }

        if (!missingControls.isEmpty()) {
            LOGGER.error(String.format("Found %s controls that are not in the graph.", missingControls.size()));
            if (addVertices) {
                LOGGER.info(String.format("Adding %s controls into the graph.", missingControls.size()));
                missingControls.forEach(control -> graph.addVertex(new ControlVertex(control)));
            }
        }

        Collection<SootMethod> missingMethods = new HashSet<>();
        Scene.v().getClasses().stream().filter(Filter::isValidClass)
                .forEach(clazz -> clazz.getMethods().forEach(method -> {
                    Vertex vertex = MethodVertex.createMethodVertex(method);
                    if (!graph.containsVertex(vertex)) {
                        SootClass currentClass = method.getDeclaringClass();
                        boolean found = false;
                        while (currentClass.hasSuperclass()) {
                            currentClass = currentClass.getSuperclass();
                            SootMethod superClassMethod = currentClass.getMethodUnsafe(method.getSubSignature());
                            if (superClassMethod != null) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            missingMethods.add(method);
                        }
                    }
                }));

        if (!missingMethods.isEmpty()) {
            LOGGER.error(String.format("Found %s methods that are not in the graph.", missingMethods.size()));
            if (addVertices) {
                LOGGER.info(String.format("Adding %s methods into the graph.", missingMethods.size()));
                missingMethods.forEach(method -> {
                    graph.addVertex(MethodVertex.createMethodVertex(method));
                    System.out.println(method.getSignature());
                });
            }
        }

        return graph;
    }

    @SuppressWarnings("unused")
    public void linkControlsToListeners() {
        // TODO: Write method to track variable assignments and link listener callback methods to their user controls.

        // Loop through all methods found by FlowDroid.
        //     Loop through all the statements in each method.
        //         If statement is InvokeStatement or AssignStatement where right operand is InvokeStatement.
        //             If frontMatter callback setter methods include InvokeStatement.
        //                 Variable equals variable name.
        // Search all statements in code base for assignments to variable name.
        // Record all values for variable name.
    }
}
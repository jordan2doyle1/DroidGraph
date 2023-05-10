package phd.research.core;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Pair;
import phd.research.StringTable;
import phd.research.Timer;
import phd.research.enums.Type;
import phd.research.graph.Classifier;
import phd.research.graph.Composition;
import phd.research.graph.Control;
import phd.research.graph.UnitGraph;
import phd.research.singletons.FlowDroidAnalysis;
import phd.research.singletons.GraphSettings;
import phd.research.utility.Filter;
import phd.research.utility.Importer;
import phd.research.utility.Writer;
import phd.research.vertices.*;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class DroidGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(DroidGraph.class);

    @NotNull
    private final Graph<Vertex, DefaultEdge> controlFlowGraph;

    private DroidControls droidControls;
    private Graph<Vertex, DefaultEdge> callGraph;

    public DroidGraph() {
        if (GraphSettings.v().isImportControlFlowGraph()) {
            this.controlFlowGraph = Importer.importDroidGraph(GraphSettings.v().getContolFlowGraphFile());
        } else {
            this.controlFlowGraph = this.generateGraph();
            verifyControlFlowGraphContents();
        }
    }

    @SuppressWarnings("unused")
    public static Vertex getControlVertex(String activity, String controlName, Set<Vertex> vertices) {
        return vertices.stream().filter(vertex -> vertex.getType() == Type.CONTROL).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getActivity().equals(activity) &&
                        v.getControl().getControlName().equals(controlName)).findFirst().orElse(null);
    }

    @SuppressWarnings("unused")
    public static Vertex getControlVertex(String activity, int controlId, Set<Vertex> vertices) {
        return vertices.stream().filter(vertex -> vertex.getType() == Type.CONTROL).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getActivity().equals(activity) &&
                        v.getControl().getControlId() == controlId).findFirst().orElse(null);
    }

    public static Vertex getUnitVertex(Unit unit, Set<Vertex> vertices) {
        return vertices.stream().filter(vertex -> vertex.getType() == Type.UNIT &&
                ((UnitVertex) vertex).getUnit().equals(unit.toString())).findFirst().orElse(null);
    }

    public static Vertex getMethodVertex(String methodSignature, Set<Vertex> vertices) {
        return vertices.stream().filter(vertex -> vertex instanceof MethodVertex &&
                ((MethodVertex) vertex).getMethodSignature().equals(methodSignature)).findFirst().orElse(null);
    }

    public DroidControls getDroidControls() {
        if (this.droidControls == null) {
            this.droidControls = new DroidControls();
        }
        return this.droidControls;
    }

    @NotNull
    public Graph<Vertex, DefaultEdge> getCallGraph() {
        if (this.callGraph == null) {
            this.callGraph = Importer.convertAndFilterAndroGuardGraph(Filter.getAndroGuardCallGraph());
        }
        return this.callGraph;
    }

    @NotNull
    public Graph<Vertex, DefaultEdge> getControlFlowGraph() {
        return this.controlFlowGraph;
    }

    @SuppressWarnings("unused")
    public void resetVisits() {
        this.getControlFlowGraph().vertexSet().forEach(vertex -> {
            vertex.visitReset();
            vertex.localVisitReset();
        });
    }

    @SuppressWarnings("unused")
    public void resetLocalVisits() {
        this.getControlFlowGraph().vertexSet().forEach(Vertex::localVisitReset);
    }

    @SuppressWarnings("unused")
    public Collection<Vertex> getControlsVisited() {
        return this.getControlFlowGraph().vertexSet().stream().filter(v -> v instanceof ControlVertex && v.hasVisit())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unused")
    public Collection<Vertex> getControlsNotVisited() {
        return this.getControlFlowGraph().vertexSet().stream().filter(v -> v instanceof ControlVertex && !v.hasVisit())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unused")
    public Collection<Vertex> getMethodsVisited() {
        return this.getControlFlowGraph().vertexSet().stream()
                .filter(v -> v instanceof MethodVertex && v.getType() != Type.DUMMY && v.hasVisit())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unused")
    public Collection<Vertex> getMethodsNotVisited() {
        return this.getControlFlowGraph().vertexSet().stream()
                .filter(v -> v instanceof MethodVertex && v.getType() != Type.DUMMY && !v.hasVisit())
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unused")
    public Pair<Float, Float> calculateCoverage() {
        float interfaceCoverage, interfaceTotal, methodCoverage, methodTotal;
        interfaceCoverage = interfaceTotal = methodCoverage = methodTotal = 0;

        for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
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

    @SuppressWarnings("unused")     // Used in DroidDynaSearch.
    public void outputVertexVisitStatus() throws IOException {
        this.outputVertexVisitStatus(
                new File(GraphSettings.v().getOutputDirectory() + File.separator + "vertex_visit_status.txt"));
    }

    public void outputVertexVisitStatus(File outputFile) throws IOException {
        StringBuilder builder = new StringBuilder();

        Collection<Vertex> vertices = this.getControlsVisited();
        builder.append("---------- Controls Visited (").append(vertices.size()).append(") ----------\n");
        for (Vertex vertex : vertices) {
            builder.append(((ControlVertex) vertex).getControl().getControlName()).append("\n");
        }

        vertices = this.getControlsNotVisited();
        builder.append("\n---------- Controls Not Visited (").append(vertices.size()).append(") ----------\n");
        for (Vertex vertex : vertices) {
            builder.append(((ControlVertex) vertex).getControl().getControlName()).append("\n");
        }

        vertices = this.getMethodsVisited();
        builder.append("\n---------- Methods Visited (").append(vertices.size()).append(") ----------\n");
        for (Vertex v : vertices) {
            builder.append(((MethodVertex) v).getMethodSignature()).append("\n");
        }

        vertices = this.getMethodsNotVisited();
        builder.append("\n---------- Methods Not Visited (").append(vertices.size()).append(") ----------\n");
        for (Vertex v : vertices) {
            builder.append(((MethodVertex) v).getMethodSignature()).append("\n");
        }

        Writer.writeString(outputFile.getParentFile(), outputFile.getName(), builder.toString());
    }

    public void outputCGDetails() throws IOException {
        Writer.writeString(GraphSettings.v().getOutputDirectory(), "call_graph_composition.txt",
                new Composition(this.getCallGraph()).toTableString()
                          );
    }

    public void outputCFGDetails() throws IOException {
        Writer.writeString(GraphSettings.v().getOutputDirectory(), "control_flow_graph_composition.txt",
                new Composition(this.getControlFlowGraph()).toTableString()
                          );
    }

    public void writeFlowDroidAnalysisToFile() throws IOException {
        if (!FlowDroidAnalysis.v().isFlowDroidExecuted()) {
            FlowDroidAnalysis.v().runFlowDroid();
        }

        Collection<SootClass> filteredClasses = new HashSet<>();
        Collection<SootMethod> allMethods = new HashSet<>(), filteredMethods = new HashSet<>(), standardMethods =
                new HashSet<>(), lifecycleCallbacks = new HashSet<>(), listenerCallbacks = new HashSet<>(),
                possibleCallbacks = new HashSet<>(), otherCallback = new HashSet<>(), ignoredMethods = new HashSet<>();

        Classifier classifier = new Classifier();

        for (SootClass clazz : Scene.v().getClasses()) {
            allMethods.addAll(clazz.getMethods());

            if (Filter.isValidClass(clazz)) {
                filteredClasses.add(clazz);

                List<SootMethod> methods = clazz.getMethods();
                for (SootMethod method : methods) {
                    if (Filter.isValidMethod(method)) {
                        filteredMethods.add(method);

                        switch (classifier.getMethodType(method)) {
                            case DUMMY:
                                ignoredMethods.add(method);
                                break;
                            case LIFECYCLE:
                                lifecycleCallbacks.add(method);
                                break;
                            case LISTENER:
                                if (classifier.isListenerMethod(method)) {
                                    listenerCallbacks.add(method);
                                } else {
                                    possibleCallbacks.add(method);
                                }
                                break;
                            case CALLBACK:
                                otherCallback.add(method);
                                break;
                            case METHOD:
                                standardMethods.add(method);
                                break;

                        }
                    }
                }
            }
        }

        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "all_classes.txt", Scene.v().getClasses());
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "filtered_classes.txt", filteredClasses);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "entry_point_classes.txt",
                FlowDroidAnalysis.v().getEntryPointClasses()
                              );

        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "all_methods.txt", allMethods);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "filtered_methods.txt", filteredMethods);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "standard_methods.txt", standardMethods);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "lifecycle_methods.txt", lifecycleCallbacks);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "listener_methods.txt", listenerCallbacks);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "possible_callbacks.txt", possibleCallbacks);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "other_callbacks.txt", otherCallback);
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "ignored_methods.txt", ignoredMethods);

        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "launch_activities.txt",
                FlowDroidAnalysis.v().getLaunchActivities()
                              );

        Map<SootClass, Set<SootClass>> fragments = classifier.getFragments();
        Writer.writeMap(GraphSettings.v().getOutputDirectory(), "fragment_classes.txt", fragments);
    }

    public void writeUnitGraphsToFile() throws IOException {
        LOGGER.info("Exporting unit graphs in " + GraphSettings.v().getFormat().name() + " format(s).");
        for (SootClass clazz : Scene.v().getClasses()) {
            for (SootMethod method : clazz.getMethods()) {
                if (Filter.isValidMethod(method) && method.hasActiveBody()) {
                    UnitGraph unitGraph = new UnitGraph(method.getActiveBody());
                    String fileName = clazz.getShortName() + "_" + method.getName();
                    Writer.writeGraph(GraphSettings.v().getOutputDirectory(), fileName, GraphSettings.v().getFormat(),
                            unitGraph.getGraph()
                                     );
                }
            }
        }
    }

    public void writeCallGraphToFile() throws IOException {
        LOGGER.info("Exporting call graph in " + GraphSettings.v().getFormat().name() + " format(s).");
        Writer.writeGraph(GraphSettings.v().getOutputDirectory(), "app_call_graph", GraphSettings.v().getFormat(),
                this.getCallGraph()
                         );
    }

    public void writeControlFlowGraphToFile() throws IOException {
        LOGGER.info("Exporting control flow graph in " + GraphSettings.v().getFormat().name() + " format(s).");
        Writer.writeGraph(GraphSettings.v().getOutputDirectory(), "app_control_flow_graph",
                GraphSettings.v().getFormat(), this.getControlFlowGraph()
                         );
    }

    public void writeControlsToFile() throws IOException {
        Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "interface_controls.txt",
                this.getDroidControls().getControls()
                              );
        Writer.writeString(GraphSettings.v().getOutputDirectory(), "control_callbacks.txt",
                this.getControlCallbackTableString()
                          );
    }

    private String getControlCallbackTableString() {
        List<Control> controls = new ArrayList<>(this.getDroidControls().getControls());
        String[][] data = new String[controls.size() + 1][];
        data[0] = new String[]{"WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD"};
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);

            StringBuilder builder = new StringBuilder("[");
            control.getListeners().forEach(l -> builder.append(l).append(","));
            if (builder.charAt(builder.length() - 1) != '[') {
                builder.replace(builder.length() - 1, builder.length(), "]");
            } else {
                builder.append("]");
            }

            data[i + 1] = new String[]{String.valueOf(control.getControlId()), control.getControlName(),
                    control.getActivity(), builder.toString()};
        }
        return StringTable.tableWithLines(data, true);
    }

    private Graph<Vertex, DefaultEdge> generateGraph() {
        if (!FlowDroidAnalysis.v().isFlowDroidExecuted()) {
            FlowDroidAnalysis.v().runFlowDroid();
        }

        Timer timer = new Timer();
        LOGGER.info("Running graph generation... (" + timer.start(true) + ")");

        LOGGER.info("Adding call graph vertices and edges to the control flow graph.");
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(graph, this.getCallGraph());
        LOGGER.info(this.getCallGraph().vertexSet().size() + " vertices and " + this.getCallGraph().edgeSet().size() +
                " edges added to the control flow graph.");

        LOGGER.info("Adding controls to the control flow graph.");
        this.getDroidControls().getControls().forEach(control -> {
            Vertex controlVertex = new ControlVertex(control);
            graph.addVertex(controlVertex);
            control.getListeners().forEach(method -> {
                SootMethod listener = Scene.v().grabMethod(method);
                Vertex listenerVertex = DroidGraph.getMethodVertex(listener.getSignature(), graph.vertexSet());
                if (listenerVertex != null) {
                    graph.addEdge(controlVertex, listenerVertex);
                } else {
                    LOGGER.error(String.format("Listener method %s not found in the graph.", listener));
                }
            });
        });
        LOGGER.info(this.getDroidControls().getControls().size() + " controls added to the control flow graph.");

        LOGGER.info("Adding unit graphs to the control flow graph.");
        AtomicInteger numberOfUnitGraphs = new AtomicInteger();
        JimpleBasedInterproceduralCFG jimpleCFG = new JimpleBasedInterproceduralCFG();
        Set<Vertex> graphVertices = new HashSet<>(graph.vertexSet());
        graphVertices.stream().filter(vertex -> vertex.getType() != Type.CONTROL).forEach(vertex -> {
            SootMethod method = Scene.v().grabMethod(((MethodVertex) vertex).getMethodSignature());
            if (method.hasActiveBody()) {
                UnitGraph unitGraph = new UnitGraph(method.getActiveBody());
                Graph<Vertex, DefaultEdge> methodSubGraph = unitGraph.getGraph();
                Graphs.addGraph(graph, methodSubGraph);
                unitGraph.getRoots().forEach(root -> graph.addEdge(vertex, root));
                numberOfUnitGraphs.getAndIncrement();

                //TODO: Fix - jimpleCFG.getCalleesOfCallAt(caller) produces error 'method is referenced but has no body'
                jimpleCFG.getCallsFromWithin(method).forEach(
                        caller -> jimpleCFG.getCalleesOfCallAt(caller).stream().filter(Filter::isValidMethod)
                                .forEach(callee -> {
                                    Vertex callerVertex = DroidGraph.getUnitVertex(caller, graph.vertexSet());
                                    if (callerVertex == null) {
                                        LOGGER.error(String.format("Caller %s not found in the graph.", caller));
                                    }
                                    Vertex calleeVertex =
                                            DroidGraph.getMethodVertex(callee.getSignature(), graph.vertexSet());
                                    if (calleeVertex == null) {
                                        LOGGER.error(String.format("Callee %s not found in the graph.", callee));
                                        if (!callee.getDeclaringClass().getPackageName()
                                                .startsWith(FlowDroidAnalysis.v().getBasePackageName())) {
                                            LOGGER.info(
                                                    String.format("Callee %s is probably not a valid method.", callee));
                                        }
                                        if (GraphSettings.v().isAddMissingComponents()) {
                                            LOGGER.info(String.format("Adding %s method into the graph.", callee));
                                            graph.addVertex(new VertexFactory().createVertex(callee));
                                        }
                                    }
                                    if (callerVertex != null && calleeVertex != null) {
                                        graph.addEdge(callerVertex, calleeVertex);
                                    }
                                }));
                //TODO: Link method return unit back to the calling unit.
            }
        });
        LOGGER.info(numberOfUnitGraphs + " unit graphs added to the control flow graph.");

        LOGGER.info(graph.vertexSet().size() + " vertices and " + graph.edgeSet().size() + " edges added to the " +
                "control flow graph.");
        LOGGER.info("(" + timer.end() + ") Graph generation took " + timer.secondsDuration() + " second(s).");
        return graph;
    }

    private void verifyControlFlowGraphContents() {
        Timer timer = new Timer();
        LOGGER.info("Verifying Control Flow Graph Content... (" + timer.start(true) + ")");
        boolean problemFound = false;

        LOGGER.info("Searching for missing controls.");
        Collection<Control> missingControls = new HashSet<>();
        for (Control control : this.getDroidControls().getControls()) {
            boolean foundControl = false;
            for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
                if (vertex instanceof ControlVertex) {
                    if (((ControlVertex) vertex).getControl().equals(control)) {
                        foundControl = true;
                    }
                }
            }

            if (!foundControl) {
                missingControls.add(control);
            }
        }

        if (GraphSettings.v().isOutputMissingComponents()) {
            try {
                Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "missing_controls.txt", missingControls);
            } catch (IOException e) {
                LOGGER.error("Error writing missing controls to output file." + e.getMessage());
            }
        }

        if (!missingControls.isEmpty()) {
            problemFound = true;
            LOGGER.error(String.format("Found %s controls that are not in the graph.", missingControls.size()));
            if (GraphSettings.v().isAddMissingComponents()) {
                LOGGER.info(String.format("Adding %s controls into the graph.", missingControls.size()));
                missingControls.forEach(control -> this.getControlFlowGraph().addVertex(new ControlVertex(control)));
            }
        }

        LOGGER.info("Searching for missing methods.");
        Collection<SootMethod> missingMethods = new HashSet<>();
        Scene.v().getClasses().stream().filter(Filter::isValidClass)
                .forEach(clazz -> clazz.getMethods().stream().filter(Filter::isValidMethod).forEach(method -> {
                    boolean foundMethod = false;
                    for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
                        if (vertex instanceof MethodVertex) {
                            if (((MethodVertex) vertex).getMethodSignature().equals(method.getSignature())) {
                                foundMethod = true;
                                break;
                            }
                        }
                    }

                    if (!foundMethod) {
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

        if (GraphSettings.v().isOutputMissingComponents()) {
            try {
                Writer.writeCollection(GraphSettings.v().getOutputDirectory(), "missing_methods.txt", missingMethods);
            } catch (IOException e) {
                LOGGER.error("Error writing missing methods to output file." + e.getMessage());
            }
        }

        if (!missingMethods.isEmpty()) {
            problemFound = true;
            LOGGER.error(String.format("Found %s methods that are not in the graph.", missingMethods.size()));
            if (GraphSettings.v().isAddMissingComponents()) {
                LOGGER.info(String.format("Adding %s methods into the graph.", missingMethods.size()));
                missingMethods.forEach(
                        method -> this.getControlFlowGraph().addVertex(new VertexFactory().createVertex(method)));
            }
        }

        if (!FlowDroidAnalysis.v().isFlowDroidExecuted()) {
            FlowDroidAnalysis.v().runFlowDroid();
        }

        LOGGER.info("Collecting class and method classifications.");
        Collection<SootClass> filteredClasses = new HashSet<>();
        Collection<SootMethod> allMethods = new HashSet<>(), filteredMethods = new HashSet<>(), standardMethods =
                new HashSet<>(), lifecycleCallbacks = new HashSet<>(), listenerCallbacks = new HashSet<>(),
                possibleCallbacks = new HashSet<>(), otherCallback = new HashSet<>(), ignoredMethods = new HashSet<>();

        Classifier classifier = new Classifier();

        for (SootClass clazz : Scene.v().getClasses()) {
            allMethods.addAll(clazz.getMethods());

            if (Filter.isValidClass(clazz)) {
                filteredClasses.add(clazz);

                List<SootMethod> methods = clazz.getMethods();
                for (SootMethod method : methods) {
                    if (Filter.isValidMethod(method)) {
                        filteredMethods.add(method);

                        switch (classifier.getMethodType(method)) {
                            case DUMMY:
                                ignoredMethods.add(method);
                                break;
                            case LIFECYCLE:
                                lifecycleCallbacks.add(method);
                                break;
                            case LISTENER:
                                if (classifier.isListenerMethod(method)) {
                                    listenerCallbacks.add(method);
                                } else {
                                    possibleCallbacks.add(method);
                                }
                                break;
                            case CALLBACK:
                                otherCallback.add(method);
                                break;
                            case METHOD:
                                standardMethods.add(method);
                                break;

                        }
                    }
                }
            }
        }

        LOGGER.info("Verifying methods in FlowDroid match methods in control flow graph.");

        Map<String, Collection<SootClass>> classEmptyCheckMap = new HashMap<>();
        classEmptyCheckMap.put("all classes", Scene.v().getClasses());
        classEmptyCheckMap.put("filtered classes", filteredClasses);
        classEmptyCheckMap.put("entry point classes", FlowDroidAnalysis.v().getEntryPointClasses());
        classEmptyCheckMap.put("launch activities", FlowDroidAnalysis.v().getLaunchActivities());
        for (Map.Entry<String, Collection<SootClass>> entry : classEmptyCheckMap.entrySet()) {
            if (entry.getValue().size() == 0) {
                problemFound = true;
                LOGGER.warn("No classes in " + entry.getKey() + ".");
            }
        }

        Map<String, Collection<SootMethod>> methodEmptyCheckMap = new HashMap<>();
        methodEmptyCheckMap.put("all methods", allMethods);
        methodEmptyCheckMap.put("filtered classes", filteredMethods);
        methodEmptyCheckMap.put("entry point classes", lifecycleCallbacks);
        methodEmptyCheckMap.put("launch activities", standardMethods);
        for (Map.Entry<String, Collection<SootMethod>> entry : methodEmptyCheckMap.entrySet()) {
            if (entry.getValue().size() == 0) {
                problemFound = true;
                LOGGER.warn("No methods in " + entry.getKey() + ".");
            }
        }

        if (filteredMethods.size() !=
                (standardMethods.size() + possibleCallbacks.size() + otherCallback.size() + listenerCallbacks.size() +
                        lifecycleCallbacks.size() + ignoredMethods.size())) {
            problemFound = true;
            LOGGER.warn("Number of filtered methods does not equal number of classified methods.");
        }

        Composition cgComposition = new Composition(this.getCallGraph());
        Map<String, Integer> callGraphEmptyCheckMap = new HashMap<>();
        callGraphEmptyCheckMap.put("units", cgComposition.getUnit());
        callGraphEmptyCheckMap.put("dummy methods", cgComposition.getDummy());
        callGraphEmptyCheckMap.put("controls", cgComposition.getControl());
        for (Map.Entry<String, Integer> entry : callGraphEmptyCheckMap.entrySet()) {
            if (entry.getValue() != 0) {
                problemFound = true;
                LOGGER.warn("Call graph contains " + entry.getKey() + ".");
            }
        }

        if (cgComposition.getVertex() !=
                (cgComposition.getMethod() + cgComposition.getCallback() + cgComposition.getListener() +
                        cgComposition.getLifecycle())) {
            problemFound = true;
            LOGGER.warn("Call graph method type counts does not match vertex count.");
        }

        Map<String, Pair<Integer, Integer>> callGraphCountCheckMap = new HashMap<>();
        callGraphCountCheckMap.put("Filtered", new Pair<>(cgComposition.getVertex(), filteredMethods.size()));
        callGraphCountCheckMap.put("Lifecycle", new Pair<>(cgComposition.getLifecycle(), lifecycleCallbacks.size()));
        callGraphCountCheckMap.put("Callback", new Pair<>(cgComposition.getCallback(), otherCallback.size()));
        callGraphCountCheckMap.put("Standard", new Pair<>(cgComposition.getMethod(), standardMethods.size()));
        callGraphCountCheckMap.put("Listener",
                new Pair<>(cgComposition.getListener(), (listenerCallbacks.size() + possibleCallbacks.size()))
                                  );
        for (Map.Entry<String, Pair<Integer, Integer>> entry : callGraphCountCheckMap.entrySet()) {
            if (!entry.getValue().getLeft().equals(entry.getValue().getRight())) {
                problemFound = true;
                LOGGER.warn(entry.getKey() + " methods count does not match call graph composition count.");
            }
        }

        Composition cfgComposition = new Composition(this.getControlFlowGraph());
        if (cfgComposition.getDummy() != 0) {
            problemFound = true;
            LOGGER.warn("Control flow graph contains dummy methods.");
        }

        Map<String, Integer> controlFlowGraphEmptyCheckMap = new HashMap<>();
        controlFlowGraphEmptyCheckMap.put("units", cfgComposition.getUnit());
        controlFlowGraphEmptyCheckMap.put("standard methods", cfgComposition.getMethod());
        controlFlowGraphEmptyCheckMap.put("lifecycle methods", cfgComposition.getLifecycle());
        controlFlowGraphEmptyCheckMap.put("controls", cfgComposition.getControl());
        for (Map.Entry<String, Integer> entry : controlFlowGraphEmptyCheckMap.entrySet()) {
            if (entry.getValue() == 0) {
                problemFound = true;
                LOGGER.warn("Control flow graph contains no " + entry.getKey() + ".");
            }
        }

        if (cfgComposition.getVertex() !=
                (cfgComposition.getUnit() + cfgComposition.getMethod() + cgComposition.getCallback() +
                        cgComposition.getListener() + cgComposition.getLifecycle() + cgComposition.getDummy() +
                        cfgComposition.getControl())) {
            problemFound = true;
            LOGGER.warn("Control flow graph vertex type counts do not match total vertex count.");
        }

        if (this.getDroidControls().getControls().size() != cfgComposition.getControl()) {
            problemFound = true;
            LOGGER.warn("Control flow graph does not contain all the found controls.");
        }

        Map<String, Pair<Integer, Integer>> cfgCountCheckMap = new HashMap<>();
        cfgCountCheckMap.put("Dummy", new Pair<>(cgComposition.getDummy(), cfgComposition.getDummy()));
        cfgCountCheckMap.put("Lifecycle", new Pair<>(cgComposition.getLifecycle(), cfgComposition.getLifecycle()));
        cfgCountCheckMap.put("Callback", new Pair<>(cgComposition.getCallback(), cfgComposition.getCallback()));
        cfgCountCheckMap.put("Method", new Pair<>(cgComposition.getMethod(), cfgComposition.getMethod()));
        cfgCountCheckMap.put("Listener", new Pair<>(cgComposition.getListener(), cfgComposition.getListener()));
        for (Map.Entry<String, Pair<Integer, Integer>> entry : cfgCountCheckMap.entrySet()) {
            if (!entry.getValue().getLeft().equals(entry.getValue().getRight())) {
                problemFound = true;
                LOGGER.warn(entry.getKey() + " count does not match between control flow graph and call graph count.");
            }
        }

        LOGGER.info("Looking for duplicate methods in control flow graph.");
        List<String> duplicates = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
            if (vertex instanceof MethodVertex) {
                String methodSignature = ((MethodVertex) vertex).getMethodSignature().replace("'", "");
                if (set.contains(methodSignature)) {
                    duplicates.add(methodSignature);
                } else {
                    set.add(methodSignature);
                }
            }
        }
        if (!duplicates.isEmpty()) {
            problemFound = true;
            for (String methodSignature : duplicates) {
                LOGGER.warn("Method " + methodSignature + " is duplicated in the graph.");
            }
        }

        if (!problemFound) {
            LOGGER.info("Content verification finished without any problems.");
        }

        LOGGER.info("(" + timer.end() + ") Content verification took " + timer.secondsDuration() + " second(s).");
    }
}
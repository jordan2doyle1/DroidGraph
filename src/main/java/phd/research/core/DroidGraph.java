package phd.research.core;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.gml.GmlImporter;
import org.jgrapht.util.SupplierUtil;
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
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.MultiMap;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class DroidGraph {

    private static final Logger logger = LoggerFactory.getLogger(DroidGraph.class);
    @NotNull
    private final DroidControls droidControls;
    @NotNull
    private final MultiMap<SootClass, AndroidCallbackDefinition> collectedCallbacks;
    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;

    @API
    public DroidGraph(File collectedCallbacksFile, File apk, File gmlFile) throws XmlPullParserException, IOException {
        this(collectedCallbacksFile, new DroidControls(collectedCallbacksFile, apk), gmlFile, true);
    }

    public DroidGraph(File collectedCallbacksFile, DroidControls droidControls, File gmlFile) throws IOException {
        this(collectedCallbacksFile, droidControls, gmlFile, true);
    }

    public DroidGraph(File collectedCallbacksFile, DroidControls droidControls,
            File gmlFile, boolean addMissingComponents)
            throws IOException {
        this.collectedCallbacks = CollectedCallbacksSerializer.deserialize(collectedCallbacksFile).getCallbackMethods();
        this.droidControls = Objects.requireNonNull(droidControls);
        this.generateGraphs(gmlFile, addMissingComponents);
    }

    public static Collection<Vertex> getControlsVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof ControlVertex && v.hasVisit()).collect(Collectors.toSet());
    }

    public static Collection<Vertex> getControlsNotVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof ControlVertex && !v.hasVisit()).collect(Collectors.toSet());
    }

    public static Collection<Vertex> getMethodsVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof MethodVertex && v.getType() != Type.dummy && v.hasVisit())
                .collect(Collectors.toSet());
    }

    public static Collection<Vertex> getMethodsNotVisited(Collection<Vertex> vertices) {
        return vertices.stream().filter(v -> v instanceof MethodVertex && v.getType() != Type.dummy && !v.hasVisit())
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
        return vertices.stream().filter(v -> v.getType() == Type.unit && ((UnitVertex) v).getUnit().equals(unit))
                .findFirst().orElse(null);
    }

    public static Vertex getMethodVertex(SootMethod method, Set<Vertex> vertices) {
        return vertices.stream().filter(v ->
                        (v.getType() == Type.listener || v.getType() == Type.lifecycle || v.getType() == Type.dummy ||
                                v.getType() == Type.method) && ((MethodVertex) v).getMethod().equals(method)).findFirst()
                .orElse(null);
    }

    @API
    public static Vertex getControlVertex(SootClass activity, String controlName, Set<Vertex> vertices) {
        return vertices.stream().filter(v -> v.getType() == Type.control).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getControlActivity().equals(activity) &&
                        v.getControl().getControlResource().getResourceName().equals(controlName)).findFirst()
                .orElse(null);
    }

    @API
    public static Vertex getControlVertex(SootClass activity, int controlId, Set<Vertex> vertices) {
        return vertices.stream().filter(v -> v.getType() == Type.control).map(v -> (ControlVertex) v)
                .filter(v -> v.getControl().getControlActivity().equals(activity) &&
                        v.getControl().getControlResource().getResourceID() == controlId).findFirst().orElse(null);
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

    @Nonnull
    public Graph<Vertex, DefaultEdge> getCallGraph() {
        return this.callGraph;
    }

    @Nonnull
    @API
    public Graph<Vertex, DefaultEdge> getControlFlowGraph() {
        return this.controlFlowGraph;
    }

    @API
    public void generateGraphs(boolean addMissingVertices) throws IOException {
        this.generateGraphs(null, addMissingVertices);
    }

    public void generateGraphs(File gmlGraph, boolean addMissingVertices) throws IOException {
        this.callGraph = generateGraph(gmlGraph);
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

    @API
    public boolean visitCFGMethodVertex(SootMethod method) {
        for (Vertex vertex : this.getControlFlowGraph().vertexSet()) {
            if ((vertex.getType() == Type.method || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.listener || vertex.getType() == Type.dummy ||
                    vertex.getType() == Type.other) && ((MethodVertex) vertex).getMethod().equals(method)) {
                vertex.visit();
                return true;
            }
        }
        return false;
    }

    @API
    public Type getMethodType(SootMethod method) throws RuntimeException {
        if (method.getDeclaringClass().getName().equals("dummyMainClass")) {
            return Type.dummy;
        } else if (Filter.isLifecycleMethod(method)) {
            return Type.lifecycle;
        } else if (Filter.isListenerMethod(this.collectedCallbacks, method) ||
                Filter.isPossibleListenerMethod(this.collectedCallbacks, method)) {
            return Type.listener;
        } else if (Filter.isOtherCallbackMethod(collectedCallbacks, method)) {
            return Type.other;
        } else if (Filter.isValidMethod(method)) {
            return Type.method;
        } else {
            throw new RuntimeException(String.format("Found method %s with unknown type.", method));
        }
    }

    public MethodVertex createMethodVertex(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass")) {
            return new DummyVertex(method);
        } else if (Filter.isLifecycleMethod(method)) {
            return new LifecycleVertex(method);
        } else if (Filter.isListenerMethod(this.collectedCallbacks, method) ||
                Filter.isPossibleListenerMethod(this.collectedCallbacks, method)) {
            return new ListenerVertex(method);
        } else if (Filter.isOtherCallbackMethod(this.collectedCallbacks, method)) {
            return new MethodVertex(Type.other, method);
        } else if (Filter.isValidMethod(method)) {
            return new MethodVertex(method);
        } else {
            throw new RuntimeException(String.format("Found method %s with an unknown type.", method));
        }
    }

    @SuppressWarnings("unused")
    @Deprecated
    private Graph<Vertex, DefaultEdge> generateGraph(CallGraph sootCallGraph) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        sootCallGraph.forEach(edge -> {
            String dummyMain = "dummyMainClass";
            if ((Filter.isValidMethod(edge.src()) || edge.src().getDeclaringClass().getName().equals(dummyMain)) &&
                    (Filter.isValidMethod(edge.tgt()) || edge.tgt().getDeclaringClass().getName().equals(dummyMain))) {
                Vertex srcVertex = createMethodVertex(edge.src());
                graph.addVertex(srcVertex);
                Vertex tgtVertex = createMethodVertex(edge.tgt());
                graph.addVertex(tgtVertex);
                graph.addEdge(srcVertex, tgtVertex);
            }
        });

        return graph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(File gmlGraph) {
        return importGMLGraph(gmlGraph);
    }

//    private Graph<Vertex, DefaultEdge> generateGraph() throws IOException {
//        File gmlGraphFile = new File("");
//        if (!gmlGraphFile.exists()) {
//            throw new IOException("");
//        }
//        return importGMLGraph(gmlGraphFile);
//    }

    private Graph<Vertex, DefaultEdge> importGMLGraph(File gmlFile) {
        Graph<String, DefaultEdge> tempGraph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        GmlImporter<String, DefaultEdge> importer = new GmlImporter<>();
        Map<String, Map<String, Attribute>> attrs = new HashMap<>();
        importer.addVertexAttributeConsumer((p, a) -> {
            Map<String, Attribute> map = attrs.computeIfAbsent(p.getFirst(), k -> new HashMap<>());
            map.put(p.getSecond(), a);
        });
        importer.importGraph(tempGraph, gmlFile);

        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        tempGraph.vertexSet().forEach(v -> {
            try {
                Vertex vertex = createVertexForLabel(attrs.get(v).get("label").getValue());
                if (vertex != null) {
                    graph.addVertex();
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        tempGraph.edgeSet().forEach(
                e -> {
                    try {
                        Vertex src =
                                createVertexForLabel(attrs.get(tempGraph.getEdgeSource(e)).get("label").getValue());
                        Vertex tgt = createVertexForLabel(attrs.get(tempGraph.getEdgeTarget(e)).get("label").getValue());
                        if (src != null && tgt != null) {
                            graph.addEdge(src, tgt);
                        }
                    } catch (FileNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                });

        // TODO: Verify graph contents.
        return graph;
    }

    private Vertex createVertexForLabel(String label) throws FileNotFoundException {
        Pattern pattern = Pattern.compile("L(.+);->(.+)\\((.*)\\)(.+;|\\[?\\[?.) ?(\\[.+])?");
        Matcher matcher = pattern.matcher(label);

        if (matcher.find()) {
            String className = matcher.group(1).trim().replace("/", ".");
            String methodName = matcher.group(2).trim();
            String parameters = matcher.group(3).trim();
            List<String> paramList = new ArrayList<>(
                    parameters.equals("") ? Collections.emptyList() : Arrays.asList(parameters.split(" ")));
            paramList.replaceAll(this::convertBytecodeTypeToJimple);
            String methodReturn = convertBytecodeTypeToJimple(matcher.group(4).trim());

            String methodSignature = buildMethodSignature(className, methodName, methodReturn, paramList);
            SootMethod method = Scene.v().grabMethod(methodSignature);

            if (method == null) {
                logger.error("Failed to find method: " + methodSignature);
                return null;
            }

            SootClass clazz = Scene.v().getSootClassUnsafe(Scene.signatureToClass(methodSignature));
            if (clazz == null) {
                logger.error("");
                return null;
            }

            if (Filter.isValidClass(clazz)) {
                this.createMethodVertex(method);
            }
        }

        return null;
    }

    private String buildMethodSignature(String clazz, String method, String returnType, Collection<String> parameters) {
        StringBuilder builder = new StringBuilder("<");
        builder.append(clazz).append(": ").append(returnType).append(" ").append(method).append("(");

        parameters.forEach(parameter-> builder.append(parameter).append(","));
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.append(")>").toString();
    }

    private String convertBytecodeTypeToJimple(String type) {
        if (type.startsWith("[")) {
            return convertBytecodeTypeToJimple(type.substring(1)) + "[]";
        } else if (type.startsWith("L")) {
            return type.substring(1, type.length() - 1).replace("/", ".");
        } else {
            return convertBytecodePrimitiveToJimple(type);
        }
    }

    private String convertBytecodePrimitiveToJimple(String primitiveType) {
        switch (primitiveType) {
            case "V":
                return "void";
            case "Z":
                return "boolean";
            case "B":
                return "byte";
            case "C":
                return "char";
            case "S":
                return "short";
            case "I":
                return "int";
            case "J":
                return "long";
            case "F":
                return "float";
            case "D":
                return "double";
            default:
                throw new RuntimeException("Primitive type provided not recognised: " + primitiveType);
        }
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
                    logger.error(String.format("Listener method %s not found in the graph.", method));
                }
            });
        });

        JimpleBasedInterproceduralCFG jimpleCFG = new JimpleBasedInterproceduralCFG();
        Set<Vertex> graphVertices = new HashSet<>(graph.vertexSet());
        graphVertices.stream().filter(vertex -> vertex.getType() != Type.control).forEach(vertex -> {
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
                                        logger.error(String.format("Caller %s not found in the graph.", caller));
                                    }
                                    Vertex calleeVertex = getMethodVertex(callee, graph.vertexSet());
                                    if (calleeVertex == null) {
                                        logger.error(String.format("Callee %s not found in the graph.", callee));
                                        if (addMissingVertices) {
                                            logger.info(String.format("Adding %s method into the graph.", callee));
                                            graph.addVertex(createMethodVertex(callee));
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
            logger.error(String.format("Found %s controls that are not in the graph.", missingControls.size()));
            if (addVertices) {
                logger.info(String.format("Adding %s controls into the graph.", missingControls.size()));
                missingControls.forEach(control -> graph.addVertex(new ControlVertex(control)));
            }
        }

        Collection<SootMethod> missingMethods = new HashSet<>();
        Scene.v().getClasses().stream().filter(Filter::isValidClass)
                .forEach(clazz -> clazz.getMethods().forEach(method -> {
                    Vertex vertex = createMethodVertex(method);
                    if (!graph.containsVertex(vertex)) {
                        missingMethods.add(method);
                    }
                }));

        if (!missingMethods.isEmpty()) {
            logger.error(String.format("Found %s methods that are not in the graph.", missingMethods.size()));
            if (addVertices) {
                logger.info(String.format("Adding %s methods into the graph.", missingMethods.size()));
                missingMethods.forEach(method -> graph.addVertex(createMethodVertex(method)));
            }
        }

        return graph;
    }
}
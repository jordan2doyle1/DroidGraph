package phd.research.core;

import heros.solver.Pair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Type;
import phd.research.graph.ContentFilter;
import phd.research.graph.GraphWriter;
import phd.research.graph.UnitGraph;
import phd.research.helper.Control;
import phd.research.jGraph.JGraph;
import phd.research.jGraph.Vertex;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.callbacks.CallbackDefinition;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */
public class ApplicationAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationAnalysis.class);

    private SetupApplication application;
    private ProcessManifest manifest;
    private final ContentFilter contentFilter;
    private final Set<Control> controls;

    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;
    private JGraph jCallGraph;
    private JGraph jControlFlowGraph;

    private final Set<SootMethod> lifecycleMethods;
    private final Set<SootMethod> callbackMethods;
    private final Set<SootMethod> listenerMethods;

    public ApplicationAnalysis(InfoflowAndroidConfiguration flowDroidConfig) {
        runFlowDroid(flowDroidConfig);

        this.contentFilter = new ContentFilter();
        this.controls = new HashSet<>();

        this.lifecycleMethods = new HashSet<>();
        this.callbackMethods = new HashSet<>();
        this.listenerMethods = new HashSet<>();

        this.extractUI();

        runAnalysis();
    }

    public Set<SootMethod> getLifecycleMethods() {
        return lifecycleMethods;
    }

    public Set<SootMethod> getCallbackMethods() {
        return callbackMethods;
    }

    public Set<SootMethod> getListenerMethods() {
        return listenerMethods;
    }

    public Set<Control> getControls() {
        return this.controls;
    }

    public void runFlowDroid(InfoflowAndroidConfiguration flowDroidConfig) {
        this.application = new SetupApplication(flowDroidConfig);
        this.application.constructCallgraph();

        this.manifest = processManifest();
    }

    public void runAnalysis() {
        this.callGraph = generateJGraphT(Scene.v().getCallGraph());
        this.controlFlowGraph = generateJGraphT(this.callGraph);
    }

    public Graph<Vertex, DefaultEdge> getCallGraph() {
        if (this.callGraph == null)
            this.callGraph = generateJGraphT(Scene.v().getCallGraph());

        return this.callGraph;
    }

    public Graph<Vertex, DefaultEdge> getControlFlowGraph() {
        if (this.controlFlowGraph == null)
            this.controlFlowGraph = generateJGraphT(this.getCallGraph());

        return this.controlFlowGraph;
    }

    public JGraph getJCallGraph() {
        if (this.jCallGraph == null)
            this.jCallGraph = generateJGraph(Scene.v().getCallGraph());

        return this.jCallGraph;
    }

    public JGraph getJControlFlowGraph() {
        if (this.jControlFlowGraph == null)
            this.jControlFlowGraph = generateJGraph(this.getJCallGraph());

        return this.jControlFlowGraph;
    }

    public String getBasePackageName() {
        return this.manifest.getPackageName();
    }

    public ContentFilter getContentFilter() {
        return this.contentFilter;
    }

    public Set<SootClass> getEntryPointClasses() {
        Set<SootClass> entryPoints = new HashSet<>();

        for (String entryPoint : this.manifest.getEntryPointClasses()) {
            SootClass entryPointClass = getClass(entryPoint);
            if (entryPointClass != null) {
                entryPoints.add(entryPointClass);
            }
        }

        return entryPoints;
    }

    public Set<SootClass> getLaunchActivities() {
        Set<SootClass> launchActivities = new HashSet<>();

        for (AXmlNode activity : manifest.getLaunchableActivityNodes()) {
            if (activity.hasAttribute("name")) {
                //TODO: Could be excluding valid activities if the app developer has not provided the name attribute.
                String activityName = activity.getAttribute("name").getValue().toString();
                SootClass launchActivity = getClass(activityName);
                if (launchActivity != null) {
                    launchActivities.add(launchActivity);
                }
            }
        }

        return launchActivities;
    }

    //TODO: Make JGraph implement JGraphT interface so that we can combine both generate methods.
    //TODO: Make a convert method to convert between the two graph objects.
    public Graph<Vertex, DefaultEdge> generateJGraphT(CallGraph sootCallGraph) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        Iterator<MethodOrMethodContext> sourceItr = sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (this.contentFilter.isValidMethod(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = this.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), formatLabel(srcMethod.toString()), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (this.contentFilter.isValidMethod(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = this.getMethodType(tgtMethod);
                        Vertex tgtVertex = new Vertex(tgtMethod.hashCode(), formatLabel(tgtMethod.toString()), methodType,
                                tgtMethod);
                        graph.addVertex(tgtVertex);

                        graph.addEdge(srcVertex, tgtVertex);
                    }
                }
            }
        }

        return graph;
    }

    //TODO: Make JGraph implement JGraphT interface so that we can combine both generate methods.
    //TODO: Make a convert method to convert between the two graph objects.
    public JGraph generateJGraph(CallGraph sootCallGraph) {
        JGraph graph = new JGraph();

        Iterator<MethodOrMethodContext> sourceItr = sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (this.contentFilter.isValidMethod(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = this.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), formatLabel(srcMethod.toString()), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (this.contentFilter.isValidMethod(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = this.getMethodType(tgtMethod);
                        Vertex tgtVertex = new Vertex(tgtMethod.hashCode(), formatLabel(tgtMethod.toString()), methodType,
                                tgtMethod);
                        graph.addVertex(tgtVertex);

                        graph.addEdge(srcVertex, tgtVertex);
                    }
                }
            }
        }

        return graph;
    }

    //TODO: Make JGraph implement JGraphT interface so that we can combine both generate methods.
    //TODO: Make a convert method to convert between the two graph objects.
    public Graph<Vertex, DefaultEdge> generateJGraphT(Graph<Vertex, DefaultEdge> callGraph) {
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(graph, callGraph);
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

    //TODO: Make JGraph implement JGraphT interface so that we can combine both generate methods.
    //TODO: Make a convert method to convert between the two graph objects.
    public JGraph generateJGraph(JGraph callGraph) {
        JGraph graph = new JGraph();
        graph.addGraph(callGraph);
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

    @SuppressWarnings("CommentedOutCode")
    private String formatLabel(String label) {
        System.out.println("Original Label: " + label);

//        // TODO: Do I need this commented out code?
//        if (label.contains("dummyMainMethod"))
//            label = label.replaceAll("_", ".");
//
//        // TODO: Do I need to get all the package names in order to do this??
//        for (String packageName : packageManager.getAllPackages()) {
//            if (!packageName.equals(this.getBasePackageName()) && !packageName.equals("")) {
//                if (label.contains(packageName))
//                    label = label.replace(packageName + ".", "");
//            }
//        }
//
//        if (label.contains(this.getBasePackageName()))
//            label = label.replace(this.getBasePackageName() + ".", "");

        return label;
    }

    protected void outputMethods(String format) {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (this.contentFilter.isValidClass(sootClass)) {
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String name = sootClass.getName().substring(sootClass.getName().lastIndexOf(".") + 1)
                                + "_" + method.getName();

                        GraphWriter graphWriter = new GraphWriter();
                        graphWriter.writeGraph(format, name, unitGraph.getJUnitGraphT());
                    }
                }
            }
        }
    }

    private SootClass getClass(String search) {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (search.equals(sootClass.getName())) {
                return sootClass;
            }
        }
        return null;
    }

    private ARSCFileParser retrieveResources() {
        ARSCFileParser resources = new ARSCFileParser();
        try {
            resources.parse(FrameworkMain.getApk());
        } catch (IOException e) {
            logger.error("Error getting resources: " + e.getMessage());
        }

        return resources;
    }

    private LayoutFileParser retrieveLayoutFileParser() {
        ProcessManifest manifest = processManifest();
        if (manifest != null) {
            LayoutFileParser layoutFileParser = new LayoutFileParser(manifest.getPackageName(), retrieveResources());
            layoutFileParser.parseLayoutFileDirect(FrameworkMain.getApk());
            return layoutFileParser;
        }
        return null;
    }

    private ProcessManifest processManifest() {
        ProcessManifest manifest;
        try {
            manifest = new ProcessManifest(FrameworkMain.getApk());
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return null;
        }
        return manifest;
    }

    private Vertex getInterfaceControl(Vertex vertex) {
        Control control = this.getControl(vertex.getSootMethod());
        if (control != null) {
            return new Vertex(control.hashCode(), formatLabel(String.valueOf(control.getId())), Type.control, vertex.getSootMethod());
        } else {
            logger.error("No control for " + vertex.getLabel());
        }

        return null;
    }

    /**
     * For some reason JGraphT does not have a method to retrieve individual vertices that already exist in the graph.
     * Instead, this method searched a list of all the vertices contained within the graph for the required
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

    public void extractUI() {
        Map<SootClass, Set<CallbackDefinition>> customCallbacks = new HashMap<>();

        for (Pair<SootClass, AndroidCallbackDefinition> callbacks : this.application.droidGraphCallbacks) {
            if (customCallbacks.containsKey(callbacks.getO1())) {
                customCallbacks.get(callbacks.getO1()).add(callbacks.getO2());
            } else {
                Set<CallbackDefinition> callbackDefinitions = new HashSet<>();
                callbackDefinitions.add(callbacks.getO2());
                customCallbacks.put(callbacks.getO1(), callbackDefinitions);
            }
        }

        logger.info("Retrieving callback methods and linking controls...");
        logger.info("FlowDroid found " + customCallbacks.size() + " classes with callbacks.");
        Set<SootMethod> controlCallbackSet = new HashSet<>();

        for (Map.Entry<SootClass, Set<CallbackDefinition>> entry : customCallbacks.entrySet()) {
            Set<CallbackDefinition> callbacks = entry.getValue();
            logger.info("FlowDroid found " + callbacks.size() + " callbacks in \"" + entry.getKey().getShortName() + "\"");

            for (CallbackDefinition callback : callbacks) {
                String targetName = callback.getTargetMethod().getName();
                if (targetName.contains("onCreate") || targetName.contains("onStart") || targetName.contains("onResume")
                        || targetName.contains("onRestart") || targetName.contains("onPause") ||
                        targetName.contains("onStop") || targetName.contains("onDestroy")) {
                    this.lifecycleMethods.add(callback.getTargetMethod());
                    logger.debug("Found lifecycle method \"" + callback.getTargetMethod().getName() + "\".");
                } else {
                    SootMethod method = callback.getTargetMethod();

                    if (this.contentFilter.isValidPackage(method.getDeclaringClass().getPackageName())) {
                        this.listenerMethods.add(method);
                        logger.debug("Found listener method \"" + method.getName() + "\".");
                        controlCallbackSet.add(method);
                    } else {
                        this.callbackMethods.add(method);
                        logger.debug("Found callback method \"" + method.getName() + "\".");
                    }
                }
            }
        }

        logger.info("Found " + this.lifecycleMethods.size() + " lifecycle methods.");
        logger.info("Found " + this.callbackMethods.size() + " callback methods");
        logger.info("Found " + this.listenerMethods.size() + " listener methods");

        Set<Pair<String, AndroidLayoutControl>> nullControls = new HashSet<>();
        LayoutFileParser lfp = retrieveLayoutFileParser();
        if (lfp != null) {
            for (Pair<String, AndroidLayoutControl> userControl : lfp.getUserControls()) {
                AndroidLayoutControl control = userControl.getO2();

                if (control.getClickListener() != null) {
                    SootMethod clickListener = searchCallbackMethods(control.getClickListener());
                    if (clickListener != null) {
                        this.controls.add(new Control(control.hashCode(), control.getID(), null, clickListener));
                        controlCallbackSet.remove(clickListener);
                        logger.debug(control.getID() + " linked to \"" + clickListener.getDeclaringClass().getShortName()
                                + "." + clickListener.getName() + "\".");
                    } else {
                        logger.error("Problem linking controls with listeners: Two callback methods have the same name.");
                    }
                } else {
                    if (control.getID() != -1) {
                        nullControls.add(userControl);
                    }
                }
            }
        }

        logger.debug("Found " + nullControls.size() + " controls without listeners.");
        logger.debug("Found " + controlCallbackSet.size() + " unassigned listeners.");

        Iterator<SootMethod> iterator = controlCallbackSet.iterator();
        while (iterator.hasNext()) {
            SootMethod listener = iterator.next();
            phd.research.helper.Pair<String, Integer> interfaceID = getInterfaceID(listener);

            if (interfaceID != null) {
                Iterator<Pair<String, AndroidLayoutControl>> controlIterator = nullControls.iterator();
                while (controlIterator.hasNext()) {
                    AndroidLayoutControl control = controlIterator.next().getO2();

                    if (control.getID() == interfaceID.getRight()) {
                        this.controls.add(new Control(control.hashCode(), control.getID(), interfaceID.getLeft(),
                                listener));
                        controlIterator.remove();
                        iterator.remove();
                        logger.debug(control.getID() + ":" + interfaceID.getLeft() + " linked to \"" +
                                listener.getDeclaringClass().getShortName() + "." + listener.getName() + "\".");
                        break;
                    }
                }
            }
        }

        logger.debug(nullControls.size() + " controls without listeners remaining.");
        logger.debug(controlCallbackSet.size() + " unassigned listeners remaining.");

        if (controlCallbackSet.size() > 0) {
            for (SootMethod sootMethod : controlCallbackSet) {
                logger.error("No control linked to \"" + sootMethod.getDeclaringClass().getShortName() + "." +
                        sootMethod.getName() + "\".");
            }
        }

        if (nullControls.size() > 0) {
            for (Pair<String, AndroidLayoutControl> nullControl : nullControls) {
                AndroidLayoutControl control = nullControl.getO2();
                logger.error("No listener linked to " + control.getID() + ".");
            }
        }
    }

    private SootMethod searchCallbackMethods(String methodName) {
        SootMethod foundMethod = null;

        for (SootMethod method : this.listenerMethods) {
            if (method.getName().equals(methodName)) {
                if (foundMethod != null) {
                    logger.error("Found multiple callback methods with the same name: " + methodName + ".");
                    return null;
                }
                foundMethod = method;
            }
        }
        return foundMethod;
    }

    private phd.research.helper.Pair<String, Integer> getInterfaceID(final SootMethod callback) {
        PatchingChain<Unit> units = callback.getActiveBody().getUnits();
        final String[] arguments = {""};
        phd.research.helper.Pair<String, Integer> interfaceID = null;

        for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
            Unit unit = iterator.next();

            unit.apply(new AbstractStmtSwitch() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    super.caseInvokeStmt(stmt);

                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    if (invokeExpr.getMethod().getName().equals("println")) {
                        arguments[0] = invokeExpr.getArg(0).toString();
                    }
                }
            });
        }

        String argument = arguments[0].replace("\"", "");
        String textID = null;
        String numberID = null;

        if ((argument.contains(":"))) {
            String[] id = argument.split(":");
            textID = "id/" + id[0];
            numberID = id[1];
        } else if (! argument.equals("")) {
            numberID = argument;
        }

        if (numberID == null) {
            logger.error("Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "."
                    + callback.getName() + "\", no value found in the method. ");
            return null;
        }

        try {
            interfaceID = new phd.research.helper.Pair<>(textID, Integer.parseInt(numberID));
            logger.info("Found control ID " + argument + " in \"" + callback.getDeclaringClass().getShortName() + "." +
                    callback.getName() + "\".");
        } catch (NumberFormatException e) {
            logger.error("Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "."
                    + callback.getName() + "\", could not parse integer: " + numberID + ".");
        }

        return interfaceID;
    }

    public Control getControl(SootMethod callback) {
        for (Control control : this.controls) {
            if (control.getClickListener().equals(callback)) {
                return control;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public Control getControl(String textId) {
        for (Control control : this.controls) {
            if (control.getTextId().equals(textId)) {
                return control;
            }
        }
        return null;
    }

    public Type getMethodType(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass"))
            return Type.dummyMethod;
        else if (isListener(method))
            return Type.listener;
        else if (isLifecycle(method))
            return Type.lifecycle;
        else if (isCallback(method))
            return Type.callback;
        else
            return Type.method;
    }

    public boolean isLifecycle(SootMethod method) {
        return this.lifecycleMethods.contains(method);
    }

    public boolean isCallback(SootMethod method) {
        return this.callbackMethods.contains(method);
    }

    public boolean isListener(SootMethod method) {
        return this.listenerMethods.contains(method);
    }
}
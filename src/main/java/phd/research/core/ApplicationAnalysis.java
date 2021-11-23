package phd.research.core;

import heros.solver.Pair;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Format;
import phd.research.enums.Type;
import phd.research.graph.Filter;
import phd.research.graph.UnitGraph;
import phd.research.graph.Writer;
import phd.research.helper.Control;
import phd.research.jGraph.Vertex;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.Chain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */
public class ApplicationAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationAnalysis.class);
    private static CollectedCallbacks callbacks;

    private final Filter filter;

    private Set<Control> controls;
    private SetupApplication application;
    private ProcessManifest manifest;

    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;

    public ApplicationAnalysis() {
        runFlowDroid();
        this.filter = new Filter();
    }

    public static CollectedCallbacks getCallbacks() {
        if (callbacks != null)
            return callbacks;
        else {
            try {
                callbacks = CollectedCallbacksSerializer.deserialize(
                        new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"));
            } catch (FileNotFoundException e) {
                logger.error("Error CollectedCallbacks File Not Found:" + e.getMessage());
            }
        }

        return callbacks;
    }

    public void runAnalysis() {
        logger.info("Running graph generation...");
        System.out.println("Running graph generation...");
        long startTime = System.currentTimeMillis();

        this.callGraph = generateGraph(Scene.v().getCallGraph());
        this.controlFlowGraph = generateGraph(this.callGraph);

        long endTime = System.currentTimeMillis();
        logger.info("Graph generation took " + (endTime - startTime) / 1000 + " second(s).");
        System.out.println("Graph generation took " + (endTime - startTime) / 1000 + " second(s).");
    }

    public Filter getFilter() {
        return this.filter;
    }

    public Set<Control> getControls() {
        if (this.controls == null)
            this.controls = getUIControls();

        return this.controls;
    }

    public SetupApplication getApplication() {
        return this.application;
    }

    public String getBasePackageName() {
        return this.manifest.getPackageName();
    }

    public Graph<Vertex, DefaultEdge> getCallGraph() {
        if (this.callGraph == null)
            this.callGraph = generateGraph(Scene.v().getCallGraph());

        return this.callGraph;
    }

    public Graph<Vertex, DefaultEdge> getControlFlowGraph() {
        if (this.controlFlowGraph == null)
            this.controlFlowGraph = generateGraph(this.getCallGraph());

        return this.controlFlowGraph;
    }

    public Set<SootClass> getEntryPointClasses() {
        Set<SootClass> entryPoints = new HashSet<>();

        for (String entryPoint : this.manifest.getEntryPointClasses()) {
            SootClass entryPointClass = getClass(entryPoint);
            if (entryPointClass != null)
                entryPoints.add(entryPointClass);
        }

        return entryPoints;
    }

    public Set<SootClass> getLaunchActivities() {
        Set<SootClass> launchActivities = new HashSet<>();

        for (AXmlNode activity : manifest.getLaunchableActivityNodes()) {
            if (activity.hasAttribute("name")) {
                // Could be excluding valid activities if the app developer has not provided the name attribute.
                String activityName = activity.getAttribute("name").getValue().toString();
                SootClass launchActivity = getClass(activityName);
                if (launchActivity != null)
                    launchActivities.add(launchActivity);
            }
        }

        return launchActivities;
    }

    private void runFlowDroid() {
        logger.info("Running FlowDroid...");
        System.out.println("Running FlowDroid...");
        long startTime = System.currentTimeMillis();

        initializeSoot();
        InfoflowAndroidConfiguration configuration = getFlowDroidConfiguration();
        this.application = new SetupApplication(configuration);
        this.application.constructCallgraph();
        this.manifest = processManifest();

        long endTime = System.currentTimeMillis();
        logger.info("FlowDroid took " + (endTime - startTime) / 1000 + " second(s).");
        System.out.println("FlowDroid took " + (endTime - startTime) / 1000 + " second(s).");
    }

    private InfoflowAndroidConfiguration getFlowDroidConfiguration() {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig().setAndroidPlatformDir(FrameworkMain.getAndroidPlatform());
        config.getAnalysisFileConfig().setSourceSinkFile(System.getProperty("user.dir") + "/SourcesAndSinks.txt");
        config.getAnalysisFileConfig().setTargetAPKFile(FrameworkMain.getApk());
        config.getCallbackConfig().setSerializeCallbacks(true);
        config.getCallbackConfig().setCallbacksFile(FrameworkMain.getOutputDirectory() + "CollectedCallbacks");
        return config;
    }

    private void initializeSoot() {
        G.reset();  // Clean up any old Soot instance we may have

        soot.options.Options.v().set_no_bodies_for_excluded(true);
        soot.options.Options.v().set_allow_phantom_refs(true);
        soot.options.Options.v().set_output_format(soot.options.Options.output_format_none);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_process_dir(Collections.singletonList(FrameworkMain.getApk()));
        soot.options.Options.v().set_android_jars(FrameworkMain.getAndroidPlatform());
        soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk_class_jimple);
        soot.options.Options.v().set_keep_offset(false);
        soot.options.Options.v().set_keep_line_number(false);
        soot.options.Options.v().set_throw_analysis(soot.options.Options.throw_analysis_dalvik);
        soot.options.Options.v().set_process_multiple_dex(true);
        soot.options.Options.v().set_ignore_resolution_errors(true);

        Scene.v().addBasicClass("android.view.View", soot.SootClass.BODIES);

        // Set Soot configuration options. Note this needs to be done before computing the classpath.
        // (SA) Exclude classes of android.* will cause layout class cannot be loaded for layout file based callback
        // analysis. Added back the exclusion, because removing it breaks calls to Android SDK stubs.
        // (JD) Remove the android.* and androidx.* within FlowDroid and see what happens.
        List<String> excludeList = new LinkedList<>(Arrays.asList("java.*", "javax.*", "sun.*", "org.apache.*",
                "org.eclipse.*", "soot.*", "android.*", "androidx.*"));
        soot.options.Options.v().set_exclude(excludeList);

        soot.options.Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(FrameworkMain.getAndroidPlatform(),
                FrameworkMain.getApk()));
        Main.v().autoSetOptions();

        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("wjpp").apply();

        // Patch the callgraph to support additional edges. We do this now, because during callback discovery, the
        // context-insensitive callgraph algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
    }

    private SootClass getClass(String search) {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (search.equals(sootClass.getName()))
                return sootClass;
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

    protected void outputMethods(Format format) throws Exception {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (this.filter.isValidClass(sootClass)) {
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String name = sootClass.getName().substring(sootClass.getName().lastIndexOf(".") + 1)
                                + "_" + method.getName();

                        Writer writer = new Writer();
                        writer.writeGraph(format, name, unitGraph.getGraph());
                    }
                }
            }
        }
    }

    // TODO: Improve the labels, remove parameter package names and shorten dummy main method labels.
    private String getLabel(SootMethod method) {
        return method.toString().replace(method.getDeclaringClass().getPackageName() + ".", "");
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
            if (vertex.getID() == id)
                return vertex;
        }

        return null;
    }

    private Type getMethodType(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass"))
            return Type.dummyMethod;
        else if (this.filter.isListenerMethod(method))
            return Type.listener;
        else if (this.filter.isLifecycleMethod(method))
            return Type.lifecycle;
        else if (this.filter.isOtherCallbackMethod(method))
            return Type.callback;
        else
            return Type.method;
    }

    private Set<Control> getUIControls() {
        Set<Control> uiControls = new HashSet<>();

        LayoutFileParser layoutParser = retrieveLayoutFileParser();
        if (layoutParser != null) {
            for (Pair<String, AndroidLayoutControl> controlPair : layoutParser.getUserControls()) {
                // TODO: Add layout file to the Control class.
                AndroidLayoutControl control = controlPair.getO2();
                if (control.getClickListener() != null) {
                    SootMethod clickListener = searchCallbackMethods(control.getClickListener());
                    if (clickListener != null)
                        uiControls.add(new Control(control.hashCode(), control.getID(), null, clickListener));
                } else {
                    // TODO: why are some of the Control ID's -1.
                    if (control.getID() != -1)
                        uiControls.add(new Control(control.hashCode(), control.getID(), null, null));
                }
            }
        }

        return uiControls;
    }

    private SootMethod searchCallbackMethods(String methodName) {
        if (callbacks == null)
            callbacks = getCallbacks();

        SootMethod foundMethod = null;

        for (SootClass currentClass : callbacks.getCallbackMethods().keySet()) {
            for (AndroidCallbackDefinition callbackDefinition : callbacks.getCallbackMethods().get(currentClass)) {
                if (callbackDefinition.getTargetMethod().getName().equals(methodName)) {
                    if (foundMethod != null) {
                        logger.error("Found multiple callback methods with the name: " + methodName + ".");
                        return null;
                    }
                    foundMethod = callbackDefinition.getTargetMethod();
                }
            }
        }

        return foundMethod;
    }

    private Control getControl(SootMethod callback) {
        if (this.controls == null)
            this.controls = getUIControls();

        for (Control control : this.controls) {
            if (control.getClickListener().equals(callback))
                return control;
        }

        return null;
    }

    @SuppressWarnings("unused")
    private Control getControl(String textId) {
        if (this.controls == null)
            this.controls = getUIControls();

        for (Control control : this.controls) {
            if (control.getTextId().equals(textId))
                return control;
        }

        return null;
    }

    // TODO: Implement callback hash set.

    private void extractUI() {
        // TODO: Find alternative to instrumentation.
//        Map<SootClass, Set<CallbackDefinition>> customCallbacks = new HashMap<>();
//
//        for (Pair<SootClass, AndroidCallbackDefinition> callbacks : this.application.droidGraphCallbacks) {
//            if (customCallbacks.containsKey(callbacks.getO1())) {
//                customCallbacks.get(callbacks.getO1()).add(callbacks.getO2());
//            } else {
//                Set<CallbackDefinition> callbackDefinitions = new HashSet<>();
//                callbackDefinitions.add(callbacks.getO2());
//                customCallbacks.put(callbacks.getO1(), callbackDefinitions);
//            }
//        }

        // Getting all the callbacks without the lifecycle methods.

        Set<SootMethod> callbackMethods = new HashSet<>(); //this.callbacks;

        Set<Control> controls = new HashSet<>();
        Set<Pair<String, AndroidLayoutControl>> nullControls = new HashSet<>();
        LayoutFileParser lfp = retrieveLayoutFileParser();
        if (lfp != null) {
            for (Pair<String, AndroidLayoutControl> userControl : lfp.getUserControls()) {
                AndroidLayoutControl control = userControl.getO2();
                if (control.getClickListener() != null) {
                    SootMethod clickListener = searchCallbackMethods(control.getClickListener());
                    if (clickListener != null) {
                        controls.add(new Control(control.hashCode(), control.getID(), null, clickListener));
                        callbackMethods.remove(clickListener);
                        logger.debug(control.getID() + " linked to \"" + clickListener.getDeclaringClass().getShortName()
                                + "." + clickListener.getName() + "\".");
                    } else
                        logger.error("Problem linking controls with listeners: Two callback methods have the same name.");
                } else {
                    if (control.getID() != -1)
                        nullControls.add(userControl);
                }
            }
        }

        Iterator<SootMethod> iterator = callbackMethods.iterator();
        while (iterator.hasNext()) {
            SootMethod listener = iterator.next();
            phd.research.helper.Pair<String, Integer> interfaceID = getInterfaceID(listener);

            if (interfaceID != null) {
                Iterator<Pair<String, AndroidLayoutControl>> controlIterator = nullControls.iterator();
                while (controlIterator.hasNext()) {
                    AndroidLayoutControl control = controlIterator.next().getO2();

                    if (control.getID() == interfaceID.getRight()) {
                        controls.add(new Control(control.hashCode(), control.getID(), interfaceID.getLeft(),
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

        if (callbackMethods.size() > 0) {
            for (SootMethod sootMethod : callbackMethods) {
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
                    if (invokeExpr.getMethod().getName().equals("println"))
                        arguments[0] = invokeExpr.getArg(0).toString();
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
        } else if (!argument.equals(""))
            numberID = argument;

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

    private Vertex getInterfaceControl(Vertex vertex) {
        Control control = this.getControl(vertex.getSootMethod());
        if (control != null)
            return new Vertex(control.hashCode(), String.valueOf(control.getId()), Type.control, vertex.getSootMethod());
        else
            logger.error("No control for " + vertex.getLabel());

        return null;
    }

    private Set<SootMethod> checkGraph(Graph<Vertex, DefaultEdge> graph) {
        // TODO: Verify graph is complete and correct (all methods present?, all vertices have input edges?, etc.)
        // TODO: Print to the console if a problem or anomaly is found.
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> notInGraph = new HashSet<>();

        if (graph == null)
            graph = this.getControlFlowGraph();

        for (SootClass sootClass : classes) {
            if (this.filter.isValidClass(sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    Type methodType = this.getMethodType(method);
                    Vertex vertex = new Vertex(method.hashCode(), getLabel(method), methodType, method);
                    if (!graph.containsVertex(vertex))
                        notInGraph.add(method);
                }
            }
        }

        if (notInGraph.isEmpty()) {
            logger.info("All methods in the graph.");
            System.out.println("All methods in the graph.");
        } else {
            logger.error(notInGraph.size() + " methods are not in the graph. ");
            System.out.println(notInGraph.size() + " methods are not in the graph. ");

            for (SootMethod method : notInGraph) {
                System.out.println(method.toString());
            }
        }

        return notInGraph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(CallGraph sootCallGraph) {
        // TODO: Confirm Call Graph Generation is correct?
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        Iterator<MethodOrMethodContext> sourceItr = sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (this.filter.isValidMethod(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = this.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), getLabel(srcMethod), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (this.filter.isValidMethod(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = this.getMethodType(tgtMethod);
                        Vertex tgtVertex = new Vertex(tgtMethod.hashCode(), getLabel(tgtMethod), methodType,
                                tgtMethod);
                        graph.addVertex(tgtVertex);

                        graph.addEdge(srcVertex, tgtVertex);
                    }
                }
            }
        }

        // checkGraph(graph);
        return graph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(Graph<Vertex, DefaultEdge> callGraph) {
        // TODO: Confirm Control Flow Graph Generation is correct?
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
                } else
                    logger.error("Failed to find interface control for vertex: \"" + vertex.getLabel() + "\". ");
            }

            if (vertex.getType() == Type.listener || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.method) {
                if (vertex.getSootMethod().hasActiveBody()) {
                    UnitGraph unitGraph = new UnitGraph(vertex.getSootMethod().getActiveBody());
                    Graph<Vertex, DefaultEdge> methodSubGraph = unitGraph.getGraph();
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
                            if (callVertex != null && calledVertex != null)
                                graph.addEdge(callVertex, calledVertex);
                        }
                    }
                }
            } else if (vertex.getType() != Type.statement && vertex.getType() != Type.control
                    && vertex.getType() != Type.dummyMethod)
                logger.error("Found unknown vertex type \"" + vertex.getType() + "\": " + vertex.getLabel());
        }

        // checkGraph(graph);
        return graph;
    }
}
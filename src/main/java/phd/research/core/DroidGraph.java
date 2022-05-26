package phd.research.core;

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
import phd.research.helper.Status;
import phd.research.jGraph.Vertex;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
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
    private static ARSCFileParser resources;
    private static LayoutFileParser layoutParser;
    private static ProcessManifest manifest;

    private SetupApplication application;
    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;
    private Set<Control> controls;

    public ApplicationAnalysis() {
        runFlowDroid();
    }

    public static CollectedCallbacks getCallbacks() {
        if (callbacks != null)
            return callbacks;

        try {
            callbacks = CollectedCallbacksSerializer.deserialize(
                    new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"));
        } catch (FileNotFoundException e) {
            logger.error("Error CollectedCallbacks File Not Found:" + e.getMessage());
        }

        return callbacks;
    }

    public static Set<SootClass> getEntryPointClasses() {
        if (manifest == null)
            manifest = ApplicationAnalysis.getManifest();

        Set<SootClass> entryPoints = new HashSet<>();

        if (manifest != null) {
            for (String entryPoint : manifest.getEntryPointClasses()) {
                SootClass entryPointClass = Scene.v().getSootClass(entryPoint);
                if (entryPointClass != null)
                    entryPoints.add(entryPointClass);
            }
        }

        return entryPoints;
    }

    public static Set<SootClass> getLaunchActivities() {
        if (manifest == null)
            manifest = ApplicationAnalysis.getManifest();

        Set<SootClass> launchActivities = new HashSet<>();
        if (manifest != null) {
            for (AXmlNode activity : manifest.getLaunchableActivityNodes()) {
                if (activity.hasAttribute("name")) {
                    // WARNING: Excluding valid launch activities if the developer doesn't provide the name attribute.
                    String activityName = activity.getAttribute("name").getValue().toString();
                    SootClass launchActivity = Scene.v().getSootClass(activityName);
                    if (launchActivity != null)
                        launchActivities.add(launchActivity);
                }
            }
        }

        return launchActivities;
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

    public void test() {
        printMethodUnitsToConsole("com.example.android.lifecycle.ActivityA$1", "onClick");

        SootClass sc = Scene.v().getSootClass("com.example.android.lifecycle.ActivityA$1");
        for (SootMethod method : sc.getMethods() ) {
            if (method.getName().contains("onClick")) {
                if (method.hasActiveBody()) {
                    PatchingChain<Unit> units = method.getActiveBody().getUnits();
                    for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
                        Unit unit = iterator.next();
                        unit.apply(new AbstractStmtSwitch<Stmt>() {
                            @Override
                            public void caseAssignStmt(AssignStmt stmt) {
                                super.caseAssignStmt(stmt);

                                // Get the left and right operand, if the right operand is a virtual invoke with name.
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("getId")) {
                                    System.out.println(unit.getUseBoxes());
                                }
                            }
                        });
                    }
//                    for (Unit unit : method.getActiveBody().getUnits()) {
//                        System.out.println(unit.toString());
//                        if (unit.)
//                    }
                }
            }
        }
    }

    public Set<Control> getControls() {
        if (this.controls == null)
            this.controls = getUIControls();

        return this.controls;
    }

    private Set<Control> getUIControls() {
        return new HashSet<>();
    }

    public SetupApplication getApplication() {
        return this.application;
    }

    public String getBasePackageName() {
        if (manifest == null)
            manifest = ApplicationAnalysis.getManifest();

        if (manifest != null)
            return manifest.getPackageName();

        return null;
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

    protected static void outputMethods(Format format) throws Exception {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (Filter.isValidClass(null, null, sootClass)) {
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String name = sootClass.getName().substring(sootClass.getName().lastIndexOf(".") + 1)
                                + "_" + method.getName();

                        Writer.writeGraph(format, FrameworkMain.getOutputDirectory(), name, unitGraph.getGraph());
                    }
                }
            }
        }
    }

    private static ARSCFileParser getResources() {
        if (resources != null)
            return resources;

        resources = new ARSCFileParser();
        try {
            resources.parse(FrameworkMain.getApk());
        } catch (IOException e) {
            logger.error("Error getting resources: " + e.getMessage());
        }

        return resources;
    }

    private static ProcessManifest getManifest() {
        if (manifest != null)
            return manifest;

        try {
            manifest = new ProcessManifest(FrameworkMain.getApk());
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return null;
        }

        return manifest;
    }

    private static LayoutFileParser getLayoutFileParser() {
        if (layoutParser != null)
            return layoutParser;

        if (manifest == null)
            manifest = ApplicationAnalysis.getManifest();

        if (manifest != null) {
            layoutParser = new LayoutFileParser(manifest.getPackageName(), ApplicationAnalysis.getResources());
            layoutParser.parseLayoutFileDirect(FrameworkMain.getApk());
            return layoutParser;
        }

        return null;
    }

    private static InfoflowAndroidConfiguration getFlowDroidConfiguration() {
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

    private static void initializeSoot() {
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
        soot.options.Options.v().set_output_dir(FrameworkMain.getOutputDirectory());

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

    private static String removePackageName(String name) {
        int index = name.lastIndexOf(".");

        if (name.contains("dummyMainMethod"))
            index = name.lastIndexOf("_");

        if(index != -1) {
            name = name.replace(name.substring(0, index + 1), "");
        }

        return name;
    }

    private static String getLabel(SootMethod method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<").append(ApplicationAnalysis.removePackageName(method.getDeclaringClass().getName()))
                .append(": ").append(ApplicationAnalysis.removePackageName(method.getReturnType().toString()))
                .append(" ").append(ApplicationAnalysis.removePackageName(method.getName())).append("(");

        List<soot.Type> parameters = method.getParameterTypes();
        for (int i = 0; i < method.getParameterCount(); i++) {
            stringBuilder.append(ApplicationAnalysis.removePackageName(parameters.get(i).toString()));
            if(i != (method.getParameterCount() - 1) )
                stringBuilder.append(",");
        }

        stringBuilder.append(")>");
        return stringBuilder.toString();
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
    private static Vertex getVertex(int id, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getID() == id)
                return vertex;
        }

        return null;
    }

    private static Type getMethodType(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass"))
            return Type.dummyMethod;
        else if (Filter.isListenerMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
            return Type.listener;
        else if (Filter.isLifecycleMethod(method))
            return Type.lifecycle;
        else if (Filter.isOtherCallbackMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
            return Type.other;
        else
            return Type.method;
    }

    private void runFlowDroid() {
        logger.info("Running FlowDroid...");
        System.out.println("Running FlowDroid...");
        long startTime = System.currentTimeMillis();

        ApplicationAnalysis.initializeSoot();
        InfoflowAndroidConfiguration configuration = ApplicationAnalysis.getFlowDroidConfiguration();
        this.application = new SetupApplication(configuration);
        this.application.constructCallgraph();

        long endTime = System.currentTimeMillis();
        logger.info("FlowDroid took " + (endTime - startTime) / 1000 + " second(s).");
        System.out.println("FlowDroid took " + (endTime - startTime) / 1000 + " second(s).");
    }

    private Control getControl(SootMethod callback) {
        if (this.controls == null)
            this.controls = getUIControls();

        for (Control control : this.controls) {
            if (control.getClickListener() != null)
                if (control.getClickListener().equals(callback))
                    return control;
        }

        return null;
    }

    private Control getControl(String resourceName) {
        if (this.controls == null)
            this.controls = getUIControls();

        for (Control control : this.controls) {
            if (control.getControlResource() != null)
                if (control.getControlResource().getResourceName().equals(resourceName))
                    return control;
        }

        return null;
    }

    // For Testing Purposes Only. E.g. className: com.example.android.lifecycle.ActivityA, methodName: onCreate
    private void printMethodUnitsToConsole(String className, String methodName) {
        System.out.println("**** Printing method units: " + className + " " + methodName + " ****");
        SootClass sc = Scene.v().getSootClass(className);
        for (SootMethod method : sc.getMethods() ) {
            if (method.getName().contains(methodName)) {
                if (method.hasActiveBody()) {
                    for (Unit unit : method.getActiveBody().getUnits()) {
                        System.out.println(unit.toString());
                    }
                }
            }
        }
        System.out.println("**** END ****");
    }



    private Vertex getInterfaceControl(Vertex vertex) {
        Control control = this.getControl(vertex.getSootMethod());
        if (control != null)
            return new Vertex(control.hashCode(), String.valueOf(control.getControlResource().getResourceID()),
                    Type.control, vertex.getSootMethod());
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
            if (Filter.isValidClass(null, null, sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    Type methodType = ApplicationAnalysis.getMethodType(method);
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

            if (Filter.isValidMethod(null, null, srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = ApplicationAnalysis.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), getLabel(srcMethod), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (Filter.isValidMethod(null, null, tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = ApplicationAnalysis.getMethodType(tgtMethod);
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
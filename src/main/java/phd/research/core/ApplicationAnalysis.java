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
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
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
import soot.util.MultiMap;

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
            ApplicationAnalysis.getManifest();

        Set<SootClass> entryPoints = new HashSet<>();

        for (String entryPoint : manifest.getEntryPointClasses()) {
            //SootClass entryPointClass = getClass(entryPoint);
            SootClass entryPointClass = Scene.v().getSootClass(entryPoint);
            if (entryPointClass != null)
                entryPoints.add(entryPointClass);
        }

        return entryPoints;
    }

    public static Set<SootClass> getLaunchActivities() {
        if (manifest == null)
            ApplicationAnalysis.getManifest();

        Set<SootClass> launchActivities = new HashSet<>();
        for (AXmlNode activity : manifest.getLaunchableActivityNodes()) {
            if (activity.hasAttribute("name")) {
                // WARNING: Excluding valid launch activities if the developer doesn't provide the name attribute.
                String activityName = activity.getAttribute("name").getValue().toString();
                SootClass launchActivity = Scene.v().getSootClass(activityName);
                if (launchActivity != null)
                    launchActivities.add(launchActivity);
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

    public Set<Control> getControls() {
        if (this.controls == null)
            this.controls = getUIControls();

        return this.controls;
    }

    public SetupApplication getApplication() {
        return this.application;
    }

    public String getBasePackageName() {
        if (manifest == null)
            ApplicationAnalysis.getManifest();

        return manifest.getPackageName();
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

        ProcessManifest manifest = ApplicationAnalysis.getManifest();

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

    protected static void outputMethods(Format format) throws Exception {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (Filter.isValidClass(sootClass)) {
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String name = sootClass.getName().substring(sootClass.getName().lastIndexOf(".") + 1)
                                + "_" + method.getName();

                        Writer.writeGraph(format, name, unitGraph.getGraph());
                    }
                }
            }
        }
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

    private static String removePackageName(String name) {
        int index = name.lastIndexOf(".");

        if (name.contains("dummyMainMethod"))
            index = name.lastIndexOf("_");

        if(index != -1) {
            name = name.replace(name.substring(0, index + 1), "");
        }

        return name;
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
        else if (Filter.isListenerMethod(method))
            return Type.listener;
        else if (Filter.isLifecycleMethod(method))
            return Type.lifecycle;
        else if (Filter.isOtherCallbackMethod(method))
            return Type.other;
        else
            return Type.method;
    }

    private static String getResourceName(String name) {
        int index = name.lastIndexOf("/");

        if(index != -1)
            name = name.replace(name.substring(0, index + 1), "");

        if(name.contains(".xml"))
            name = name.replace(".xml", "");

        return name;
    }

    private static ARSCFileParser.AbstractResource getResourceById(int resourceId) {
        if (resources == null)
            resources = getResources();

        ARSCFileParser.ResType resType = resources.findResourceType(resourceId);
        if (resType == null)
            return null;

        List<ARSCFileParser.AbstractResource> foundResources = resType.getAllResources(resourceId);
        if (foundResources.isEmpty())
            return null;

        if (foundResources.size() > 1)
            logger.error("Error: Multiple resources with ID " + resourceId + ", returning the first.");

        return foundResources.get(0);
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

    private SootClass findLayoutClass(int layoutId) {
        for (SootClass entryClass : ApplicationAnalysis.getEntryPointClasses()) {
            SootClass layoutClass = findLayoutClassRecursively(layoutId, entryClass);

            if (layoutClass != null) {
                return layoutClass;
            }
        }

        Status.reset();
        return null;
    }

    private SootClass findLayoutClassRecursively(int layoutId, SootClass entryClass) {
        SootClass layoutClass = findSetContentView(layoutId, entryClass);

        if (layoutClass == null && entryClass.hasSuperclass()) {
            layoutClass = findLayoutClassRecursively(layoutId, entryClass.getSuperclassUnsafe());
        }

        return layoutClass;
    }

    private SootClass findSetContentView(int layoutId, SootClass entryClass) {
        SootMethod onCreateMethod = null;
        try {
            onCreateMethod = entryClass.getMethodByName("onCreate");
        } catch (RuntimeException e) {
            logger.error("No onCreate Method Found: " + e);
        }

        if (onCreateMethod != null && onCreateMethod.hasActiveBody()) {
            PatchingChain<Unit> units = onCreateMethod.getActiveBody().getUnits();

            for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
                Unit unit = iterator.next();

                unit.apply(new AbstractStmtSwitch() {
                    @Override
                    public void caseInvokeStmt(InvokeStmt stmt) {
                        super.caseInvokeStmt(stmt);

                        InvokeExpr invokeExpr = stmt.getInvokeExpr();
                        if (invokeExpr.getMethod().getName().equals("setContentView")) {
                            if (invokeExpr.getArg(0).toString().equals(String.valueOf(layoutId)))
                                Status.foundClass(true);
                        }
                    }
                });

                if (Status.isClassFound()) {
                    Status.reset();
                    return entryClass;
                }
            }
        }

        return null;
    }

    private SootMethod findCallbackMethod(SootClass callbackClass, int id) {
        for (SootMethod method : callbackClass.getMethods()) {
            if (method != null && method.hasActiveBody()) {
                PatchingChain<Unit> units = method.getActiveBody().getUnits();

                //final Status test = new Status();
                // Don't use workarounds, instead create our own classes that inherit from FlowDroid.

                for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext();) {
                    Unit unit = iterator.next();

                    unit.apply(new AbstractStmtSwitch() {
                        @Override
                        public void caseInvokeStmt(InvokeStmt stmt) {
                            super.caseInvokeStmt(stmt);

                            if (Status.isViewFound()) {
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("<init>")) {
                                    Status.foundClass(true);
                                    Status.setFoundClass(invokeExpr.getMethod().getDeclaringClass());
                                }
                            }
                        }

                        @Override
                        public void caseAssignStmt(AssignStmt stmt) {
                            super.caseAssignStmt(stmt);

                            if (stmt.containsInvokeExpr()) {
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("findViewById")) {
                                    if (Integer.parseInt(invokeExpr.getArg(0).toString()) == id) {
                                        Status.foundView(true);
                                    }
                                }
                            }
                        }
                    });

                    if (Status.isViewFound() && Status.isClassFound()) {
                        if (Status.getFoundClass().getMethodCount() > 2)
                            logger.warn("Warning: Class contains multiple callback methods. Using the first method.");

                        for (SootMethod classMethod : Status.getFoundClass().getMethods()) {
                            if(!classMethod.getName().equals("<init>")) {
                                Status.reset();
                                return classMethod;
                            }
                        }
                    }
                }

                Status.reset();
            }
        }

        return null;
    }

    @SuppressWarnings("unused")
    private SootMethod searchForCallbackMethod(String methodName) {
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

    // TODO: Refactor from here...

    private SootMethod searchForCallbackMethod(SootClass callbackClass, String methodName) {
        SootMethod foundMethod = null;

        for (SootMethod method : callbackClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (foundMethod != null) {
                    logger.error("Found multiple callback methods with the name: " + methodName + ".");
                    return null;
                }
                foundMethod = method;
            }
        }

        return foundMethod;
    }

    private Set<Control> getUIControls() {
        Set<Control> uiControls = new HashSet<>();

        if (layoutParser == null)
            layoutParser = ApplicationAnalysis.getLayoutFileParser();

        assert layoutParser != null;
        for (String layoutFile : layoutParser.getUserControls().keySet()) {
            ARSCFileParser.AbstractResource layoutResource = resources.findResourceByName(
                    "layout", ApplicationAnalysis.getResourceName(layoutFile));

            for (AndroidLayoutControl control : layoutParser.getUserControls().get(layoutFile)) {
                if (control.getID() != -1) {
                    ARSCFileParser.AbstractResource controlResource = ApplicationAnalysis.getResourceById(
                            control.getID());

                    if (controlResource == null) {
                        logger.error("Error: No resource found with ID " + control.getID() + ".");
                        continue;
                    }

                    SootClass callbackClass = findLayoutClass(layoutResource.getResourceID());
                    if (callbackClass != null) {
                        SootMethod clickListener;

                        if (control.getClickListener() != null)
                            clickListener = searchForCallbackMethod(callbackClass, control.getClickListener());
                        else
                            clickListener = findCallbackMethod(callbackClass, control.getID());

                        if (clickListener == null)
                            logger.error("Error: Couldn't find click listener method with ID: " + control.getID());

                        uiControls.add(new Control(control.hashCode(), controlResource, layoutResource, clickListener));
                    }
                }
            }
        }

        return uiControls;
    }

    // Recursive method to search for variable assignments and ultimately end in a class declaration.
    // find method findViewById and pass assigned variable to recursive call.
    // find button declaration and pass assigned variable to recurve call.
    // find <init> method and pass assigned variable to recursive call.
    // find setOnClickListener method and return click listener method.



    @SuppressWarnings("unused")
    private void tempName0() {
        if (layoutParser == null)
            layoutParser = getLayoutFileParser();

        assert layoutParser != null;
        for (heros.solver.Pair<String, AndroidLayoutControl> controlPair : layoutParser.getUserControls()) {
            if (controlPair.getO1().contains("activity_a")) {
                System.out.println("Layout File: " + controlPair.getO1());
                String resourceName = ApplicationAnalysis.getResourceName(controlPair.getO1());
                for (ARSCFileParser.ResPackage resPackage : resources.getPackages()) {
                    for (ARSCFileParser.ResType type : resPackage.getDeclaredTypes()) {
                        if (type.getTypeName().equals("layout")) {
                            System.out.println("Found1: " + type.getResourceByName(resourceName));
                            for (ARSCFileParser.AbstractResource res : type.getAllResources()) {
                                System.out.println("Name: " + res.getResourceName());
                                if (res.getResourceName().equals(resourceName)) {
                                    System.out.println("Found2: " + res);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void tempName1() {
        SootClass sc = Scene.v().getSootClass(
                "com.example.android.lifecycle.ActivityA");
        for (SootMethod method : sc.getMethods() ) {
            if (method.getName().contains("onCreate")) {
                if (method.hasActiveBody()) {
                    for (Unit unit : method.getActiveBody().getUnits()) {
                        System.out.println(unit.toString());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    private void tempName2() {
        SootClass sc = Scene.v().getSootClass(
                "com.example.android.lifecycle.ActivityA");

        if (callbacks == null)
            callbacks = getCallbacks();

        MultiMap<SootClass, AndroidCallbackDefinition> callbackMap = callbacks.getCallbackMethods();
        Set<AndroidCallbackDefinition> callbackDefinitions = callbackMap.get(sc);

        for (AndroidCallbackDefinition callbackDefinition : callbackDefinitions) {
            if (callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                System.out.println("Method: " + callbackDefinition.getTargetMethod());
                System.out.println("Parent: " + callbackDefinition.getParentMethod());
            }
        }

        System.out.println();
        for (SootMethod method : sc.getMethods() ) {
            if (method.getName().contains("onCreate")) {
                if (method.hasActiveBody()) {
//                    Chain<Local> locals = method.getActiveBody().getLocals();
//                    for (Local local : locals) {
//                        List<ValueBox> uses = local.getUseBoxes();
//                        for (ValueBox use : uses) {
//                            System.out.println("Local:" + local + " - Use:" + use);
//                        }
//                    }
                    List<ValueBox> defs = method.getActiveBody().getUseBoxes();
                    for (ValueBox value : defs) {
                        System.out.println(value.getValue());
                    }
                    System.out.println();
//                    for (Unit unit : method.getActiveBody().getUnits()) {
//                        System.out.println(unit.toString());
//                    }
                }
            }
        }
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

    @SuppressWarnings("unused")
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

    // TODO: Implement callback hash set.

//    private void extractUI() {
//        // TODO: Find alternative to instrumentation.
////        Map<SootClass, Set<CallbackDefinition>> customCallbacks = new HashMap<>();
////
////        for (Pair<SootClass, AndroidCallbackDefinition> callbacks : this.application.droidGraphCallbacks) {
////            if (customCallbacks.containsKey(callbacks.getO1())) {
////                customCallbacks.get(callbacks.getO1()).add(callbacks.getO2());
////            } else {
////                Set<CallbackDefinition> callbackDefinitions = new HashSet<>();
////                callbackDefinitions.add(callbacks.getO2());
////                customCallbacks.put(callbacks.getO1(), callbackDefinitions);
////            }
////        }
//
//        // Getting all the callbacks without the lifecycle methods.
//
//        Set<SootMethod> callbackMethods = new HashSet<>(); //this.callbacks;
//
//        Set<Control> controls = new HashSet<>();
//        Set<Pair<String, AndroidLayoutControl>> nullControls = new HashSet<>();
//        LayoutFileParser lfp = retrieveLayoutFileParser();
//        if (lfp != null) {
//            for (Pair<String, AndroidLayoutControl> userControl : lfp.getUserControls()) {
//                AndroidLayoutControl control = userControl.getO2();
//                if (control.getClickListener() != null) {
//                    SootMethod clickListener = searchCallbackMethods(control.getClickListener());
//                    if (clickListener != null) {
//                        controls.add(new Control(control.hashCode(), control.getID(), null, -1, null, clickListener));
//                        callbackMethods.remove(clickListener);
//                        logger.debug(control.getID() + " linked to \"" + clickListener.getDeclaringClass().getShortName()
//                                + "." + clickListener.getName() + "\".");
//                    } else
//                        logger.error("Problem linking controls with listeners: Two callback methods have the same name.");
//                } else {
//                    if (control.getID() != -1)
//                        nullControls.add(userControl);
//                }
//            }
//        }
//
//        Iterator<SootMethod> iterator = callbackMethods.iterator();
//        while (iterator.hasNext()) {
//            SootMethod listener = iterator.next();
//            phd.research.helper.Pair<String, Integer> interfaceID = getInterfaceID(listener);
//
//            if (interfaceID != null) {
//                Iterator<Pair<String, AndroidLayoutControl>> controlIterator = nullControls.iterator();
//                while (controlIterator.hasNext()) {
//                    AndroidLayoutControl control = controlIterator.next().getO2();
//
//                    if (control.getID() == interfaceID.getRight()) {
//                        controls.add(new Control(control.hashCode(), control.getID(), interfaceID.getLeft(), -1, null,
//                                listener));
//                        controlIterator.remove();
//                        iterator.remove();
//                        logger.debug(control.getID() + ":" + interfaceID.getLeft() + " linked to \"" +
//                                listener.getDeclaringClass().getShortName() + "." + listener.getName() + "\".");
//                        break;
//                    }
//                }
//            }
//        }
//
//        if (callbackMethods.size() > 0) {
//            for (SootMethod sootMethod : callbackMethods) {
//                logger.error("No control linked to \"" + sootMethod.getDeclaringClass().getShortName() + "." +
//                        sootMethod.getName() + "\".");
//            }
//        }
//
//        if (nullControls.size() > 0) {
//            for (Pair<String, AndroidLayoutControl> nullControl : nullControls) {
//                AndroidLayoutControl control = nullControl.getO2();
//                logger.error("No listener linked to " + control.getID() + ".");
//            }
//        }
//    }

    // IGNORE BELOW: A PROBLEM FOR ANOTHER DAY!

    @SuppressWarnings("unused")
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
            return new Vertex(control.hashCode(), String.valueOf(control.getControlResource().getResourceID()),
                    Type.control, vertex.getSootMethod());
        else
            logger.error("No control for " + vertex.getLabel());

        return null;
    }

    @SuppressWarnings("unused")
    private Set<SootMethod> checkGraph(Graph<Vertex, DefaultEdge> graph) {
        // TODO: Verify graph is complete and correct (all methods present?, all vertices have input edges?, etc.)
        // TODO: Print to the console if a problem or anomaly is found.
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> notInGraph = new HashSet<>();

        if (graph == null)
            graph = this.getControlFlowGraph();

        for (SootClass sootClass : classes) {
            if (Filter.isValidClass(sootClass)) {
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

            if (Filter.isValidMethod(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = ApplicationAnalysis.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), getLabel(srcMethod), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (Filter.isValidMethod(tgtMethod) ||
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
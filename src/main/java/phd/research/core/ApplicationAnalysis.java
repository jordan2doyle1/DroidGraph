package phd.research.core;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Type;
import phd.research.graph.Composition;
import phd.research.graph.GraphWriter;
import phd.research.graph.UnitGraph;
import phd.research.helper.Control;
import phd.research.jGraph.JGraph;
import phd.research.jGraph.Vertex;
import soot.*;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class ApplicationAnalysis {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationAnalysis.class);

    private final ContentFilter contentFilter;
    private InterfaceManager interfaceManager;

    private SetupApplication application;
    private ProcessManifest manifest;
    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;
    private JGraph jCallGraph;
    private JGraph jControlFlowGraph;

    public ApplicationAnalysis(InfoflowAndroidConfiguration flowDroidConfig) {
        runFlowDroid(flowDroidConfig);

        this.contentFilter = new ContentFilter();
        this.interfaceManager = new InterfaceManager();
        this.interfaceManager.extractUI(application, this.contentFilter);

        runAnalysis();
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
            generateJGraphT(this.getCallGraph());

        return this.controlFlowGraph;
    }

    public JGraph getJCallGraph() {
        if (this.jCallGraph == null)
            this.jCallGraph = generateJGraph(Scene.v().getCallGraph());

        return this.jCallGraph;
    }

    public JGraph getJControlFlowGraph() {
        if (this.jControlFlowGraph == null)
            generateJGraph(this.getJCallGraph());

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
                Type methodType = this.interfaceManager.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), formatLabel(srcMethod.toString()), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (this.contentFilter.isValidMethod(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = interfaceManager.getMethodType(tgtMethod);
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

    public JGraph generateJGraph(CallGraph sootCallGraph) {
        JGraph graph = new JGraph();

        Iterator<MethodOrMethodContext> sourceItr = sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (this.contentFilter.isValidMethod(srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = interfaceManager.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), formatLabel(srcMethod.toString()), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (this.contentFilter.isValidMethod(tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = interfaceManager.getMethodType(tgtMethod);
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

    protected void printAnalysisDetails() {
        System.out.println("-------------------------------- Analysis Details ---------------------------------\n");

        System.out.println("Base Package Name: " + getBasePackageName());
//        System.out.println("Number of Packages: " + this.packageManager.filteredCount() + " (Total: " +
//                packageManager.packageCount() + ")");
        System.out.println();

//        System.out.println("Number of Entry Points: " + this.classManager.entryPointCount());
//        System.out.println("Number of Launching Activities: " + this.classManager.launchActivityCount());
//        System.out.println("Number of Classes: " + this.classManager.filteredCount() + " (Total: " +
//                this.classManager.classCount() + ")");
        System.out.println();

//        System.out.println("Number of Methods: " + this.methodManager.filteredCount() + " (Total: " +
//                this.methodManager.methodCount() + ")");
        System.out.println();

        System.out.println("Number of Lifecycle Methods: " + this.interfaceManager.lifecycleCount());
        System.out.println("Number of System Callbacks: " + this.interfaceManager.callbackCount());
        System.out.println("Number of Callback Methods: " + this.interfaceManager.listenerCount());
        System.out.println("Number of Callback ID's: " + this.interfaceManager.controlCount());
        System.out.println();

        System.out.println("Interface Callback Table");
        System.out.println(this.interfaceManager.getControlListenerTable());

        System.out.println("Call Graph Composition Table");
        Composition callGraphComposition = new Composition(this.callGraph);
        System.out.println(callGraphComposition);
        JGraph jCallGraph = generateJGraph(Scene.v().getCallGraph());
        Composition jCallGraphComposition = new Composition(jCallGraph);
        if (!callGraphComposition.equals(jCallGraphComposition))
            System.out.println(jCallGraphComposition);

        System.out.println("Control Flow Graph Composition Table");
        Composition controlFlowGraphComposition = new Composition(this.controlFlowGraph);
        System.out.println(controlFlowGraphComposition);
        JGraph jControlFlowGraph = generateJGraph(jCallGraph);
        Composition jControlFlowGraphComposition = new Composition(jControlFlowGraph);
        if (!controlFlowGraphComposition.equals(jControlFlowGraphComposition))
                System.out.println(jControlFlowGraphComposition);

        System.out.println("\n-----------------------------------------------------------------------------------\n");
    }

    private String formatLabel(String label) {
        System.out.println("Original Label: " + label);

//        PackageManager packageManager = PackageManager.getInstance();
//
//        if (label.contains("dummyMainMethod"))
//            label = label.replaceAll("_", ".");
//
//        for (String packageName : packageManager.getAllPackages()) {
//            if (!packageName.equals(packageManager.getBasename()) && !packageName.equals("")) {
//                if (label.contains(packageName))
//                    label = label.replace(packageName + ".", "");
//            }
//        }
//
//        if (label.contains(packageManager.getBasename()))
//            label = label.replace(packageManager.getBasename() + ".", "");

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
        Control control = this.interfaceManager.getControl(vertex.getSootMethod());
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
}
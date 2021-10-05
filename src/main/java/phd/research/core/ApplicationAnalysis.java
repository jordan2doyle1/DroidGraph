package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.graph.Callgraph;
import phd.research.graph.ControlFlowGraph;
import soot.Scene;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;

import java.io.IOException;

/**
 * @author Jordan Doyle
 */
public class GraphManager {

    private static final Logger logger = LoggerFactory.getLogger(GraphManager.class);
    private static final GraphManager instance = new GraphManager();

    private String appName;
    private Callgraph callGraph;
    private ControlFlowGraph controlFlowGraph;

    private GraphManager() {
    }

    public void runSoot(InfoflowAndroidConfiguration configuration) {
        SetupApplication app = new SetupApplication(configuration);
        app.constructCallgraph();

        PackageManager.getInstance().start();
        ClassManager.getInstance().start();
        MethodManager.getInstance().start();
        InterfaceManager.getInstance().extractUI(app);
        GraphManager.getInstance().start();
    }
    public static GraphManager getInstance() {
        return instance;
    }

    public void start() {
        this.appName = retrieveAppName();
        this.callGraph = new Callgraph(Scene.v().getCallGraph());
        this.controlFlowGraph = new ControlFlowGraph(this.callGraph);
    }

    public String getAppName() {
        return this.appName;
    }

    public Callgraph getCallGraph() {
        return this.callGraph;
    }

    public ControlFlowGraph getControlFlowGraph() {
        return this.controlFlowGraph;
    }

    private String retrieveAppName() {
        logger.info("Retrieving application name...");
        String applicationName;

        ProcessManifest manifest;
        try {
            manifest = new ProcessManifest(FrameworkMain.getApk());
            applicationName = manifest.getApplication().getName();
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return null;
        }

        logger.info("Application name is \"" + applicationName + "\"");
        return applicationName;
    }

    protected void printAnalysisDetails() {
        System.out.println("-------------------------------- Analysis Details ---------------------------------\n");

        PackageManager packageManager = PackageManager.getInstance();
        System.out.println("Base Package Name: " + packageManager.getBasename());
        System.out.println("Number of Packages: " + packageManager.filteredCount() + " (Total: " +
                packageManager.packageCount() + ")");
        System.out.println();

        ClassManager classManager = ClassManager.getInstance();
        System.out.println("Number of Entry Points: " + classManager.entryPointCount());
        System.out.println("Number of Launching Activities: " + classManager.launchActivityCount());
        System.out.println("Number of Classes: " + classManager.filteredCount() + " (Total: " +
                classManager.classCount() + ")");
        System.out.println();

        MethodManager methodManager = MethodManager.getInstance();
        System.out.println("Number of Methods: " + methodManager.filteredCount() + " (Total: " +
                methodManager.methodCount() + ")");
        System.out.println();

        InterfaceManager interfaceManager = InterfaceManager.getInstance();
        System.out.println("Number of Lifecycle Methods: " + interfaceManager.lifecycleCount());
        System.out.println("Number of System Callbacks: " + interfaceManager.callbackCount());
        System.out.println("Number of Callback Methods: " + interfaceManager.listenerCount());
        System.out.println("Number of Callback ID's: " + interfaceManager.controlCount());
        System.out.println();
        System.out.println("Interface Callback Table");
        System.out.println(interfaceManager.getControlListenerTable());

        GraphManager graphManager = GraphManager.getInstance();
        System.out.println("Call Graph Composition Table");
        System.out.println(graphManager.getCallGraph().getGraphCompositionTable());

        System.out.println("Control Flow Graph Composition Table");
        System.out.println(graphManager.getControlFlowGraph().getGraphCompositionTable());

        System.out.println("\n-----------------------------------------------------------------------------------\n");
    }
}
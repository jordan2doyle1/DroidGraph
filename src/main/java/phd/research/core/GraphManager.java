package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.graph.Callgraph;
import phd.research.graph.ControlFlowGraph;
import soot.Scene;
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
            manifest = new ProcessManifest(FrameworkMain.getAPK());
            applicationName = manifest.getApplication().getName();
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return null;
        }

        logger.info("Application name is \"" + applicationName + "\"");
        return applicationName;
    }
}
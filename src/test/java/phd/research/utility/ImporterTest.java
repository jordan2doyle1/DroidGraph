package phd.research.utility;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Before;
import org.junit.Test;
import phd.research.core.DroidGraph;
import phd.research.singletons.Settings;
import phd.research.vertices.AndroGuardVertex;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Doyle
 */

public class ImporterTest {

    private static final String inputDirectory =
            System.getProperty("user.dir") + File.separator + "samples" + File.separator;

    private Settings settings;

    @Before
    public void setUp() throws IOException {
        this.settings = Settings.v();
        this.settings.setApkFile(new File(ImporterTest.inputDirectory + "ActivityLifecycle.apk"));
        this.settings.setCallGraphFile(new File(ImporterTest.inputDirectory + "ActivityLifecycle.gml"));
    }

    @Test
    public void testImportAndroGuardGraph() {
        Importer.importAndroGuardGraph(this.settings.getCallGraphFile());
    }

    @Test
    public void testConvertAndFilterAndroGuardGraph() {
        Graph<AndroGuardVertex, DefaultEdge> graph = Importer.importAndroGuardGraph(this.settings.getCallGraphFile());
        Importer.convertAndFilterAndroGuardGraph(graph);
    }

    @Test
    public void testImportDroidGraph() throws IOException {
        this.settings.setImportControlFlowGraph(new File(ImporterTest.inputDirectory + "ActivityLifecycle.json"));
        Importer.importDroidGraph(this.settings.getContolFlowGraphFile());

        DroidGraph droidGraph = new DroidGraph();

        assertEquals("Wrong number of vertices imported.", 8038, droidGraph.getControlFlowGraph().vertexSet().size());
        assertEquals("Wrong number of edges imported.", 7932, droidGraph.getControlFlowGraph().edgeSet().size());
    }
}
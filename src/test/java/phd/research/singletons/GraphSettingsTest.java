package phd.research.singletons;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Format;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Jordan Doyle
 */

public class GraphSettingsTest {

    private static final String workingDirectory = System.getProperty("user.dir") + File.separator + "samples";

    private GraphSettings settings;

    @Before
    public void setUp() throws IOException {
        this.settings = GraphSettings.v();
        this.settings.setApkFile(
                new File(GraphSettingsTest.workingDirectory + File.separator + "Activity_Lifecycle_1.apk"));
        this.settings.setCallGraphFile(
                new File(GraphSettingsTest.workingDirectory + File.separator + "Activity_Lifecycle_1.gml"));
        this.settings.setOutputDirectory(new File(GraphSettingsTest.workingDirectory));
    }

    @After
    public void tearDown() {
        GraphSettings.resetDefaults();
    }

    @Test
    public void testConstructor() {
        assertTrue("Apk file does not exist.", this.settings.getApkFile().isFile());
        assertTrue("Platform directory does not exist.", this.settings.getPlatformDirectory().isDirectory());
        assertTrue("Output directory does not exist.", this.settings.getOutputDirectory().isDirectory());
        assertEquals("Format is not JSON", Format.JSON, this.settings.getFormat());
        assertTrue("Call graph file does not exist.", this.settings.getCallGraphFile().isFile());
        assertTrue("FlowDroid callbacks file does not exist.", this.settings.getFlowDroidCallbacksFile().isFile());
    }

    @Test(expected = IOException.class)
    public void testApkIOException() throws IOException {
        this.settings.setApkFile(new File(" "));
    }

    @Test(expected = IOException.class)
    public void testCallGraphIOException() throws IOException {
        this.settings.setCallGraphFile(new File(" "));
    }

    @Test(expected = IOException.class)
    public void testPlatformIOException() throws IOException {
        this.settings.setPlatformDirectory(new File(" "));
    }

    @Test(expected = IOException.class)
    public void testOutputIOException() throws IOException {
        this.settings.setOutputDirectory(new File(" "));
    }

    @Test
    public void testOutputUpdate() throws IOException {
        this.settings.setOutputDirectory(new File(GraphSettingsTest.workingDirectory));
        assertTrue("FlowDroid callback file not updated with output directory.",
                this.settings.getFlowDroidCallbacksFile().getAbsolutePath().contains(GraphSettingsTest.workingDirectory)
                  );
    }

    @Test
    public void testSetImportControlFlowGraph() throws IOException {
        assertFalse("Import control flow graph should not be enabled.", this.settings.isImportControlFlowGraph());
        assertNull("Control flow graph file should be null.", this.settings.getContolFlowGraphFile());

        this.settings.setImportControlFlowGraph(
                new File(GraphSettingsTest.workingDirectory + File.separator + "app_control_flow_graph.json"));

        assertTrue("Import control flow graph should be enabled.", this.settings.isImportControlFlowGraph());
        assertTrue("Control flow graph file does not exist.", this.settings.getContolFlowGraphFile().isFile());
    }

    @Test(expected = IOException.class)
    public void testControlFlowGraphIOException() throws IOException {
        this.settings.setImportControlFlowGraph(new File(" "));
    }

    @Test
    public void testSetIsAddMissingComponents() {
        assertFalse("isAddMissingComponents should return false.", this.settings.isAddMissingComponents());
        this.settings.setAddMissingComponents(true);
        assertTrue("isAddMissingComponents should return true.", this.settings.isAddMissingComponents());

    }

    @Test
    public void testSetFormat() {
        assertEquals("Default format is not set correctly.", Format.JSON, this.settings.getFormat());
        this.settings.setFormat(Format.ALL);
        assertEquals("Wrong format returned after change.", Format.ALL, this.settings.getFormat());

    }

    @Test
    public void testSetPlatformDirectory() throws IOException {
        String defaultPlatformDirectory = System.getenv("ANDROID_HOME") + File.separator + "platforms";
        assertEquals("Default platform directory is not set.", defaultPlatformDirectory,
                this.settings.getPlatformDirectory().getAbsolutePath()
                    );

        this.settings.setPlatformDirectory(new File(GraphSettingsTest.workingDirectory));
        assertEquals("Wrong platform directory returned after change.", GraphSettingsTest.workingDirectory,
                this.settings.getPlatformDirectory().getAbsolutePath()
                    );
    }

    @Test
    public void testValidate() throws IOException {
        this.settings.setImportControlFlowGraph(
                new File(GraphSettingsTest.workingDirectory + File.separator + "app_control_flow_graph.json"));
        this.settings.validate();
    }
}
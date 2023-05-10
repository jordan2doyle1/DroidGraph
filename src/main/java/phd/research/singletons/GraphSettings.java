package phd.research.singletons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.enums.Format;

import java.io.File;
import java.io.IOException;

/**
 * @author Jordan Doyle
 */

public class GraphSettings {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphSettings.class);

    private static final String FLOWDROID_CALLBACKS_FILE_NAME = "flow_droid_callbacks";

    private static GraphSettings instance = null;

    private Format format;

    private File androidPlatformDirectory;
    private File outputDirectory;
    private File apkFile;
    private File callGraphFile;
    private File controlFlowGraphFile;
    private File flowDroidCallbacksFile;

    private boolean importControlFlowGraph;
    private boolean defaultCallbacksFile;
    private boolean addMissingComponents;
    private boolean outputMissingComponents;
    private boolean loggerActive;

    private GraphSettings() {
        this.format = Format.JSON;
        this.androidPlatformDirectory = new File(System.getenv("ANDROID_HOME") + File.separator + "platforms");
        this.outputDirectory = new File(System.getProperty("user.dir") + File.separator + "output");
        this.importControlFlowGraph = false;
        this.defaultCallbacksFile = true;
        this.flowDroidCallbacksFile =
                new File(this.outputDirectory + File.separator + GraphSettings.FLOWDROID_CALLBACKS_FILE_NAME);
        this.addMissingComponents = false;
        this.outputMissingComponents = false;
        this.loggerActive = true;
    }

    public static GraphSettings v() {
        if (instance == null) {
            instance = new GraphSettings();
        }
        return instance;
    }

    public static void resetDefaults() {
        instance = null;
        LOGGER.info("GraphSettings have been reset to default.");
    }

    public void validate() throws IOException {
        this.loggerActive = false;
        setPlatformDirectory(this.androidPlatformDirectory);
        setOutputDirectory(this.outputDirectory);
        setApkFile(this.apkFile);
        setCallGraphFile(this.callGraphFile);

        if (this.isImportControlFlowGraph()) {
            setImportControlFlowGraph(this.controlFlowGraphFile);
        }
        this.loggerActive = true;
    }

    public File getApkFile() {
        return this.apkFile;
    }

    public void setApkFile(File apkFile) throws IOException {
        if (apkFile == null || !apkFile.isFile()) {
            throw new IOException("Apk file does not exist or is not a file (" + apkFile + ").");
        }

        this.apkFile = apkFile;

        if (this.loggerActive) {
            LOGGER.info("Apk file set as '" + apkFile.getAbsolutePath() + "'.");
        }
    }

    public File getPlatformDirectory() {
        return this.androidPlatformDirectory;
    }

    public void setPlatformDirectory(File androidPlatformDirectory) throws IOException {
        if (!androidPlatformDirectory.isDirectory()) {
            throw new IOException("Platform does not exist or is not a directory (" + androidPlatformDirectory + ").");
        }

        this.androidPlatformDirectory = androidPlatformDirectory;

        if (this.loggerActive) {
            LOGGER.info("Android platform directory set as '" + androidPlatformDirectory.getAbsolutePath() + "'");
        }
    }

    public File getOutputDirectory() {
        return this.outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) throws IOException {
        if (!outputDirectory.isDirectory()) {
            throw new IOException("Output directory does not exist or is not a directory (" + outputDirectory + ").");
        }

        this.outputDirectory = outputDirectory;

        if (this.defaultCallbacksFile) {
            this.flowDroidCallbacksFile =
                    new File(this.outputDirectory + File.separator + GraphSettings.FLOWDROID_CALLBACKS_FILE_NAME);
        }

        if (this.loggerActive) {
            LOGGER.info("Output directory set as '" + outputDirectory.getAbsolutePath() + ".");
        }
    }

    public Format getFormat() {
        return this.format;
    }

    public void setFormat(Format format) {
        this.format = format;
        LOGGER.info("Format set as " + format.name());
    }

    public File getCallGraphFile() {
        return this.callGraphFile;
    }

    public void setCallGraphFile(File callGraphFile) throws IOException {
        if (callGraphFile == null || !callGraphFile.isFile()) {
            throw new IOException("Call graph file does not exist or is not a file (" + callGraphFile + ").");
        }

        this.callGraphFile = callGraphFile;

        if (this.loggerActive) {
            LOGGER.info("Call graph file set as '" + callGraphFile.getAbsolutePath() + "'.");
        }
    }

    public boolean isImportControlFlowGraph() {
        return this.importControlFlowGraph;
    }

    public void setImportControlFlowGraph(File controlFlowGraphFile) throws IOException {
        if (controlFlowGraphFile == null || !controlFlowGraphFile.isFile()) {
            throw new IOException(
                    "Control flow graph file does not exist or is not a file (" + controlFlowGraphFile + ").");
        }
        this.importControlFlowGraph = true;
        this.controlFlowGraphFile = controlFlowGraphFile;

        if (this.loggerActive) {
            LOGGER.info("Import control flow graph file set as '" + controlFlowGraphFile.getAbsolutePath() + "'.");
        }
    }

    public File getContolFlowGraphFile() {
        return this.controlFlowGraphFile;
    }

    public boolean isAddMissingComponents() {
        return this.addMissingComponents;
    }

    public void setAddMissingComponents(boolean addMissingComponents) {
        this.addMissingComponents = addMissingComponents;
        LOGGER.info("Add missing components set as " + addMissingComponents);
    }

    public File getFlowDroidCallbacksFile() {
        return this.flowDroidCallbacksFile;
    }

    @SuppressWarnings("unused")     // Used in DroidCoverage.
    public void setFlowDroidCallbacksFile(File callbacksFile) {
        this.flowDroidCallbacksFile = callbacksFile;
        this.defaultCallbacksFile = false;
        LOGGER.info("FlowDroid callbacks file set as '" + callbacksFile.getAbsolutePath() + "'.");
    }

    public boolean isOutputMissingComponents() {
        return this.outputMissingComponents;
    }

    public void setOutputMissingComponents(boolean outputMissingComponents) {
        this.outputMissingComponents = outputMissingComponents;
        LOGGER.info("Output missing components set as " + outputMissingComponents);
    }
}

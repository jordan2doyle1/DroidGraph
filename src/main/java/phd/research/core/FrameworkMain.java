package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.graph.GraphWriter;
import phd.research.jGraph.Vertex;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class FrameworkMain {

    private static final Logger logger = LoggerFactory.getLogger(FrameworkMain.class);

    private static String propertiesPath = System.getProperty("user.dir") + "/config/default.properties";

    private static boolean outputGraph = false;
    private static String outputFormat;
    private static String graphType;
    private static boolean consoleOutput = false;

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        logger.info("Start time: " + startTime);

        if (args.length != 0) {
            for (int i = 0; i < args.length; i++) {
                if ("-properties_path".equals(args[i])) {
                    if ((args.length - i) < 2) {
                        logger.error("Too few arguments: Must give properties file directory with -properties option.");
                        return;
                    }
                    propertiesPath = args[i + 1];
                    i++;
                } else if ("-o".equals(args[i])) {
                    if ((args.length - i) < 3) {
                        logger.error("Too few arguments: Must give output format and graph type with -o option.");
                        return;
                    }
                    outputGraph = true;
                    outputFormat = args[i + 1];
                    graphType = args[i + 2];
                    i += 2;
                } else if ("-c".equals(args[i])) {
                    consoleOutput = true;
                } else {
                    logger.error("Argument " + args[i] + " is not recognised.");
                    return;
                }
            }
        }

        if (apkExists()) {
            executeSoot();

            if (outputGraph) {
                GraphWriter.cleanDirectory(GraphWriter.getOutputLocation());
                String[] formats = outputFormat.split(",");

                if (isRecognisedFormat(formats)) {
                    for (String format : formats) {
                        for (String type : graphType.split(",")) {
                            switch (type) {
                                case "UG":
                                    MethodManager.getInstance().outputFilteredMethods(format);
                                    break;
                                case "CG":
                                    GraphManager.getInstance().getCallGraph().outputGraph(format);
                                    break;
                                case "CFG":
                                    GraphManager.getInstance().getControlFlowGraph().outputGraph(format);
                                default:
                                    logger.error("Unrecognised graph type: " + type);
                            }
                        }
                    }
                }
            }

            if (consoleOutput) printAnalysisDetails();
        } else {
            logger.error("No APK file found!");
        }

        long endTime = System.nanoTime();
        logger.info("End time: " + endTime);
        long execTime = (((endTime - startTime) / 1000) / 1000) / 1000; // Nano to Micro to Milli to Seconds.
        logger.info("Execution time: " + Math.round(execTime / 60.0) + " minute(s) " + "(" + execTime + " second(s))");
    }

    public static Properties getFrameworkProperties() {
        Properties properties = new Properties();

        try {
            properties.load(new FileInputStream(propertiesPath));
        } catch (IOException e) {
            logger.error("Error loading properties file: " + e.getMessage());
            return null;
        }

        return properties;
    }

    public static boolean apkExists() {
        Properties properties = getFrameworkProperties();

        if (properties != null) {
            File apk;
            boolean useSource = Boolean.parseBoolean(properties.getProperty("USE_SOURCE"));

            if (useSource) {
                apk = new File(String.format("%s%s%s%s", properties.getProperty("SOURCE_LOCATION"),
                        properties.getProperty("PROJECT_NAME"), properties.getProperty("SOURCE_APK_LOCATION"),
                        properties.getProperty("SOURCE_APK_NAME")));
                System.out.println(apk);
            } else
                apk = new File(String.format("%s%s", properties.getProperty("APK_LOCATION"),
                        properties.getProperty("APK_NAME")));

            return apk.exists();
        }

        return false;
    }

    public static String getAPK() {
        Properties properties = FrameworkMain.getFrameworkProperties();
        String apk = null;

        if (properties != null) {
            boolean useSource = Boolean.parseBoolean(properties.getProperty("USE_SOURCE"));
            if (useSource) {
                apk = properties.getProperty("SOURCE_LOCATION") + properties.getProperty("PROJECT_NAME") +
                        properties.getProperty("SOURCE_APK_LOCATION") +
                        properties.getProperty("SOURCE_APK_NAME");
            } else {
                apk = properties.getProperty("APK_LOCATION") + properties.getProperty("APK_NAME");
            }
        }

        return apk;
    }

    public static void printList(Set<?> list) {
        int counter = 0;
        int numberOfPrints = 10;
        for (Object item : list) {
            if (counter < numberOfPrints) {
                if (item instanceof String)
                    System.out.println("\t" + item);
                else if (item instanceof SootClass)
                    System.out.println("\t" + ((SootClass) item).getName());
                else if (item instanceof Vertex)
                    System.out.println("\t" + ((Vertex) item).getLabel());
                else if (item instanceof SootMethod) {
                    SootMethod method = (SootMethod) item;
                    System.out.println("\t" + method.getDeclaringClass().getName() + ":" + method.getName());
                }
            } else {
                int remaining = list.size() - numberOfPrints;
                System.out.println("+ " + remaining + " more!");
                break;
            }
            counter++;
        }
        System.out.println();
    }

    private static boolean isRecognisedFormat(String[] formats) {
        for (String format : formats) {
            if (!(format.equals("DOT") || format.equals("JSON"))) {
                logger.error("Unrecognised output format: " + format);
                return false;
            }
        }
        return true;
    }

    public static ARSCFileParser retrieveResources() {
        ARSCFileParser resources = new ARSCFileParser();
        try {
            resources.parse(getAPK());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resources;
    }

    public static LayoutFileParser retrieveLayoutFileParser() {
        LayoutFileParser lfp = null;
        try {
            ProcessManifest manifest = new ProcessManifest(getAPK());
            lfp = new LayoutFileParser(manifest.getPackageName(), retrieveResources());
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        assert lfp != null;
        lfp.parseLayoutFileDirect(getAPK());
        return lfp;
    }

    private static void executeSoot() {
        Properties properties = getFrameworkProperties();

        if (properties == null) {
            return;
        }

        InfoflowAndroidConfiguration configuration = new InfoflowAndroidConfiguration();
        configuration.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstance);
        configuration.setMergeDexFiles(true);
        configuration.getAnalysisFileConfig().setAndroidPlatformDir(properties.getProperty("PLATFORM_LOCATION"));
        configuration.getAnalysisFileConfig().setSourceSinkFile(System.getProperty("user.dir") + "/SourcesAndSinks.txt");

        boolean useSource = Boolean.parseBoolean(properties.getProperty("USE_SOURCE"));
        if (useSource) {
            configuration.getAnalysisFileConfig().setTargetAPKFile(String.format("%s%s%s%s",
                    properties.getProperty("SOURCE_LOCATION"),
                    properties.getProperty("PROJECT_NAME"),
                    properties.getProperty("SOURCE_APK_LOCATION"),
                    properties.getProperty("SOURCE_APK_NAME"))
            );
        } else {
            configuration.getAnalysisFileConfig().setTargetAPKFile(String.format("%s%s",
                    properties.getProperty("APK_LOCATION"), properties.getProperty("APK_NAME"))
            );
        }

        SetupApplication app = new SetupApplication(configuration);
        app.constructCallgraph();

        retrieveLayoutFileParser();
        PackageManager.getInstance().start();
        ClassManager.getInstance().start();
        MethodManager.getInstance().start();
        InterfaceManager.getInstance().extractUI(app);
        GraphManager.getInstance().start();
    }

    private static void printAnalysisDetails() {
        System.out.println("-------------------------------- Analysis Details ---------------------------------\n");

        GraphManager graphManager = GraphManager.getInstance();
        System.out.println("Application Name: " + graphManager.getAppName());
        System.out.println();

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

        System.out.println("Call Graph Composition Table");
        System.out.println(graphManager.getCallGraph().getGraphCompositionTable());

        System.out.println("Control Flow Graph Composition Table");
        System.out.println(graphManager.getControlFlowGraph().getGraphCompositionTable());

        System.out.println("\n-----------------------------------------------------------------------------------\n");
    }
}
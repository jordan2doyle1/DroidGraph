package phd.research.core;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.graph.ContentViewer;
import phd.research.graph.GraphWriter;
import phd.research.jGraph.Vertex;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;

import java.io.File;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class FrameworkMain {
    private static final Logger logger = LoggerFactory.getLogger(FrameworkMain.class);

    private static String androidPlatform;
    private static String apk;
    private static boolean consoleOutput;
    private static boolean outputUnitGraphs;
    private static boolean outputCallGraph;
    private static boolean outputControlFlowGraph;
    private static String outputDirectory;
    private static String outputFormat;
    private static String packageBlacklist;
    private static String classBlacklist;

    public static void main(String[] args) {
        long startTime = System.nanoTime();
        LocalDateTime startDate = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy-HH:mm:ss");
        logger.info("Start time: " + dateFormatter.format(startDate) + " - " + startTime);

        Options options = new Options();
        options.addOption(Option.builder("ap").longOpt("android-platform").desc("Android SDK platform directory.")
                .required().hasArg().numberOfArgs(1).argName("DIRECTORY").build());
        options.addOption(Option.builder("a").longOpt("apk").desc("APK file to analyse.").required().hasArg()
                .numberOfArgs(1).argName("FILE").build());
        options.addOption(Option.builder("co").longOpt("console-output").desc("Print output to console.").build());
        options.addOption(Option.builder("ug").longOpt("unit-graph").desc("Output Unit Graphs.").build());
        options.addOption(Option.builder("cg").longOpt("call-graph").desc("Output Call Graph.").build());
        options.addOption(Option.builder("cfg").longOpt("control-flow-graph").desc("Output Control Flow Graph.")
                .build());
        options.addOption(Option.builder("od").longOpt("output-directory").desc("Directory for output files.")
                .hasArg().numberOfArgs(1).argName("DIRECTORY").build());
        options.addOption(Option.builder("of").longOpt("output-format").hasArg()
                .desc("Graph output format ('DOT', 'JSON', 'ALL').").numberOfArgs(1).argName("FORMAT").build());
        options.addOption(Option.builder("pb").longOpt("package-blacklist")
                .desc("File containing ignored packages.").hasArg().numberOfArgs(1).argName("FILE").build());
        options.addOption(Option.builder("cb").longOpt("class-blacklist").desc("File containing ignored classes.")
                .hasArg().numberOfArgs(1).argName("FILE").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());
        options.addOption(Option.builder("sp").longOpt("source-project").desc("Name of source project.").hasArg()
                .numberOfArgs(1).argName("NAME").build());

        CommandLine cmd = null;
        try {
            if (checkForHelp(args)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DroidGraph2.0", options);
                System.exit(0);
            }

            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            final PrintWriter writer = new PrintWriter(System.out);
            formatter.printUsage(writer,80,"DroidGraph2.0", options);
            writer.flush();
            System.exit(0);
        }

        if (cmd != null) {
            androidPlatform = cmd.getOptionValue("ap");
            if (!directoryExists(androidPlatform)) {
                logger.error("Error: Android platform directory does not exist (" + androidPlatform + ").");
                System.err.println("Error: Android platform directory does not exist (" + androidPlatform + ").");
                System.exit(10);
            }

            String sourceProject = (cmd.hasOption("sp") ? cmd.getOptionValue("sp") : null);

            apk = cmd.getOptionValue("a");
            if (!fileExists(apk)) {
                if (cmd.hasOption("sp")) {
                    String sourceApk = "/Users/jordandoyle/Android_Projects/" + sourceProject +
                            "/app/build/outputs/apk/debug/app-debug.apk";
                    if (fileExists(sourceApk)) {
                        apk = sourceApk;
                    } else {
                        logger.error("Error: Source project (" + sourceProject + ") does not exist .");
                        System.err.println("Error: Source project (" + sourceProject + ") does not exist .");
                        System.exit(20);
                    }
                } else {
                    logger.error("Error: APK file does not exist (" + apk + ").");
                    System.err.println("Error: APK file does not exist (" + apk + ").");
                    System.exit(30);
                }
            }

            consoleOutput = cmd.hasOption("co");
            outputUnitGraphs = cmd.hasOption("ug");
            outputCallGraph = cmd.hasOption("cg");
            outputControlFlowGraph = cmd.hasOption("cfg");

            outputDirectory = (cmd.hasOption("od") ? cmd.getOptionValue("od") :
                    System.getProperty("user.dir") + "/sootOutput/");
            if (!directoryExists(outputDirectory)) {
                outputDirectory = System.getProperty("user.dir") + "/sootOutput/";
                logger.warn("Warning: Output directory does not exist, using default directory instead.");
            }
            outputFormat = (cmd.hasOption("of") ? cmd.getOptionValue("of") : "JSON");
            if (!isRecognisedFormat(outputFormat)) {
                logger.error("Warning: Unrecognised output format, using default format instead.");
                System.err.println("Warning: Unrecognised output format, using default format instead.");
            }
            packageBlacklist = (cmd.hasOption("pb") ? cmd.getOptionValue("pb") :
                    System.getProperty("user.dir") + "/package_blacklist");
            if (!fileExists(packageBlacklist)) {
                logger.warn("Warning: Package blacklist file does not exist, using default instead.");
                packageBlacklist = System.getProperty("user.dir") + "/package_blacklist";
            }
            classBlacklist = (cmd.hasOption("cb") ? cmd.getOptionValue("cb") :
                    System.getProperty("user.dir") + "/class_blacklist");
            if (!fileExists(classBlacklist)) {
                classBlacklist = System.getProperty("user.dir") + "/class_blacklist";
                logger.warn("Warning: Class blacklist file does not exist, using default instead.");
            }
        }

        InfoflowAndroidConfiguration configuration = new InfoflowAndroidConfiguration();
        configuration.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstance);
        configuration.setMergeDexFiles(true);
        configuration.getAnalysisFileConfig().setAndroidPlatformDir(androidPlatform);
        configuration.getAnalysisFileConfig().setSourceSinkFile(System.getProperty("user.dir") + "/SourcesAndSinks.txt");
        configuration.getAnalysisFileConfig().setTargetAPKFile(apk);

        ApplicationAnalysis appAnalysis = new ApplicationAnalysis(configuration);
        appAnalysis.runAnalysis();

        GraphWriter.cleanDirectory(outputDirectory);

        if (outputUnitGraphs)
            appAnalysis.outputMethods(outputFormat);

        GraphWriter graphWriter = new GraphWriter();
        if (outputCallGraph) {
            String outputName = (cmd != null && cmd.hasOption("sp") ? cmd.getOptionValue("sp") + "-CG" : "App-CG");
            graphWriter.writeGraph(outputFormat, outputName, appAnalysis.getCallGraph());
        }
        if (outputControlFlowGraph) {
            String outputName = (cmd != null && cmd.hasOption("sp") ? cmd.getOptionValue("sp") + "-CFG" : "App-CFG");
            graphWriter.writeGraph(outputFormat, outputName, appAnalysis.getControlFlowGraph());
        }

        if (consoleOutput) {
            ContentViewer contentViewer = new ContentViewer(appAnalysis);
            contentViewer.printAppDetails();
            contentViewer.printCallGraphDetails();
            contentViewer.printCFGDetails();
        }

        long endTime = System.nanoTime();
        logger.info("End time: " + endTime);
        long execTime = (((endTime - startTime) / 1000) / 1000) / 1000; // Nano to Micro to Milli to Seconds.
        logger.info("Execution time: " + Math.round(execTime / 60.0) + " minute(s) " + "(" + execTime + " second(s))");
    }

    public static String getOutputDirectory() {
        return outputDirectory;
    }

    public static String getApk() {
        return apk;
    }

    public static String getPackageBlacklist() {
        return packageBlacklist;
    }

    public static String getClassBlacklist() {
        return classBlacklist;
    }

    private static boolean checkForHelp(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args, true);
        } catch (ParseException e) {
            logger.error("Error Parsing Command Line Arguments: " + e.getMessage());
            System.err.println("Error Parsing Command Line Arguments: " + e.getMessage());
        }

        if (cmd != null) {
            return cmd.hasOption("h");
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean directoryExists(String directoryName) {
        File directory = new File(directoryName);
        return directory.isDirectory();
    }

    private static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    private static boolean isRecognisedFormat(String format) {
        return format.equals("DOT") || format.equals("JSON") || format.equals("ALL");
    }

    @SuppressWarnings("unused")
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
}
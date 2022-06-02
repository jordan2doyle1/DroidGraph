package phd.research.core;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.enums.Format;
import phd.research.graph.Viewer;
import phd.research.graph.Writer;
import phd.research.ui.UiControls;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private static boolean outputContent;
    private static String outputDirectory;
    private static Format outputFormat;
    private static String fmOutputFile;

    public static void main(String[] args) {
        LocalDateTime startDate = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy-HH:mm:ss");
        logger.info("Start time: " + dateFormatter.format(startDate));

        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("apk").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("APK file to analyse.").build());
        options.addOption(Option.builder("fm").longOpt("fm-output").hasArg().numberOfArgs(1).argName("FILE")
                .desc("FrontMatter analysis output file.").build());
        options.addOption(Option.builder("p").longOpt("android-platform").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Android SDK platform directory.").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory for output files.").build());
        options.addOption(Option.builder("f").longOpt("output-format").hasArg().numberOfArgs(1).argName("FORMAT")
                .desc("Graph output format ('DOT','JSON','ALL').").build());
        options.addOption(Option.builder("s").longOpt("source-project").hasArg().numberOfArgs(1).argName("NAME")
                .desc("Name of source project.").build());
        options.addOption(Option.builder("co").longOpt("console-output").desc("Print output to console.").build());
        options.addOption(Option.builder("ug").longOpt("unit-graph").desc("Output Unit Graphs.").build());
        options.addOption(Option.builder("cg").longOpt("call-graph").desc("Output Call Graph.").build());
        options.addOption(
                Option.builder("cfg").longOpt("control-flow-graph").desc("Output Control Flow Graph.").build());
        options.addOption(Option.builder("cf").longOpt("content-files").desc("Output content files.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

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
            formatter.printUsage(writer, 80, "DroidGraph2.0", options);
            writer.flush();
            System.exit(0);
        }

        if (cmd != null) {
            androidPlatform =
                    (cmd.hasOption("p") ? cmd.getOptionValue("p") : System.getenv("ANDROID_HOME") + "/platforms/");
            if (!directoryExists(androidPlatform)) {
                logger.error("Android platform directory does not exist (" + androidPlatform + ").");
                System.exit(10);
            }

            String sourceProject = (cmd.hasOption("s") ? cmd.getOptionValue("s") : null);

            apk = cmd.getOptionValue("a");
            if (!fileExists(apk)) {
                if (cmd.hasOption("s")) {
                    String sourceApk = "/Users/jordandoyle/Android_Projects/" + sourceProject + "/app/build/outputs" +
                            "/apk/debug/app-debug.apk";
                    if (fileExists(sourceApk)) {
                        apk = sourceApk;
                    } else {
                        logger.error("Source project (" + sourceProject + ") does not exist .");
                        System.exit(20);
                    }
                } else {
                    logger.error("APK file does not exist (" + apk + ").");
                    System.exit(30);
                }
            }

            consoleOutput = cmd.hasOption("co");
            outputUnitGraphs = cmd.hasOption("ug");
            outputCallGraph = cmd.hasOption("cg");
            outputControlFlowGraph = cmd.hasOption("cfg");
            outputContent = cmd.hasOption("cf");

            outputDirectory =
                    (cmd.hasOption("o") ? cmd.getOptionValue("o") : System.getProperty("user.dir") + "/output/");
            if (!directoryExists(outputDirectory)) {
                outputDirectory = System.getProperty("user.dir") + "/output/";
                if (createDirectory(outputDirectory)) {
                    if (cmd.hasOption("o")) {
                        logger.warn("Output directory doesn't exist, using default directory instead.");
                    }
                } else {
                    logger.error("Output directory does not exist.");
                }
            }
            String outputFormat = (cmd.hasOption("f") ? cmd.getOptionValue("f") : "JSON");
            if (!isRecognisedFormat(outputFormat)) {
                logger.warn("Unrecognised output format, using default format instead.");
            }

            if (cmd.hasOption("fm")) {
                fmOutputFile = cmd.getOptionValue("a");
                if (!fileExists(fmOutputFile)) {
                    logger.error("FrontMatter output file does not exist (" + fmOutputFile + ").");
                    System.exit(30);
                }
            }
        }

        try {
            Writer.cleanDirectory(outputDirectory);
        } catch (IOException e) {
            logger.error("Problem cleaning output directory: " + e.getMessage());
        }

        logger.info("Running FlowDroid...");
        long fdStartTime = System.currentTimeMillis();

        FlowDroidUtils.runFlowDroid(FrameworkMain.getApk(), FrameworkMain.getAndroidPlatform(),
                FrameworkMain.getOutputDirectory()
                                   );

        long fdEndTime = System.currentTimeMillis();
        logger.info("FlowDroid took " + (fdEndTime - fdStartTime) / 1000 + " second(s).");

        logger.info("Running graph generation...");
        long dgStartTime = System.currentTimeMillis();

        File callbackFile = new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks");
        UiControls uiControls = new UiControls(callbackFile, FrameworkMain.getApk());
        DroidGraph droidGraph = new DroidGraph(callbackFile, uiControls);
        droidGraph.generateGraphs();

        long dgEndTime = System.currentTimeMillis();
        logger.info("Graph generation took " + (dgEndTime - dgStartTime) / 1000 + " second(s).");

        Viewer viewer = null;
        if (outputUnitGraphs || outputCallGraph || outputControlFlowGraph || outputContent) {
            long startTime = System.currentTimeMillis();
            logger.info("Starting file output...");

            if (outputUnitGraphs) {
                try {
                    Writer.outputMethods(FrameworkMain.getOutputDirectory(), outputFormat);
                } catch (Exception e) {
                    logger.error("Problem writing methods to output file: " + e.getMessage());
                }
            }

            if (outputCallGraph) {
                String outputName = (cmd != null && cmd.hasOption("s") ? cmd.getOptionValue("s") + "-CG" : "App-CG");
                try {
                    Writer.writeGraph(outputFormat, outputDirectory, outputName, droidGraph.getCallGraph());
                } catch (Exception e) {
                    logger.error("Problem writing call graph to output file: " + e.getMessage());
                }
            }

            if (outputControlFlowGraph) {
                String outputName =
                        (cmd != null && cmd.hasOption("s") ? cmd.getOptionValue("s") + "-CFG" : "App" + "-CFG");
                try {
                    Writer.writeGraph(outputFormat, outputDirectory, outputName, droidGraph.getControlFlowGraph());
                } catch (Exception e) {
                    logger.error("Problem writing CFG to output file: " + e.getMessage());
                }
            }

            if (outputContent) {
                viewer = new Viewer(callbackFile, uiControls);
                try {
                    viewer.writeContentsToFile(FrameworkMain.getOutputDirectory());
                } catch (IOException e) {
                    logger.error("Problem writing content to output file: " + e.getMessage());
                }
            }

            long endTime = System.currentTimeMillis();
            logger.info("File output took " + (endTime - startTime) / 60 + " second(s).");
        }

        if (consoleOutput) {
            if (viewer == null) {
                viewer = new Viewer(callbackFile, uiControls);
            }

            viewer.printAppDetails(FrameworkMain.getApk());
            viewer.printUnassignedCallbacks();
            viewer.printCallbackTable();
            Viewer.printCallGraphDetails(droidGraph.getCallGraph());
            Viewer.printCFGDetails(droidGraph.getControlFlowGraph());
        }

        LocalDateTime endDate = LocalDateTime.now();
        logger.info("End time: " + dateFormatter.format(endDate));
        Duration duration = Duration.between(startDate, endDate);
        logger.info("Execution time: " + duration.getSeconds() + " second(s).");
    }

    public static String getOutputDirectory() {
        return outputDirectory;
    }

    public static String getFrontMatterOutputFile() {
        return fmOutputFile;
    }

    public static String getApk() {
        return apk;
    }

    public static String getAndroidPlatform() {
        return androidPlatform;
    }

    private static boolean checkForHelp(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args, true);
        } catch (ParseException e) {
            logger.error("Problem Parsing Command Line Arguments: " + e.getMessage());
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

    private static boolean createDirectory(String directoryName) {
        File directory = new File(directoryName);
        return directory.mkdir();
    }

    private static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    private static boolean isRecognisedFormat(String format) {
        switch (format) {
            case "DOT":
                outputFormat = Format.dot;
                return true;
            case "JSON":
                outputFormat = Format.json;
                return true;
            case "ALL":
                outputFormat = Format.all;
                return true;
            default:
                return false;
        }
    }
}
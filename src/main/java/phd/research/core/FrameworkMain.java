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
        System.out.println("Start time: " + dateFormatter.format(startDate));

        Options options = new Options();
        options.addOption(Option.builder("ap").longOpt("android-platform")
                .desc("Android SDK platform directory.").required().hasArg().numberOfArgs(1)
                .argName("DIRECTORY").build());
        options.addOption(Option.builder("a").longOpt("apk")
                .desc("APK file to analyse.").required().hasArg().numberOfArgs(1).argName("FILE").build());
        options.addOption(Option.builder("co").longOpt("console-output")
                .desc("Print output to console.").build());
        options.addOption(Option.builder("ug").longOpt("unit-graph")
                .desc("Output Unit Graphs.").build());
        options.addOption(Option.builder("cg").longOpt("call-graph").desc("Output Call Graph.").build());
        options.addOption(Option.builder("cfg").longOpt("control-flow-graph")
                .desc("Output Control Flow Graph.").build());
        options.addOption(Option.builder("od").longOpt("output-directory")
                .desc("Directory for output files.").hasArg().numberOfArgs(1).argName("DIRECTORY").build());
        options.addOption(Option.builder("of").longOpt("output-format")
                .hasArg().desc("Graph output format ('DOT', 'JSON', 'ALL').").numberOfArgs(1)
                .argName("FORMAT").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());
        options.addOption(Option.builder("sp").longOpt("source-project")
                .desc("Name of source project.").hasArg().numberOfArgs(1).argName("NAME").build());
        options.addOption(Option.builder("cf").longOpt("content-files").desc("Output content files.")
                .build());
        options.addOption(Option.builder("fof").longOpt("fm-output-file")
                .desc("FrontMatter analysis output file.").hasArg().numberOfArgs(1).argName("FILE").build());

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
                    String sourceApk = "/Users/jordandoyle/Android_Projects/" + sourceProject
                            + "/app/build/outputs/apk/debug/app-debug.apk";
                    if (fileExists(sourceApk)) apk = sourceApk;
                    else {
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
            outputContent = cmd.hasOption("cf");

            outputDirectory = (cmd.hasOption("od") ? cmd.getOptionValue("od")
                    : System.getProperty("user.dir") + "/output/");
            if (!directoryExists(outputDirectory)) {
                outputDirectory = System.getProperty("user.dir") + "/output/";
                if (createDirectory(outputDirectory)) {
                    if (cmd.hasOption("od")) {
                        logger.warn("Warning: Output directory doesn't exist, using default directory instead.");
                        System.err.println("Warning: Output directory doesn't exist, using default directory instead.");
                    }
                } else {
                    logger.error("Error: Output directory does not exist.");
                    System.err.println("Error: Output directory does not exist.");
                }
            }
            String outputFormat = (cmd.hasOption("of") ? cmd.getOptionValue("of") : "JSON");
            if (!isRecognisedFormat(outputFormat)) {
                logger.error("Warning: Unrecognised output format, using default format instead.");
                System.err.println("Warning: Unrecognised output format, using default format instead.");
            }

            if (cmd.hasOption("fof")) {
                fmOutputFile = cmd.getOptionValue("a");
                if (!fileExists(fmOutputFile)) {
                    logger.error("Error: FrontMatter output file does not exist (" + fmOutputFile + ").");
                    System.err.println("Error: FrontMatter output file does not exist (" + fmOutputFile + ").");
                    System.exit(30);
                }
            }
        }

        try {
            Writer.cleanDirectory(outputDirectory);
        } catch (IOException e) {
            logger.error("Error cleaning output directory: " + e.getMessage());
        }

        logger.info("Running FlowDroid...");
        System.out.println("Running FlowDroid...");
        long fdStartTime = System.currentTimeMillis();

        FlowDroidUtils.runFlowDroid();

        long fdEndTime = System.currentTimeMillis();
        logger.info("FlowDroid took " + (fdEndTime - fdStartTime) / 1000 + " second(s).");
        System.out.println("FlowDroid took " + (fdEndTime - fdStartTime) / 1000 + " second(s).");

        logger.info("Running graph generation...");
        System.out.println("Running graph generation...");
        long dgStartTime = System.currentTimeMillis();

        UiControls uiControls = new UiControls(
                new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"),
                FrameworkMain.getApk()
        );
        DroidGraph droidGraph = new DroidGraph(
                new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), uiControls);
        droidGraph.generateGraphs();

        long dgEndTime = System.currentTimeMillis();
        logger.info("Graph generation took " + (dgEndTime - dgStartTime) / 1000 + " second(s).");
        System.out.println("Graph generation took " + (dgEndTime - dgStartTime) / 1000 + " second(s).");

        Viewer viewer = null;
        if (outputUnitGraphs || outputCallGraph || outputControlFlowGraph || outputContent) {
            long startTime = System.currentTimeMillis();
            logger.info("Starting file output...");
            System.out.println("Starting file output...");

            if (outputUnitGraphs) try {
                Writer.outputMethods(FrameworkMain.getOutputDirectory(), outputFormat);
            } catch (Exception e) {
                logger.error("Error writing methods to output file: " + e.getMessage());
            }

            if (outputCallGraph) {
                String outputName = (cmd != null && cmd.hasOption("sp") ? cmd.getOptionValue("sp") + "-CG" : "App-CG");
                try {
                    Writer.writeGraph(outputFormat, outputDirectory, outputName, droidGraph.getCallGraph());
                } catch (Exception e) {
                    logger.error("Error writing call graph to output file: " + e.getMessage());
                }
            }

            if (outputControlFlowGraph) {
                String outputName = (cmd != null && cmd.hasOption("sp") ? cmd.getOptionValue("sp") + "-CFG" : "App-CFG");
                try {
                    Writer.writeGraph(outputFormat, outputDirectory, outputName, droidGraph.getControlFlowGraph());
                } catch (Exception e) {
                    logger.error("Error writing CFG to output file: " + e.getMessage());
                }
            }

            if (outputContent) {
                viewer = new Viewer(
                        new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), uiControls
                );
                try {
                    viewer.writeContentsToFile(FrameworkMain.getOutputDirectory());
                } catch (IOException e) {
                    logger.error("Error writing content to output file: " + e.getMessage());
                }
            }

            long endTime = System.currentTimeMillis();
            logger.info("File output took " + (endTime - startTime) / 60 + " second(s).");
            System.out.println("File output took " + (endTime - startTime) / 60 + " second(s).");
        }

        if (consoleOutput) {
            if (viewer == null)
                viewer = new Viewer(
                        new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), uiControls
                );

            viewer.printAppDetails(FrameworkMain.getApk());
            viewer.printUnassignedCallbacks();
            viewer.printCallbackTable();
            Viewer.printCallGraphDetails(droidGraph.getCallGraph());
            Viewer.printCFGDetails(droidGraph.getControlFlowGraph());
        }

        LocalDateTime endDate = LocalDateTime.now();
        logger.info("End time: " + dateFormatter.format(endDate));
        System.out.println("End time: " + dateFormatter.format(endDate));
        Duration duration = Duration.between(startDate, endDate);
        logger.info("Execution time: " + duration.getSeconds() + " second(s).");
        System.out.println("Execution time: " + duration.getSeconds() + " second(s).");
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
            logger.error("Error Parsing Command Line Arguments: " + e.getMessage());
            System.err.println("Error Parsing Command Line Arguments: " + e.getMessage());
        }

        if (cmd != null) return cmd.hasOption("h");

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
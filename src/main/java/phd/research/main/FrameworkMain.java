package phd.research.main;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.Timer;
import phd.research.core.DroidControls;
import phd.research.core.DroidGraph;
import phd.research.core.FlowDroidUtils;
import phd.research.enums.Format;
import phd.research.graph.Viewer;
import phd.research.graph.Writer;
import phd.research.helper.PythonRunner;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Jordan Doyle
 */

public class FrameworkMain {
    private static final Logger logger = LoggerFactory.getLogger(FrameworkMain.class);

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("apk-file").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("APK file to analyse.").build());
        options.addOption(Option.builder("p").longOpt("android-platform").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Android SDK platform directory.").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory for output files.").build());
        options.addOption(Option.builder("f").longOpt("output-format").hasArg().numberOfArgs(1).argName("FORMAT")
                .desc("Graph output format ('DOT','JSON', GML, 'ALL').").build());

        options.addOption(Option.builder("g").longOpt("generate-cfg").desc("Generate CFG.").build());
        options.addOption(Option.builder("ug").longOpt("unit-graph").desc("Output Unit Graphs.").build());
        options.addOption(Option.builder("cg").longOpt("call-graph").desc("Output Call Graph.").build());
        options.addOption(Option.builder("cf").longOpt("control-flow-graph").desc("Output CFG.").build());

        options.addOption(Option.builder("i").longOpt("import-cg").hasArg().numberOfArgs(1).argName("FILE")
                .desc("Import AndroGuard call graph from given file.").build());
        options.addOption(Option.builder("v").longOpt("venv").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory containing Python virtual environment.").build());

        options.addOption(Option.builder("c").longOpt("clean-directory").desc("Clean output directory.").build());
        options.addOption(Option.builder("s").longOpt("output-analysis").desc("Output soot content files.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

        CommandLine cmd = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            final PrintWriter writer = new PrintWriter(System.out);
            formatter.printUsage(writer, 80, "DroidGraph2.0", options);
            writer.flush();
            System.exit(0);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("DroidGraph2.0", options);
            System.exit(0);
        }

        Timer timer = new Timer();
        logger.info("Start time: " + timer.start());

        File apk = new File(cmd.getOptionValue("a"));
        if (!apk.exists()) {
            logger.error("APK file does not exist (" + apk + ").");
            System.exit(10);
        }

        File androidPlatform = new File((cmd.hasOption("p") ? cmd.getOptionValue("p") :
                System.getenv("ANDROID_HOME") + File.separator + "platforms"));
        if (!androidPlatform.isDirectory()) {
            logger.error("Android platform directory does not exist (" + androidPlatform + ").");
            System.exit(20);
        }

        File defaultOutput = new File(System.getProperty("user.dir") + File.separator + "output");
        File outputDirectory = (cmd.hasOption("o") ? new File(cmd.getOptionValue("o")) : defaultOutput);
        if (!outputDirectory.isDirectory()) {
            outputDirectory = defaultOutput;
            if (outputDirectory.mkdir()) {
                if (cmd.hasOption("o")) {
                    logger.warn("Output directory doesn't exist, using default directory instead.");
                }
            } else {
                logger.error("Output directory does not exist.");
                System.exit(30);
            }
        }

        if (cmd.hasOption("c")) {
            try {
                FileUtils.cleanDirectory(outputDirectory);
            } catch (IOException e) {
                logger.error("Problem cleaning output directory: " + e.getMessage());
            }
        }

        Format outputFormat;
        try {
            outputFormat = (cmd.hasOption("f") ? FlowDroidUtils.stringToFormat(cmd.getOptionValue("f")) : Format.json);
        } catch (RuntimeException e) {
            logger.warn(e.getMessage() + " Using default format instead.");
            outputFormat = Format.json;
        }

        Timer cTimer = new Timer();
        logger.info("Running FlowDroid... (" + cTimer.start(true) + ")");
        FlowDroidUtils.runFlowDroid(apk, androidPlatform, outputDirectory);
        logger.info("(" + cTimer.end() + ") FlowDroid took " + cTimer.secondsDuration() + " second(s).");

        File callbackFile = new File(System.getProperty("user.dir") + File.separator + "FlowDroidCallbacks");
        if (!callbackFile.exists()) {
            logger.error("FlowDroid callbacks file does not exist.");
            System.exit(50);
        }

        logger.info("Processing UI Controls... (" + cTimer.start(true) + ")");
        DroidControls droidControls = null;
        try {
            droidControls = new DroidControls(callbackFile, apk);
        } catch (XmlPullParserException | IOException e) {
            logger.error("Failure while parsing app interface: " + e.getMessage());
        }
        logger.info("(" + cTimer.end() + ") UI control processing took " + cTimer.secondsDuration() + " second(s).");

        boolean outputAnalysis = cmd.hasOption("s");
        if (outputAnalysis) {
            logger.info("Starting file output... (" + cTimer.start(true) + ")");
            Viewer viewer = new Viewer(droidControls);
            try {
                viewer.writeAnalysisToFile(outputDirectory, apk);
            } catch (IOException | XmlPullParserException e) {
                logger.error("Failed to write app analysis to output file: " + e.getMessage());
            }
            logger.info("(" + cTimer.end() + ") File output took " + cTimer.secondsDuration() + " second(s).");
        }

        if (cmd.hasOption("g")) {
            logger.info("Running graph generation... (" + cTimer.start(true) + ")");
            DroidGraph droidGraph = null;
            try {
                if (cmd.hasOption("i")) {
                    File androGuardCallGraph = new File(cmd.getOptionValue("i"));
                    if (!androGuardCallGraph.exists()) {
                        throw new IOException("AndroGuard graph file does not exist (" + androGuardCallGraph + ").");
                    }
                    droidGraph = new DroidGraph(droidControls, androGuardCallGraph);
                    droidGraph.generateGraphs(androGuardCallGraph, true);
                } else {
                    PythonRunner pythonRunner = new PythonRunner();
                    if (cmd.hasOption("v")) {
                        pythonRunner.setVirtualEnvDirectory(new File(cmd.getOptionValue("v")));
                    }
                    List<String> output = pythonRunner.runAndroGuard(apk, outputDirectory, cmd.hasOption("v"));
                    droidGraph = new DroidGraph(droidControls, new File(outputDirectory + "/AndroGuardCG.gml"));
                    droidGraph.generateGraphs(new File(outputDirectory + "/AndroGuardCG.gml"), true);
                }
            } catch (IOException | InterruptedException e) {
                logger.error("Failure while generating Call Graph and Control Flow Graph: " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(60);
            }
            logger.info("(" + cTimer.end() + ") Graph generation took " + cTimer.secondsDuration() + " second(s).");

            boolean outputUnitGraphs = cmd.hasOption("ug");
            boolean outputCallGraph = cmd.hasOption("cg");
            boolean outputControlFlowGraph = cmd.hasOption("cf");

            if (outputUnitGraphs || outputCallGraph || outputControlFlowGraph) {
                logger.info("Starting graph output... (" + cTimer.start(true) + ")");

                if (outputUnitGraphs) {
                    try {
                        Writer.outputMethods(outputDirectory, outputFormat);
                    } catch (Exception e) {
                        logger.error("Problem writing methods to output file: " + e.getMessage());
                    }
                }

                if (outputCallGraph) {
                    try {
                        Writer.writeGraph(outputDirectory, "APP-CG", outputFormat, droidGraph.getCallGraph());
                    } catch (Exception e) {
                        logger.error("Problem writing call graph to output file: " + e.getMessage());
                    }
                }

                if (outputControlFlowGraph) {
                    try {
                        Writer.writeGraph(outputDirectory, "APP-CFG", outputFormat, droidGraph.getControlFlowGraph());
                    } catch (Exception e) {
                        logger.error("Problem writing CFG to output file: " + e.getMessage());
                    }
                }

                logger.info("(" + cTimer.end() + ") Graph output took " + cTimer.secondsDuration() + " second(s).");
            }

            if (outputAnalysis) {
                try {
                    Viewer.outputCGDetails(outputDirectory, droidGraph.getCallGraph());
                    Viewer.outputCFGDetails(outputDirectory, droidGraph.getControlFlowGraph());
                } catch (IOException e) {
                    logger.error("Failed to write graph composition details to output file: " + e.getMessage());
                }
            }
        }

        logger.info("End time: " + timer.end());
        logger.info("Execution time: " + timer.secondsDuration() + " second(s).");
    }
}
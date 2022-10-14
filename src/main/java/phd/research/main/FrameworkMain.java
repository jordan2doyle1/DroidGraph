package phd.research.main;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.Timer;
import phd.research.core.DroidControls;
import phd.research.core.DroidGraph;
import phd.research.core.FlowDroidAnalysis;
import phd.research.enums.Format;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkMain.class);

    @SuppressWarnings("CommentedOutCode")
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
        LOGGER.info("Start time: " + timer.start());

        File apk = new File(cmd.getOptionValue("a"));
        if (!apk.exists()) {
            LOGGER.error("APK file does not exist (" + apk + ").");
            System.exit(10);
        }

        File androidPlatform = new File((cmd.hasOption("p") ? cmd.getOptionValue("p") :
                System.getenv("ANDROID_HOME") + File.separator + "platforms"));
        if (!androidPlatform.isDirectory()) {
            LOGGER.error("Android platform directory does not exist (" + androidPlatform + ").");
            System.exit(20);
        }

        File defaultOutput = new File(System.getProperty("user.dir") + File.separator + "output");
        File outputDirectory = (cmd.hasOption("o") ? new File(cmd.getOptionValue("o")) : defaultOutput);
        if (!outputDirectory.isDirectory()) {
            outputDirectory = defaultOutput;
            if (outputDirectory.mkdir()) {
                if (cmd.hasOption("o")) {
                    LOGGER.warn("Output directory doesn't exist, using default directory instead.");
                }
            } else {
                LOGGER.error("Output directory does not exist.");
                System.exit(30);
            }
        }

        if (cmd.hasOption("c")) {
            try {
                FileUtils.cleanDirectory(outputDirectory);
            } catch (IOException e) {
                LOGGER.error("Problem cleaning output directory: " + e.getMessage());
            }
        }

        Format outputFormat;
        try {
            outputFormat = (cmd.hasOption("f") ? Format.valueOf(cmd.getOptionValue("f")) : Format.JSON);
        } catch (RuntimeException e) {
            LOGGER.warn(e.getMessage() + " Using default format instead.");
            outputFormat = Format.JSON;
        }

        Timer cTimer = new Timer();
        LOGGER.info("Running FlowDroid... (" + cTimer.start(true) + ")");
        FlowDroidAnalysis flowDroidAnalysis;
        try {
            flowDroidAnalysis = new FlowDroidAnalysis(apk, androidPlatform, outputDirectory);
            flowDroidAnalysis.runFlowDroid();
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Error occurred while running FlowDroid: " + e.getMessage());
        }
        LOGGER.info("(" + cTimer.end() + ") FlowDroid took " + cTimer.secondsDuration() + " second(s).");

        File callbackFile = new File(System.getProperty("user.dir") + File.separator + "FlowDroidCallbacks");
        if (!callbackFile.exists()) {
            LOGGER.error("FlowDroid callbacks file does not exist.");
            System.exit(50);
        }

        LOGGER.info("Processing UI Controls... (" + cTimer.start(true) + ")");
        DroidControls droidControls = null;
        try {
            droidControls = new DroidControls(flowDroidAnalysis);
        } catch (XmlPullParserException | IOException e) {
            LOGGER.error("Failure while parsing app interface: " + e.getMessage());
        }
        LOGGER.info("(" + cTimer.end() + ") UI control processing took " + cTimer.secondsDuration() + " second(s).");

        boolean outputAnalysis = cmd.hasOption("s");
        if (outputAnalysis) {
            LOGGER.info("Starting file output... (" + cTimer.start(true) + ")");
            try {
                if (droidControls != null) {
                    droidControls.writeControlsToFile(outputDirectory);
                }
                flowDroidAnalysis.writeAnalysisToFile(outputDirectory);
            } catch (IOException e) {
                LOGGER.error("Failed to write app analysis to output file: " + e.getMessage());
            }
            LOGGER.info("(" + cTimer.end() + ") File output took " + cTimer.secondsDuration() + " second(s).");
        }

        if (cmd.hasOption("g")) {
            LOGGER.info("Running graph generation... (" + cTimer.start(true) + ")");
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
                LOGGER.error("Failure while generating Call Graph and Control Flow Graph: " + e.getMessage());
                e.printStackTrace(System.err);
                System.exit(60);
            }
            LOGGER.info("(" + cTimer.end() + ") Graph generation took " + cTimer.secondsDuration() + " second(s).");

            boolean outputUnitGraphs = cmd.hasOption("ug");
            boolean outputCallGraph = cmd.hasOption("cg");
            boolean outputControlFlowGraph = cmd.hasOption("cf");

            if (outputUnitGraphs || outputCallGraph || outputControlFlowGraph) {
                LOGGER.info("Starting graph output... (" + cTimer.start(true) + ")");

                if (outputUnitGraphs) {
                    try {
                        Writer.outputMethods(outputDirectory, outputFormat);
                    } catch (Exception e) {
                        LOGGER.error("Problem writing methods to output file: " + e.getMessage());
                    }
                }

                if (outputCallGraph) {
                    try {
                        Writer.writeGraph(outputDirectory, "APP-CG", outputFormat, droidGraph.getCallGraph());
                    } catch (Exception e) {
                        LOGGER.error("Problem writing call graph to output file: " + e.getMessage());
                    }
                }

                if (outputControlFlowGraph) {
                    try {
                        Writer.writeGraph(outputDirectory, "APP-CFG", outputFormat, droidGraph.getControlFlowGraph());
                    } catch (Exception e) {
                        LOGGER.error("Problem writing CFG to output file: " + e.getMessage());
                    }
                }

                LOGGER.info("(" + cTimer.end() + ") Graph output took " + cTimer.secondsDuration() + " second(s).");
            }

            if (outputAnalysis) {
                try {
                    DroidGraph.outputCGDetails(outputDirectory, droidGraph.getCallGraph());
                    DroidGraph.outputCFGDetails(outputDirectory, droidGraph.getControlFlowGraph());
                } catch (IOException e) {
                    LOGGER.error("Failed to write graph composition details to output file: " + e.getMessage());
                }
            }

            //            LOGGER.info("Running graph export... (" + cTimer.start(true) + ")");
            //            try {
            //                Writer.writeGraph(outputDirectory, "exportTest", Format.ALL, droidGraph
            //                .getControlFlowGraph());
            //            } catch (IOException e) {
            //                throw new RuntimeException("Failed to export graph: " + e.getMessage() + e);
            //            }
            //            LOGGER.info("(" + cTimer.end() + ") Graph export took " + cTimer.secondsDuration() + "
            //            second(s).");
            //
            //            LOGGER.info("Running graph import... (" + cTimer.start(true) + ")");
            //            Graph<Vertex, DefaultEdge> jsonImport =
            //                    Importer.importDroidGraph(Format.JSON, new File(outputDirectory + "/JSON/exportTest
            //                    .json"));
            //
            //            Graph<Vertex, DefaultEdge> dotImport = Importer.importDroidGraph(Format.DOT, new File
            //            (outputDirectory +
            //                    "/DOT/exportTest.dot"));
            //            Graph<Vertex, DefaultEdge> gmlImport = Importer.importDroidGraph(Format.GML, new File
            //            (outputDirectory +
            //                    "/GML/exportTest.gml"));
            //
            //            if (dotImport.vertexSet().size() == jsonImport.vertexSet().size() && dotImport.vertexSet()
            //            .size() ==
            //                    gmlImport.vertexSet().size()) {
            //                System.out.println("All vertex imports match.");
            //            } else {
            //                System.out.println("Vertex imports DO NOT match.");
            //            }
            //
            //            if (dotImport.edgeSet().size() == jsonImport.edgeSet().size() && dotImport.edgeSet().size() ==
            //                    gmlImport.edgeSet().size()) {
            //                System.out.println("All edge imports match.");
            //            } else {
            //                System.out.println("Edge imports DO NOT match.");
            //            }
            //
            //            if (jsonImport.vertexSet().size() == droidGraph.getControlFlowGraph().vertexSet().size() &&
            //                    jsonImport.edgeSet().size() == droidGraph.getControlFlowGraph().edgeSet().size()) {
            //                System.out.println("Import matches export.");
            //            } else {
            //                System.out.println("JSON Vertex Set: " + jsonImport.vertexSet().size());
            //                System.out.println("Droid Vertex Set: " + droidGraph.getControlFlowGraph().vertexSet()
            //                .size());
            //                System.out.println("JSON Edge Set: " + jsonImport.edgeSet().size());
            //                System.out.println("Droid Edge Set: " + droidGraph.getControlFlowGraph().edgeSet().size
            //                ());
            //            }
            //            LOGGER.info("(" + cTimer.end() + ") Graph import took " + cTimer.secondsDuration() + " second(s).");
        }

        LOGGER.info("End time: " + timer.end());
        LOGGER.info("Execution time: " + timer.secondsDuration() + " second(s).");
    }
}
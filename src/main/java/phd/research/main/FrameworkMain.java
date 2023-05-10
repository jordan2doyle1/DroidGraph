package phd.research.main;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Timer;
import phd.research.core.DroidGraph;
import phd.research.enums.Format;
import phd.research.singletons.GraphSettings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Jordan Doyle
 */

public class FrameworkMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkMain.class);

    public static void main(String[] args) {

        Timer timer = new Timer();
        LOGGER.info("Start time: " + timer.start());

        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("apk-file").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("The APK file to analyse.").build());
        options.addOption(Option.builder("i").longOpt("import-CG").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("Import AndroGuard call graph from the given file.").build());
        options.addOption(Option.builder("l").longOpt("load-CFG").hasArg().numberOfArgs(1).argName("FILE")
                .desc("Load the control flow graph from the given file.").build());
        options.addOption(Option.builder("p").longOpt("android-platform").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The Android SDK platform directory.").build());
        options.addOption(Option.builder("f").longOpt("output-format").hasArg().numberOfArgs(1).argName("FORMAT")
                .desc("The graph output format ('DOT','JSON', GML, 'ALL').").build());
        options.addOption(Option.builder("v").longOpt("venv").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The directory containing Python virtual environment.").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The directory for storing output files.").build());

        options.addOption(Option.builder("ug").longOpt("output-UG").desc("Output all method Unit graphs.").build());
        options.addOption(Option.builder("cg").longOpt("output-CG").desc("Output the call graph.").build());
        options.addOption(Option.builder("cf").longOpt("output-CFG").desc("Output control flow graph.").build());

        options.addOption(Option.builder("m").longOpt("missing-components").desc("Output missing components.").build());
        options.addOption(Option.builder("s").longOpt("output-analysis").desc("Output soot content files.").build());
        options.addOption(Option.builder("c").longOpt("clean-directory").desc("Clean the output directory.").build());
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
            System.exit(10);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("DroidGraph2.0", options);
            System.exit(0);
        }

        GraphSettings settings = GraphSettings.v();
        try {
            settings.setApkFile(new File(cmd.getOptionValue("a")));
            settings.setCallGraphFile(new File(cmd.getOptionValue("i")));
        } catch (IOException e) {
            LOGGER.error("Files missing: " + e.getMessage());
            System.exit(20);
        }

        if (cmd.hasOption("p")) {
            try {
                settings.setPlatformDirectory(new File(cmd.getOptionValue("p")));
            } catch (IOException e) {
                LOGGER.error("Files missing: " + e.getMessage());
                System.exit(30);
            }
        }

        if (cmd.hasOption("f")) {
            settings.setFormat(Format.valueOf(cmd.getOptionValue("f")));
        }

        if (cmd.hasOption("m")) {
            settings.setOutputMissingComponents(true);
        }

        if (cmd.hasOption("o")) {
            try {
                settings.setOutputDirectory(new File(cmd.getOptionValue("o")));
            } catch (IOException e) {
                LOGGER.error("Files missing: " + e.getMessage());
                System.exit(40);
            }
        }

        try {
            settings.validate();
        } catch (IOException e) {
            LOGGER.error("Files missing: " + e.getMessage());
            System.exit(50);
        }

        if (cmd.hasOption("c")) {
            try {
                FileUtils.cleanDirectory(settings.getOutputDirectory());
            } catch (IOException e) {
                LOGGER.error("Failed to clean output directory." + e.getMessage());
            }
        }

        DroidGraph droidGraph = new DroidGraph();

        boolean outputUnitGraphs = cmd.hasOption("ug");
        boolean outputCallGraph = cmd.hasOption("cg");
        boolean outputControlFlowGraph = cmd.hasOption("cf");

        Timer cTimer = new Timer();
        if (outputUnitGraphs || outputCallGraph || outputControlFlowGraph) {
            LOGGER.info("Starting graph output... (" + cTimer.start(true) + ")");

            if (outputUnitGraphs) {
                try {
                    droidGraph.writeUnitGraphsToFile();
                } catch (IOException e) {
                    LOGGER.error("Problem writing methods to output file: " + e.getMessage());
                }
            }

            if (outputCallGraph) {
                try {
                    droidGraph.writeCallGraphToFile();
                } catch (Exception e) {
                    LOGGER.error("Problem writing call graph to output file: " + e.getMessage());
                }
            }

            if (outputControlFlowGraph) {
                try {
                    droidGraph.writeControlFlowGraphToFile();
                } catch (Exception e) {
                    LOGGER.error("Problem writing CFG to output file: " + e.getMessage());
                }
            }

            LOGGER.info("(" + cTimer.end() + ") Graph output took " + cTimer.secondsDuration() + " second(s).");
        }

        if (cmd.hasOption("s")) {
            LOGGER.info("Starting file output... (" + cTimer.start(true) + ")");
            try {
                droidGraph.outputCGDetails();
                droidGraph.writeFlowDroidAnalysisToFile();
                droidGraph.writeControlsToFile();
                droidGraph.outputCFGDetails();
            } catch (IOException e) {
                LOGGER.error("Failed to write app analysis to output files: " + e.getMessage());
            }
            LOGGER.info("(" + cTimer.end() + ") File output took " + cTimer.secondsDuration() + " second(s).");
        }

        LOGGER.info("End time: " + timer.end());
        LOGGER.info("Execution time: " + timer.secondsDuration() + " second(s).");
    }
}
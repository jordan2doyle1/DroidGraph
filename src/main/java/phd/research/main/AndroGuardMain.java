package phd.research.main;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.Timer;
import phd.research.androguard.AndroGuard;
import phd.research.core.DroidControls;
import phd.research.core.FlowDroidUtils;
import phd.research.graph.Viewer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Jordan Doyle
 */

public class AndroGuardMain {
    private static final Logger logger = LoggerFactory.getLogger(AndroGuardMain.class);

    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("a").longOpt("apk").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("APK file to analyse.").build());
        options.addOption(Option.builder("g").longOpt("graph").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("File containing AndroGuard call graph.").build());
        options.addOption(Option.builder("p").longOpt("android-platform").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Android SDK platform directory.").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("Directory for output files.").build());
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
            formatter.printUsage(writer, 80, "Import GML", options);
            writer.flush();
            System.exit(0);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Import GML", options);
            System.exit(0);
        }

        Timer timer = new Timer();
        logger.info("Start time: " + timer.start());

        File apk = new File(cmd.getOptionValue("a"));
        if (!apk.exists()) {
            logger.error("APK file does not exist (" + apk + ").");
            System.exit(10);
        }

        File agGraph = new File(cmd.getOptionValue("g"));
        if (!agGraph.exists()) {
            logger.error("AndroGuard graph file does not exist (" + agGraph + ").");
            System.exit(70);
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

        boolean outputAnalysis = cmd.hasOption("s");

        if (cmd.hasOption("c")) {
            try {
                FileUtils.cleanDirectory(outputDirectory);
            } catch (IOException e) {
                logger.error("Problem cleaning output directory: " + e.getMessage());
            }
        }

        Timer cTimer = new Timer();
        logger.info("Running FlowDroid... (" + cTimer.start(true) + ")");
        FlowDroidUtils.runFlowDroid(apk, androidPlatform, outputDirectory);
        logger.info("(" + cTimer.end() + ") FlowDroid took " + cTimer.secondsDuration() + " second(s).");

        File callbackFile = new File(outputDirectory + File.separator + FlowDroidUtils.CALLBACK_FILE_NAME);
        if (!callbackFile.exists()) {
            logger.error("Collected callbacks file does not exist.");
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

        if (outputAnalysis) {
            logger.info("Starting file output... (" + cTimer.start(true) + ")");
            Viewer viewer = new Viewer(callbackFile, droidControls);
            try {
                viewer.writeAnalysisToFile(outputDirectory, apk);
            } catch (IOException | XmlPullParserException e) {
                logger.error("Failed to write app analysis to output file: " + e.getMessage());
            }
            logger.info("(" + cTimer.end() + ") File output took " + cTimer.secondsDuration() + " second(s).");
        }

        logger.info("Running graph import... (" + cTimer.start(true) + ")");
        AndroGuard androGuard = new AndroGuard(agGraph);
        logger.info("(" + cTimer.end() + ") Graph import took " + cTimer.secondsDuration() + " second(s).");

        logger.info("Starting graph output... (" + cTimer.start(true) + ")");
        androGuard.outputGMLGraph(outputDirectory);
        logger.info("(" + cTimer.end() + ") Graph output took " + cTimer.secondsDuration() + " second(s).");

        if (outputAnalysis) {
            try {
                Viewer.outputCGDetails(outputDirectory, androGuard.getCallGraph());
            } catch (IOException e) {
                logger.error("Failed to write graph composition details to output file: " + e.getMessage());
            }
        }

        logger.info("End time: " + timer.end());
        logger.info("Execution time: " + timer.secondsDuration() + " second(s).");
    }
}

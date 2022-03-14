package phd.research.frontmatter;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FrameworkMain;
import phd.research.graph.Writer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(FrameworkMain.class);

    private static String fmOutput1;
    private static String fmOutput2;
    private static String outputDirectory;

    public static void main(String[] args) {
        LocalDateTime startDate = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy-HH:mm:ss");
        logger.info("Start time: " + dateFormatter.format(startDate));
        System.out.println("Start time: " + dateFormatter.format(startDate));

        Options options = new Options();
        options.addOption(Option.builder("fmo1").longOpt("Front Matter Output 1").required().hasArg()
                .desc("Output file 1 from Front Matter Analysis of an APK.").numberOfArgs(1).argName("FILE").build());
        options.addOption(Option.builder("fmo2").longOpt("Front Matter Output 2").required().hasArg()
                .desc("Output file 2 from Front Matter Analysis of an APK.").numberOfArgs(1).argName("FILE").build());
        options.addOption(Option.builder("od").longOpt("output-directory").desc("Directory for output files.")
                .hasArg().numberOfArgs(1).argName("DIRECTORY").build());

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
            fmOutput1 = cmd.getOptionValue("fmo1");
            if (!fileExists(fmOutput1)) {
                logger.error("Error: Front Matter output file does not exist (" + fmOutput1 + ").");
                System.err.println("Error: Front Matter output file does not exist (" + fmOutput1 + ").");
                System.exit(10);
            }

            fmOutput2 = cmd.getOptionValue("fmo2");
            if (!fileExists(fmOutput2)) {
                logger.error("Error: Front Matter output file does not exist (" + fmOutput2 + ").");
                System.err.println("Error: Front Matter output file does not exist (" + fmOutput2 + ").");
                System.exit(10);
            }

            outputDirectory = (cmd.hasOption("od") ? cmd.getOptionValue("od") :
                    System.getProperty("user.dir") + "/output/");
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
        }

        try {
            Writer.cleanDirectory(outputDirectory);
        } catch (IOException e) {
            logger.error("Error cleaning output directory: " + e.getMessage());
        }

        try {
            Compare fmCompare = new Compare(readJSONOutput(fmOutput1), readJSONOutput(fmOutput2));
            fmCompare.findDiff();
            fmCompare.writeDiffToFile(
                    "fmDiff:" + new File(fmOutput1).getName() + "->" + new File(fmOutput2).getName());
            System.out.println(fmCompare.getDiffJsonString() + "\n");
        } catch (IOException e) {
            logger.error("Error: Problem reading output files." + e);
            System.err.println("Error: Problem reading output files." + e);
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

        if (cmd != null)
            return cmd.hasOption("h");

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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    private static JsonObject readJSONOutput(String file) throws IOException {
        File outputFile = new File(file);
        if (!outputFile.exists())
            throw new IOException("Output file does not exist!");

        JsonReader reader = Json.createReader(new FileInputStream(outputFile));
        JsonObject output = reader.readObject();
        reader.close();

        return output;
    }
}

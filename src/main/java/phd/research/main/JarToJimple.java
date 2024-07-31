package phd.research.main;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.helper.Timer;
import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.SourceLocator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author Jordan Doyle
 */

public class JarToJimple {

    private static final Logger LOGGER = LoggerFactory.getLogger(JarToJimple.class);

    public static void main(String[] args) throws IOException {

        Timer timer = new Timer();
        LOGGER.info("Start time: {}", timer.start());

        Options options = new Options();
        options.addOption(Option.builder("j").longOpt("jar-file").required().hasArg().numberOfArgs(1).argName("FILE")
                .desc("The jar file to convert to Jimple.").build());
        options.addOption(Option.builder("o").longOpt("output-directory").hasArg().numberOfArgs(1).argName("DIRECTORY")
                .desc("The directory for storing output files.").build());
        options.addOption(Option.builder("h").longOpt("help").desc("Display help.").build());

        CommandLine cmd = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            final PrintWriter writer = new PrintWriter(System.out);
            formatter.printUsage(writer, 80, "JarToJimple", options);
            writer.flush();
            System.exit(10);
        }

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("JarToJimple", options);
            System.exit(0);
        }

        File jar_file = new File(cmd.getOptionValue("j"));
        if (!jar_file.exists()) {
            LOGGER.error("Jar file ({}) does not exist.", jar_file);
            System.exit(20);
        }

        File output_directory = cmd.hasOption("o") ? new File(cmd.getOptionValue("o")) : new File("output");
        if (!output_directory.exists()) {
            LOGGER.error("Output directory ({}) does not exist.", output_directory);
            System.exit(30);
        }

        soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_class);
        soot.options.Options.v().set_output_format(soot.options.Options.output_format_jimple);
        soot.options.Options.v().set_soot_classpath("/Users/jordandoyle/.jenv/versions/1.8.0.392/jre/lib/rt.jar");
        soot.options.Options.v().set_process_dir(Collections.singletonList(jar_file.getAbsolutePath()));
        soot.options.Options.v().set_output_dir(output_directory.getAbsolutePath());

        Scene.v().loadNecessaryClasses();

        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            String fileName = SourceLocator.v().getFileNameFor(sootClass, soot.options.Options.output_format_jimple);
            OutputStream streamOut = Files.newOutputStream(Paths.get(fileName));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));

            LOGGER.info("Writing class {} to Jimple file.", sootClass.getName());
            Printer.v().printTo(sootClass, writerOut);

            writerOut.flush();
            streamOut.close();
        }

        LOGGER.info("End time: {}", timer.end());
        LOGGER.info("Execution time: {} second(s).", timer.secondsDuration());
    }
}
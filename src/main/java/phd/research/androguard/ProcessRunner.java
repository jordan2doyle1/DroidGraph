package phd.research.androguard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.main.AndroGuardMain;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessRunner {

    private static final Logger logger = LoggerFactory.getLogger(AndroGuardMain.class);

    private File virtualEnvironmentDirectory;

    public ProcessRunner() {

    }

    public void setVirtualEnvironmentDirectory(File directory) throws IOException {
        if (confirmVirtualEnvironment(directory)) {
            this.virtualEnvironmentDirectory = directory;
        }
    }

    public boolean isPythonInstalled(String baseVersion, boolean virtual) throws IOException, InterruptedException {
        String resultRegex = "^Python " + baseVersion + "(?:(?:\\.\\d{1,2}\\.?)+)?$";
        String[] command;

        if (virtual) {
            confirmVirtualEnvironment(this.virtualEnvironmentDirectory);
            command = new String[]{
                    this.virtualEnvironmentDirectory + File.separator + "bin" + File.separator + "python3",
                    "--version"};
        } else {
            command = new String[]{"python3", "--version"};
        }

        return runCommand(command, resultRegex);
    }

    @SuppressWarnings("unused")
    public boolean isPyEnvInstalled() throws IOException, InterruptedException {
        String[] command = {"pyenv", "--version"};
        return runCommand(command, "^pyenv (?:\\d{1,2}\\.?)+$");
    }

    public boolean isAndroGuardInstalled(String baseVersion, boolean virtual) throws IOException, InterruptedException {
        String resultRegex = "^androguard, version " + baseVersion + "(?:(?:\\.\\d{1,2}\\.?)+)?$";
        String[] command;

        if (virtual) {
            confirmVirtualEnvironment(this.virtualEnvironmentDirectory);
            command = new String[]{
                    this.virtualEnvironmentDirectory + File.separator + "bin" + File.separator + "androguard",
                    "--version"};
        } else {
            command = new String[]{"androguard", "--version"};
        }

        return runCommand(command, resultRegex);
    }

    public List<String> runAndroGuardCg(File apk, File output, boolean virtual)
            throws IOException, InterruptedException {
        String[] command;

        if (virtual) {
            confirmVirtualEnvironment(this.virtualEnvironmentDirectory);
            command = new String[]{
                    this.virtualEnvironmentDirectory + File.separator + "bin" + File.separator + "androguard", "cg",
                    "-o", output.getAbsolutePath() + File.separator + "AndroGuardCG.gml", apk.getAbsolutePath()};
        } else {
            command = new String[]{"androguard", "cg", apk.getAbsolutePath(), output.getAbsolutePath()};
        }

        return runCommand(command);
    }

    private boolean runCommand(String[] command, String expectedOutputRegex) throws IOException, InterruptedException {
        List<String> results = runCommand(command);
        return results.size() == 1 && results.get(0).matches(expectedOutputRegex);
    }

    private List<String> runCommand(String[] command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        List<String> results = readProcessOutput(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Error occurred while executing command: " + Arrays.toString(command));
        }

        return results;
    }

    private List<String> readProcessOutput(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> output = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            output.add(line);
        }

        output.stream().filter(l -> l.equals("")).forEach(output::remove);
        return output;
    }

    private boolean confirmVirtualEnvironment(File directory) throws IOException {
        File activationScript = new File(directory + File.separator + "bin" + File.separator + "activate");
        if (directory.isDirectory() && activationScript.isFile()) {
            return true;
        } else {
            throw new IOException("Directory does not exist or is not a python virtual environment: " + directory);
        }
    }
}

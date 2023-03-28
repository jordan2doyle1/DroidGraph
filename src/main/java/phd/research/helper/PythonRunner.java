package phd.research.helper;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Jordan Doyle
 */

@SuppressWarnings("unused")
public class PythonRunner {

    private File virtualEnvDirectory;

    public PythonRunner() {

    }

    @SuppressWarnings("SameReturnValue")
    private static boolean confirmVirtualEnvironment(File directory) throws IOException {
        File activationScript = new File(directory + File.separator + "bin" + File.separator + "activate");
        if (directory.isDirectory() && activationScript.isFile()) {
            return true;
        } else {
            throw new IOException("Directory does not exist or is not a python virtual environment: " + directory);
        }
    }

    private static boolean runCommand(String[] command, String expectedOutputRegex)
            throws IOException, InterruptedException, RuntimeException {
        List<String> results = PythonRunner.runCommand(command);
        return results.size() == 1 && results.get(0).matches(expectedOutputRegex);
    }

    private static List<String> runCommand(String[] command)
            throws IOException, InterruptedException, RuntimeException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        List<String> results = PythonRunner.readProcessOutput(process.getInputStream());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error (exit code: " + exitCode + ") occurred while executing command: " +
                    Arrays.toString(command));
        }

        return results;
    }

    private static List<String> readProcessOutput(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> output = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            output.add(line);
        }

        output.stream().filter(l -> l.equals("")).forEach(output::remove);
        return output;
    }

    public void setVirtualEnvDirectory(File directory) throws IOException {
        if (PythonRunner.confirmVirtualEnvironment(directory)) {
            this.virtualEnvDirectory = directory;
        }
    }

    public boolean isPythonInstalled(String baseVersion, boolean virtual)
            throws IOException, InterruptedException, RuntimeException {
        String resultRegex = "^Python " + baseVersion + "(?:(?:\\.\\d{1,2}\\.?)+)?$";
        String[] command = {"python3", "--version"};

        if (virtual) {
            PythonRunner.confirmVirtualEnvironment(this.virtualEnvDirectory);
            String pythonPath = this.virtualEnvDirectory + File.separator + "bin" + File.separator + "python3";
            command = new String[]{pythonPath, "--version"};
        }

        return PythonRunner.runCommand(command, resultRegex);
    }

    public boolean isAndroGuardInstalled(String baseVersion, boolean virtual)
            throws IOException, InterruptedException, RuntimeException {
        String resultRegex = "^androguard, version " + baseVersion + "(?:(?:\\.\\d{1,2}\\.?)+)?$";
        String[] command = {"androguard", "--version"};

        if (virtual) {
            PythonRunner.confirmVirtualEnvironment(this.virtualEnvDirectory);
            String androguardPath = this.virtualEnvDirectory + File.separator + "bin" + File.separator + "androguard";
            command = new String[]{androguardPath, "--version"};
        }

        return PythonRunner.runCommand(command, resultRegex);
    }

    public List<String> runAndroGuard(File apk, File outputDirectory, boolean virtual)
            throws IOException, InterruptedException, RuntimeException {
        boolean pythonInstalled = this.isPythonInstalled("3.8", virtual);
        boolean androguardInstalled = this.isAndroGuardInstalled("3.3.5", virtual);
        if (pythonInstalled && androguardInstalled) {
            return this.runAndroGuardCallGraph(apk, outputDirectory, virtual);
        }
        throw new RuntimeException("Python or AndroGuard not installed.");
    }

    private List<String> runAndroGuardCallGraph(File apk, File outputDirectory, boolean virtual)
            throws IOException, InterruptedException, RuntimeException {
        String[] command = {"androguard", "cg", apk.getAbsolutePath(), outputDirectory.getAbsolutePath()};

        if (virtual) {
            PythonRunner.confirmVirtualEnvironment(this.virtualEnvDirectory);
            String androguardPath = this.virtualEnvDirectory + File.separator + "bin" + File.separator + "androguard";
            String graphOutputFileName = outputDirectory.getAbsolutePath() + File.separator + "AndroGuardCG.gml";
            command = new String[]{androguardPath, "cg", "-o", graphOutputFileName, apk.getAbsolutePath()};
        }

        return PythonRunner.runCommand(command);
    }
}

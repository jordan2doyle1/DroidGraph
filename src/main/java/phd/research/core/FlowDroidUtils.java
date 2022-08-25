package phd.research.core;

import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Format;
import phd.research.helper.API;
import phd.research.helper.DroidControlFactory;
import phd.research.helper.MenuFileParser;
import soot.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.LayoutControlFactory;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.options.Options;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */

public class FlowDroidUtils {

    public static final String CALLBACK_FILE_NAME = "CollectedCallbacks";

    public static String getBasePackageName(File apk) throws XmlPullParserException, IOException {
        try (ProcessManifest manifest = new ProcessManifest(apk)) {
            return manifest.getPackageName();
        }
    }

    public static Set<SootClass> getLaunchActivities(File apk) throws XmlPullParserException, IOException {
        try (ProcessManifest manifest = new ProcessManifest(apk)) {
            Set<SootClass> launchActivities = new HashSet<>();
            for (AXmlNode activity : manifest.getLaunchableActivityNodes()) {
                if (activity.hasAttribute("name")) {
                    // WARNING: Excluding valid launch activities if the developer doesn't provide the name attribute.
                    String activityName = activity.getAttribute("name").getValue().toString();
                    SootClass launchActivity = Scene.v().getSootClass(activityName);
                    if (launchActivity != null) {
                        launchActivities.add(launchActivity);
                    }
                }
            }
            return launchActivities;
        }
    }

    public static ARSCFileParser getResources(File apk) throws IOException {
        ARSCFileParser resources = new ARSCFileParser();
        resources.parse(apk.getAbsolutePath());
        return resources;
    }

    public static Set<SootClass> getEntryPointClasses(File apk) throws XmlPullParserException, IOException {
        try (ProcessManifest manifest = new ProcessManifest(apk)) {
            Set<SootClass> entryPoints = new HashSet<>();
            for (String entryPoint : manifest.getEntryPointClasses()) {
                SootClass entryPointClass = Scene.v().getSootClass(entryPoint);
                if (entryPointClass != null) {
                    entryPoints.add(entryPointClass);
                }
            }
            return entryPoints;
        }
    }

    public static MenuFileParser getMenuFileParser(File apk) throws XmlPullParserException, IOException {
        try (ProcessManifest manifest = new ProcessManifest(apk)) {
            LayoutControlFactory controlFactory = new DroidControlFactory();
            controlFactory.setLoadAdditionalAttributes(true);
            MenuFileParser menuParser = new MenuFileParser();
            menuParser.setControlFactory(controlFactory);
            menuParser.parseLayoutFileDirect(manifest.getPackageName(), apk.getAbsolutePath());
            return menuParser;
        }
    }

    public static LayoutFileParser getLayoutFileParser(File apk) throws XmlPullParserException, IOException {
        try (ProcessManifest manifest = new ProcessManifest(apk)) {
            LayoutControlFactory controlFactory = new DroidControlFactory();
            controlFactory.setLoadAdditionalAttributes(true);
            LayoutFileParser layoutParser = new LayoutFileParser(manifest.getPackageName(), getResources(apk));
            layoutParser.setControlFactory(controlFactory);
            layoutParser.parseLayoutFileDirect(apk.getAbsolutePath());
            return layoutParser;
        }
    }

    public static Format stringToFormat(String format) throws RuntimeException {
        switch (format) {
            case "DOT":
                return Format.dot;
            case "JSON":
                return Format.json;
            case "ALL":
                return Format.all;
            default:
                throw new RuntimeException("Unrecognised Graph Output Format.");
        }
    }

    private static void initializeSoot(File apk, File platform, File outputDirectory) {
        G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(soot.options.Options.output_format_none);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(apk.getAbsolutePath()));
        Options.v().set_android_jars(platform.getAbsolutePath());
        Options.v().set_src_prec(soot.options.Options.src_prec_apk_class_jimple);
        Options.v().set_keep_offset(false);
        Options.v().set_keep_line_number(false);
        Options.v().set_throw_analysis(soot.options.Options.throw_analysis_dalvik);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_output_dir(outputDirectory.getAbsolutePath());

        // "androidx.*" "android.*"
        List<String> excludeList = new LinkedList<>(
                Arrays.asList("java.*", "javax.*", "sun.*", "org.apache.*", "org.eclipse.*", "soot.*"));
        Options.v().set_exclude(excludeList);

        Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(platform.getAbsolutePath(), apk.getAbsolutePath()));
        Main.v().autoSetOptions();

        Scene.v().addBasicClass("android.view.View", soot.SootClass.BODIES);
        Scene.v().loadNecessaryClasses();

        // Allows you to add pre-processors that run before call-graph construction. Only enabled in whole-program mode.
        PackManager.v().getPack("wjpp").apply();

        // Patch the call graph to support additional edges. We do this now, because during callback discovery, the
        // context-insensitive call graph algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
    }

    private static InfoflowAndroidConfiguration getFlowDroidConfiguration(File apk, File platform, File directory) {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig().setAndroidPlatformDir(platform.getAbsolutePath());
        String sourceAndSinkFileName = System.getProperty("user.dir") + File.separator + "SourcesAndSinks.txt";
        config.getAnalysisFileConfig().setSourceSinkFile(sourceAndSinkFileName);
        config.getAnalysisFileConfig().setTargetAPKFile(apk.getAbsolutePath());
        config.getCallbackConfig().setSerializeCallbacks(true);
        config.getCallbackConfig().setCallbacksFile(directory + File.separator + CALLBACK_FILE_NAME);
        return config;
    }

    @API
    public static void runFlowDroid(File apk, File platform, File directory) {
        FlowDroidUtils.initializeSoot(apk, platform, directory);
        InfoflowAndroidConfiguration configuration = FlowDroidUtils.getFlowDroidConfiguration(apk, platform, directory);

        SetupApplication application = new SetupApplication(configuration);
        application.constructCallgraph();
    }
}

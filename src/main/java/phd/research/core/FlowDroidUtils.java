package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class FlowDroidUtils {

    private static final Logger logger = LoggerFactory.getLogger(FlowDroidUtils.class);

    public FlowDroidUtils() {

    }

    public static ProcessManifest getManifest(String apk) {
        ProcessManifest manifest;

        try {
            manifest = new ProcessManifest(apk);
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return null;
        }

        return manifest;
    }

    public static Set<SootClass> getLaunchActivities(String apk) {
        ProcessManifest manifest = getManifest(apk);

        Set<SootClass> launchActivities = new HashSet<>();
        if (manifest != null) {
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
        }

        return launchActivities;
    }

    public static String getBasePackageName(String apk) {
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            return manifest.getPackageName();
        }

        return null;
    }

    public static Set<SootClass> getEntryPointClasses(String apk) {
        Set<SootClass> entryPoints = new HashSet<>();
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            for (String entryPoint : manifest.getEntryPointClasses()) {
                SootClass entryPointClass = Scene.v().getSootClass(entryPoint);
                if (entryPointClass != null) {
                    entryPoints.add(entryPointClass);
                }
            }
        }

        return entryPoints;
    }

    public static ARSCFileParser getResources(String apk) {
        ARSCFileParser resources = new ARSCFileParser();

        try {
            resources.parse(apk);
        } catch (IOException e) {
            logger.error("Problem getting resources: " + e.getMessage());
        }

        return resources;
    }

    public static LayoutFileParser getLayoutFileParser(String apk) {
        LayoutFileParser layoutParser;
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            layoutParser = new LayoutFileParser(manifest.getPackageName(), getResources(apk));
            layoutParser.parseLayoutFileDirect(apk);
            return layoutParser;
        }

        return null;
    }

    public static CollectedCallbacks readCollectedCallbacks(File callbackFile) {
        CollectedCallbacks callbacks = null;

        try {
            callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        } catch (FileNotFoundException e) {
            logger.error("Collected Callbacks File Not Found:" + e.getMessage());
        }

        return callbacks;
    }

    private static void initializeSoot(String apk, String androidPlatform, String outputDirectory) {
        G.reset();  // Clean up any old Soot instance we may have

        soot.options.Options.v().set_no_bodies_for_excluded(true);
        soot.options.Options.v().set_allow_phantom_refs(true);
        soot.options.Options.v().set_output_format(soot.options.Options.output_format_none);
        soot.options.Options.v().set_whole_program(true);
        soot.options.Options.v().set_process_dir(Collections.singletonList(apk));
        soot.options.Options.v().set_android_jars(androidPlatform);
        soot.options.Options.v().set_src_prec(soot.options.Options.src_prec_apk_class_jimple);
        soot.options.Options.v().set_keep_offset(false);
        soot.options.Options.v().set_keep_line_number(false);
        soot.options.Options.v().set_throw_analysis(soot.options.Options.throw_analysis_dalvik);
        soot.options.Options.v().set_process_multiple_dex(true);
        soot.options.Options.v().set_ignore_resolution_errors(true);
        soot.options.Options.v().set_output_dir(outputDirectory);

        Scene.v().addBasicClass("android.view.View", soot.SootClass.BODIES);

        // Set Soot configuration options. Note this needs to be done before computing the classpath.
        // (SA) Exclude classes of android.* will cause layout class cannot be loaded for layout file based callback
        // analysis. Added back the exclusion, because removing it breaks calls to Android SDK stubs.
        // (JD) Remove the android.* and androidx.* within FlowDroid and see what happens.
        List<String> excludeList = new LinkedList<>(
                Arrays.asList("java.*", "javax.*", "sun.*", "org.apache.*", "org" + ".eclipse.*", "soot.*", "android.*",
                        "androidx.*"
                             ));
        soot.options.Options.v().set_exclude(excludeList);

        soot.options.Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(androidPlatform, apk));
        Main.v().autoSetOptions();

        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("wjpp").apply();

        // Patch the call graph to support additional edges. We do this now, because during callback discovery, the
        // context-insensitive call graph algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
    }

    private static InfoflowAndroidConfiguration getFlowDroidConfiguration(String apk, String androidPlatform,
            String outputDirectory) {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidPlatform);
        config.getAnalysisFileConfig().setSourceSinkFile(System.getProperty("user.dir") + "/SourcesAndSinks.txt");
        config.getAnalysisFileConfig().setTargetAPKFile(apk);
        config.getCallbackConfig().setSerializeCallbacks(true);
        config.getCallbackConfig().setCallbacksFile(outputDirectory + "CollectedCallbacks");
        return config;
    }

    public static void runFlowDroid(String apk, String androidPlatform, String outputDirectory) {
        FlowDroidUtils.initializeSoot(apk, androidPlatform, outputDirectory);
        InfoflowAndroidConfiguration configuration =
                FlowDroidUtils.getFlowDroidConfiguration(apk, androidPlatform, outputDirectory);
        SetupApplication application = new SetupApplication(configuration);
        application.constructCallgraph();
    }
}

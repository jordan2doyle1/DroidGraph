package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.options.Options;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class FlowDroidUtils {

    private static final Logger logger = LoggerFactory.getLogger(FlowDroidUtils.class);

    public FlowDroidUtils() {

    }

    public static ProcessManifest getManifest(File apk) {
        ProcessManifest manifest;

        try {
            manifest = new ProcessManifest(apk);
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return null;
        }

        return manifest;
    }

    public static Set<SootClass> getLaunchActivities(File apk) {
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

    public static String getBasePackageName(File apk) {
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            return manifest.getPackageName();
        }

        return null;
    }

    public static Set<SootClass> getEntryPointClasses(File apk) {
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

    public static ARSCFileParser getResources(File apk) {
        ARSCFileParser resources = new ARSCFileParser();

        try {
            resources.parse(apk.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Problem getting resources: " + e.getMessage());
        }

        return resources;
    }

    public static LayoutFileParser getLayoutFileParser(File apk) {
        LayoutFileParser layoutParser;
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            layoutParser = new LayoutFileParser(manifest.getPackageName(), getResources(apk));
            layoutParser.parseLayoutFileDirect(apk.getAbsolutePath());
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

    static void initializeSoot(File apk, File androidPlatform, File outputDirectory) {
        G.reset();  // Clean up any old Soot instance we may have

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(soot.options.Options.output_format_none);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(apk.getAbsolutePath()));
        Options.v().set_android_jars(androidPlatform.getAbsolutePath());
        Options.v().set_src_prec(soot.options.Options.src_prec_apk_class_jimple);
        Options.v().set_keep_offset(false);
        Options.v().set_keep_line_number(false);
        Options.v().set_throw_analysis(soot.options.Options.throw_analysis_dalvik);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_output_dir(outputDirectory.getAbsolutePath());

        // Set Soot configuration options. Note this needs to be done before computing the classpath.
        // (SA) Exclude classes of android.* will cause layout class cannot be loaded for layout file based callback
        // analysis. Added back the exclusion, because removing it breaks calls to Android SDK stubs.
        // (JD) Remove the android.* and androidx.* within FlowDroid and see what happens.
        List<String> excludeList = new LinkedList<>(
                Arrays.asList("java.*", "javax.*", "sun.*", "org.apache.*", "org.eclipse.*", "soot.*", "android.*",
                        "androidx.*"));

        Options.v().set_exclude(excludeList);

        Options.v().set_soot_classpath(Scene.v().getAndroidJarPath(androidPlatform.getAbsolutePath(), apk.getAbsolutePath()));
        Main.v().autoSetOptions();

        Scene.v().addBasicClass("android.view.View", soot.SootClass.BODIES);

        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("wjpp").apply();

        // Patch the call graph to support additional edges. We do this now, because during callback discovery, the
        // context-insensitive call graph algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
    }

    static InfoflowAndroidConfiguration getFlowDroidConfiguration(String apk, String androidPlatform,
            String outputDirectory) {
        //SootConfigForAndroid = new SetConfigForAndroid();
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidPlatform);
        config.getAnalysisFileConfig().setSourceSinkFile(System.getProperty("user.dir") + "/SourcesAndSinks.txt");
        config.getAnalysisFileConfig().setTargetAPKFile(apk);
        config.getCallbackConfig().setSerializeCallbacks(true);
        config.getCallbackConfig().setCallbacksFile(outputDirectory + "/CollectedCallbacks");
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
        return config;
    }

    public static void runFlowDroid(File apk, File androidPlatform, File outputDirectory) {
        FlowDroidUtils.initializeSoot(apk, androidPlatform, outputDirectory);

        InfoflowAndroidConfiguration configuration =
                FlowDroidUtils.getFlowDroidConfiguration(apk.getAbsolutePath(), androidPlatform.getAbsolutePath(),
                        outputDirectory.getAbsolutePath());
        SetupApplication application = new SetupApplication(configuration);

        application.setSootConfig(new LocalConfig());

        List<String> e = Options.v().exclude();

        application.constructCallgraph();
    }

    public static class LocalConfig extends SootConfigForAndroid {

        @Override
        public void setSootOptions(Options options, InfoflowConfiguration config) {
            // explicitly include packages for shorter runtime:
            List<String> excludeList = new LinkedList<>();
            excludeList.add("java.*");
            excludeList.add("sun.*");

            // exclude classes of android.* will cause layout class cannot be
            // loaded for layout file based callback analysis.

            // 2020-07-26 (SA): added back the exclusion, because removing it breaks
            // calls to Android SDK stubs. We need a proper test case for the layout
            // file issue and then see how to deal with it.
            excludeList.add("android.*");
            excludeList.add("androidx.*");

            excludeList.add("org.apache.*");
            excludeList.add("org.eclipse.*");
            excludeList.add("soot.*");
            excludeList.add("javax.*");
            options.set_exclude(excludeList);
            Options.v().set_no_bodies_for_excluded(true);
        }
    }
}

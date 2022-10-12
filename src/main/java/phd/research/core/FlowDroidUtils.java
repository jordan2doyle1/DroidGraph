package phd.research.core;

import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Format;
import phd.research.graph.Filter;
import phd.research.helper.API;
import phd.research.helper.DroidControlFactory;
import phd.research.helper.MenuFileParser;
import soot.*;
import soot.jimple.DefinitionStmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.LayoutControlFactory;
import soot.jimple.infoflow.cfg.LibraryClassPatcher;
import soot.jimple.toolkits.ide.exampleproblems.IFDSReachingDefinitions;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */

public class FlowDroidUtils {

//    public static final ProcessManifest manifest = readManifest();
//
//    public static ProcessManifest readManifest() {
//        try {
//            return new ProcessManifest(new File(""));
//        } catch (IOException | XmlPullParserException e) {
//            throw new RuntimeException(e);
//        }
//    }

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

//    public static Set<SootClass> getEntryPointClasses() {
//        Set<SootClass> entryPoints = new HashSet<>();
//        for (String entryPoint : manifest.getEntryPointClasses()) {
//            SootClass entryPointClass = Scene.v().getSootClass(entryPoint);
//            entryPoints.add(entryPointClass);
//        }
//        return entryPoints;
//    }

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
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.herosifds", new FlowDroidUtils.IFDSDataFlowTransformer()));

        // Patch the call graph to support additional edges. We do this now, because during callback discovery, the
        // context-insensitive call graph algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
        PackManager.v().runPacks();
    }

    public static class IFDSDataFlowTransformer extends SceneTransformer {
        @Override
        protected void internalTransform(String phaseName, Map<String, String> options) {
            JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
            IFDSTabulationProblem<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod, InterproceduralCFG<Unit,SootMethod>>
                    problem = new IFDSReachingDefinitions(icfg);
            //IFDSReachingDefinitions problem = new IFDSReachingDefinitions(icfg);
            IFDSSolver<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod,
                    InterproceduralCFG<Unit, SootMethod>> solver = new IFDSSolver<>(problem);

            System.out.println("Starting solver");
            solver.solve();
            SootClass clazz = Scene.v().getSootClassUnsafe("com.example.activity.lifecycle.ActivityA");
            SootMethod onCreate = clazz.getMethod("onCreate");
            for (Unit unit : onCreate.getActiveBody().getUnits()) {
                solver.ifdsResultsAt(unit);
            }
            System.out.println("Done");
        }
    }

    public static class MethodOutput extends BodyTransformer {

        List<SootMethod> filtered = new ArrayList<>();

        @Override
        protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
            SootMethod currentMethod = b.getMethod();
            if (Filter.isValidMethod(currentMethod)) {
                filtered.add(currentMethod);
            }
        }

    }

    private static InfoflowAndroidConfiguration getFlowDroidConfiguration(File apk, File platform) {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig().setAndroidPlatformDir(platform.getAbsolutePath());
        String sourceAndSinkFileName = System.getProperty("user.dir") + File.separator + "SourcesAndSinks.txt";
        config.getAnalysisFileConfig().setSourceSinkFile(sourceAndSinkFileName);
        config.getAnalysisFileConfig().setTargetAPKFile(apk.getAbsolutePath());
        config.getCallbackConfig().setSerializeCallbacks(true);
        config.getCallbackConfig().setCallbacksFile(System.getProperty("user.dir") + File.separator + "FlowDroidCallbacks");
        return config;
    }

    @API
    public static void runFlowDroid(File apk, File platform, File directory) {
        FlowDroidUtils.initializeSoot(apk, platform, directory);
        InfoflowAndroidConfiguration configuration = FlowDroidUtils.getFlowDroidConfiguration(apk, platform);

        SetupApplication application = new SetupApplication(configuration);
        application.constructCallgraph();
    }
}

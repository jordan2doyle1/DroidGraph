package phd.research.singletons;

import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.Timer;
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

public class FlowDroidAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDroidAnalysis.class);

    private static FlowDroidAnalysis instance = null;

    @NotNull
    private final ProcessManifest manifest;
    @NotNull
    private final ARSCFileParser resources;

    private boolean sootInitialised;
    private boolean flowDroidExecuted;

    private SetupApplication application;

    private FlowDroidAnalysis() {
        this.flowDroidExecuted = false;
        this.sootInitialised = false;

        try {
            this.manifest = new ProcessManifest(GraphSettings.v().getApkFile());
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("Failed to process APK manifest file." + e.getMessage());
        }

        this.resources = new ARSCFileParser();
        try {
            this.resources.parse(GraphSettings.v().getApkFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse APK resources." + e.getMessage());
        }
    }

    public static FlowDroidAnalysis v() {
        if (instance == null) {
            instance = new FlowDroidAnalysis();
        }
        return instance;
    }

    public String getBasePackageName() {
        return this.manifest.getPackageName();
    }

    @NotNull
    public ARSCFileParser getResources() {
        return this.resources;
    }

    public Set<SootClass> getLaunchActivities() {
        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        Set<SootClass> launchActivities = new HashSet<>();
        for (AXmlNode activity : this.manifest.getLaunchableActivityNodes()) {
            if (activity.hasAttribute("name")) {
                // WARNING: Excluding valid launch activities if the developer doesn't provide the name attribute.
                String activityName = activity.getAttribute("name").getValue().toString();
                SootClass launchActivity = Scene.v().getSootClassUnsafe(activityName);
                if (launchActivity != null) {
                    launchActivities.add(launchActivity);
                }
            }
        }

        return launchActivities;
    }

    public Set<SootClass> getEntryPointClasses() {
        if (this.isFlowDroidExecuted()) {
            return this.application.getEntrypointClasses();
        }

        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        Set<SootClass> entryPoints = new HashSet<>();
        for (String entryPoint : this.manifest.getEntryPointClasses()) {
            SootClass entryPointClass = Scene.v().getSootClassUnsafe(entryPoint);
            if (entryPointClass != null) {
                entryPoints.add(entryPointClass);
            }
        }

        return entryPoints;
    }

    public MenuFileParser getMenuFileParser() {
        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        LayoutControlFactory controlFactory = new DroidControlFactory();
        controlFactory.setLoadAdditionalAttributes(true);
        MenuFileParser menuParser = new MenuFileParser();
        menuParser.setControlFactory(controlFactory);
        menuParser.parseLayoutFileDirect(this.getBasePackageName(), GraphSettings.v().getApkFile().getAbsolutePath());
        return menuParser;
    }

    public LayoutFileParser getLayoutFileParser() {
        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        LayoutControlFactory controlFactory = new DroidControlFactory();
        controlFactory.setLoadAdditionalAttributes(true);
        LayoutFileParser layoutParser = new LayoutFileParser(this.getBasePackageName(), this.resources);
        layoutParser.setControlFactory(controlFactory);
        layoutParser.parseLayoutFileDirect(GraphSettings.v().getApkFile().getAbsolutePath());
        return layoutParser;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isSootInitialised() {
        return this.sootInitialised;
    }

    public boolean isFlowDroidExecuted() {
        return this.flowDroidExecuted;
    }

    public void initializeSoot() {
        Timer timer = new Timer();
        LOGGER.info("Initializing Soot... (" + timer.start(true) + ")");

        G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(soot.options.Options.output_format_none);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(GraphSettings.v().getApkFile().getAbsolutePath()));
        Options.v().set_android_jars(GraphSettings.v().getPlatformDirectory().getAbsolutePath());
        Options.v().set_src_prec(Options.src_prec_apk_class_jimple);
        Options.v().set_throw_analysis(Options.throw_analysis_dalvik);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_output_dir(GraphSettings.v().getOutputDirectory().getAbsolutePath());

        List<String> excludeList = new LinkedList<>(
                Arrays.asList("java.*", "javax.*", "sun.*", "org.apache.*", "org.eclipse.*", "soot.*"));
        Options.v().set_exclude(excludeList);

        Options.v().set_soot_classpath(Scene.v()
                .getAndroidJarPath(GraphSettings.v().getPlatformDirectory().getAbsolutePath(),
                        GraphSettings.v().getApkFile().getAbsolutePath()
                                  ));
        Main.v().autoSetOptions();

        Scene.v().addBasicClass("android.view.View", soot.SootClass.BODIES);
        Scene.v().loadNecessaryClasses();

        PackManager.v().getPack("wjpp").apply();

        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();

        this.sootInitialised = true;

        LOGGER.info("(" + timer.end() + ") Soot initialization took " + timer.secondsDuration() + " second(s).");
    }

    public void runFlowDroid() {
        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        Timer timer = new Timer();
        LOGGER.info("Running FlowDroid Analysis... (" + timer.start(true) + ")");

        InfoflowAndroidConfiguration configuration = this.getFlowDroidConfiguration();
        this.application = new SetupApplication(configuration);
        this.application.constructCallgraph();

        this.flowDroidExecuted = true;

        LOGGER.info("(" + timer.end() + ") FlowDroid analysis took " + timer.secondsDuration() + " second(s).");
    }

    private InfoflowAndroidConfiguration getFlowDroidConfiguration() {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig()
                .setAndroidPlatformDir(GraphSettings.v().getPlatformDirectory().getAbsolutePath());
        String sourceAndSinkFileName = System.getProperty("user.dir") + File.separator + "SourcesAndSinks.txt";
        config.getAnalysisFileConfig().setSourceSinkFile(sourceAndSinkFileName);
        config.getAnalysisFileConfig().setTargetAPKFile(GraphSettings.v().getApkFile().getAbsolutePath());
        config.getCallbackConfig().setSerializeCallbacks(true);
        config.getCallbackConfig().setCallbacksFile(GraphSettings.v().getFlowDroidCallbacksFile().getAbsolutePath());
        return config;
    }

    @SuppressWarnings("unused")
    private String getMethodUnitStructure(String methodSignature) {
        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        StringBuilder builder = new StringBuilder(String.format("**** Method: %s ****\n", methodSignature));
        SootMethod method = Scene.v().getMethod(methodSignature);
        if (method != null) {
            if (method.hasActiveBody()) {
                for (Unit unit : method.getActiveBody().getUnits()) {
                    builder.append(unit.toString()).append("\n");
                }
            }
        }

        return builder.append("**** END ****").toString();
    }

    @SuppressWarnings("unused")
    private void runIFDSTransformer() {
        if (!this.isSootInitialised()) {
            this.initializeSoot();
        }

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", new IFDSDataFlowTransformer()));
        PackManager.v().runPacks();
    }

    public static class IFDSDataFlowTransformer extends SceneTransformer {
        @Override
        protected void internalTransform(String phaseName, Map<String, String> options) {
            JimpleBasedInterproceduralCFG cfg = new JimpleBasedInterproceduralCFG();
            IFDSReachingDefinitions problem = new IFDSReachingDefinitions(cfg);
            IFDSSolver<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod, InterproceduralCFG<Unit, SootMethod>>
                    solver = new IFDSSolver<>(problem);
            solver.solve();

            SootClass clazz = Scene.v().getSootClassUnsafe("com.example.activity.lifecycle .ActivityA");
            SootMethod onCreate = clazz.getMethod("onCreate");
            for (Unit unit : onCreate.getActiveBody().getUnits()) {
                solver.ifdsResultsAt(unit);
            }
        }
    }
}

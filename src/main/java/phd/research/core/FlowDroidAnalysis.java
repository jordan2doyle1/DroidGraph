package phd.research.core;

import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Type;
import phd.research.graph.Filter;
import phd.research.graph.Writer;
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

public class FlowDroidAnalysis {

    public static final File COLLECTED_CALLBACKS_FILE =
            new File(System.getProperty("user.dir") + File.separator + "FlowDroidCallbacks");

    @NotNull
    private final File apk;
    @NotNull
    private final File platformDirectory;
    @NotNull
    private final File outputDirectory;
    @NotNull
    private final ProcessManifest manifest;
    @NotNull
    private final ARSCFileParser resources;

    private SetupApplication application;

    public FlowDroidAnalysis(File apk, File platformDirectory, File outputDirectory)
            throws XmlPullParserException, IOException {
        this.apk = Objects.requireNonNull(apk);
        this.platformDirectory = Objects.requireNonNull(platformDirectory);
        this.outputDirectory = Objects.requireNonNull(outputDirectory);
        this.manifest = new ProcessManifest(apk);
        this.resources = new ARSCFileParser();
        this.resources.parse(this.apk.getAbsolutePath());
    }

    @SuppressWarnings("unused")
    private static String getMethodUnitStructure(String methodSignature) {
        // For Testing Purposes Only. E.g. className: com.example.android.lifecycle.ActivityA, methodName: onCreate
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

    @API
    public String getBasePackageName() {
        return this.manifest.getPackageName();
    }

    @NotNull
    public ARSCFileParser getResources() {
        return this.resources;
    }

    public Set<SootClass> getLaunchActivities() {
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
        if (this.application != null) {
            return this.application.getEntrypointClasses();
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
        LayoutControlFactory controlFactory = new DroidControlFactory();
        controlFactory.setLoadAdditionalAttributes(true);
        MenuFileParser menuParser = new MenuFileParser();
        menuParser.setControlFactory(controlFactory);
        menuParser.parseLayoutFileDirect(this.manifest.getPackageName(), this.apk.getAbsolutePath());
        return menuParser;
    }

    public LayoutFileParser getLayoutFileParser() {
        LayoutControlFactory controlFactory = new DroidControlFactory();
        controlFactory.setLoadAdditionalAttributes(true);
        LayoutFileParser layoutParser = new LayoutFileParser(this.manifest.getPackageName(), this.resources);
        layoutParser.setControlFactory(controlFactory);
        layoutParser.parseLayoutFileDirect(this.apk.getAbsolutePath());
        return layoutParser;
    }

    public void writeAnalysisToFile(File directory) throws IOException {
        Collection<SootClass> filteredClasses = new HashSet<>();
        Collection<SootMethod> allMethods = new HashSet<>(), filteredMethods = new HashSet<>(), standardMethods =
                new HashSet<>(), lifecycleCallbacks = new HashSet<>(), listenerCallbacks = new HashSet<>(),
                possibleCallbacks = new HashSet<>(), otherCallback = new HashSet<>();

        for (SootClass clazz : Scene.v().getClasses()) {
            allMethods.addAll(clazz.getMethods());

            if (Filter.isValidClass(clazz)) {
                filteredClasses.add(clazz);

                List<SootMethod> methods = clazz.getMethods();
                for (SootMethod method : methods) {
                    filteredMethods.add(method);

                    switch (Type.getMethodType(method)) {
                        case LIFECYCLE:
                            lifecycleCallbacks.add(method);
                            break;
                        case LISTENER:
                            if (Filter.isListenerMethod(method)) {
                                listenerCallbacks.add(method);
                            } else {
                                possibleCallbacks.add(method);
                            }
                            break;
                        case CALLBACK:
                            otherCallback.add(method);
                            break;
                        case METHOD:
                            standardMethods.add(method);
                            break;
                        case DUMMY:
                            break;
                    }
                }
            }
        }

        Writer.writeCollection(directory, "all_classes", Scene.v().getClasses());
        Writer.writeCollection(directory, "filtered_classes", filteredClasses);
        Writer.writeCollection(directory, "lifecycle_methods", lifecycleCallbacks);
        Writer.writeCollection(directory, "listener_methods", listenerCallbacks);
        Writer.writeCollection(directory, "possible_callbacks", possibleCallbacks);
        Writer.writeCollection(directory, "other_callbacks", otherCallback);
        Writer.writeCollection(directory, "standardMethods", standardMethods);
        Writer.writeCollection(directory, "all_methods", allMethods);
        Writer.writeCollection(directory, "filtered_methods", filteredMethods);
        Writer.writeCollection(directory, "entry_points", this.getEntryPointClasses());
        Writer.writeCollection(directory, "launch_activities", this.getLaunchActivities());
    }

    @API
    public void runFlowDroid() {
        this.initializeSoot();
        InfoflowAndroidConfiguration configuration = this.getFlowDroidConfiguration();

        this.application = new SetupApplication(configuration);
        this.application.constructCallgraph();
    }

    // TODO: Remove duplicate setting between initializeSoot and getFlowDroidConfiguration, does it still work?
    private void initializeSoot() {
        G.reset();

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(soot.options.Options.output_format_none);
        Options.v().set_whole_program(true);
        Options.v().set_process_dir(Collections.singletonList(this.apk.getAbsolutePath()));
        Options.v().set_android_jars(this.platformDirectory.getAbsolutePath());
        Options.v().set_src_prec(soot.options.Options.src_prec_apk_class_jimple);
        Options.v().set_keep_offset(false);
        Options.v().set_keep_line_number(false);
        Options.v().set_throw_analysis(soot.options.Options.throw_analysis_dalvik);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_ignore_resolution_errors(true);
        Options.v().set_output_dir(outputDirectory.getAbsolutePath());

        List<String> excludeList = new LinkedList<>(
                Arrays.asList("java.*", "javax.*", "sun.*", "org.apache.*", "org.eclipse.*", "soot.*"));
        Options.v().set_exclude(excludeList);

        Options.v().set_soot_classpath(
                Scene.v().getAndroidJarPath(this.platformDirectory.getAbsolutePath(), this.apk.getAbsolutePath()));
        Main.v().autoSetOptions();

        Scene.v().addBasicClass("android.view.View", soot.SootClass.BODIES);
        Scene.v().loadNecessaryClasses();

        // Allows you to add pre-processors that run before call-graph construction. Only enabled in whole-program mode.
        PackManager.v().getPack("wjpp").apply();
        //        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifds", new FlowDroidAnalysis
        //        .IFDSDataFlowTransformer()));

        // Patch the call graph to support additional edges. We do this now, because during callback discovery, the
        // context-insensitive call graph algorithm would flood us with invalid edges.
        LibraryClassPatcher patcher = new LibraryClassPatcher();
        patcher.patchLibraries();
        //        PackManager.v().runPacks();
    }

    private InfoflowAndroidConfiguration getFlowDroidConfiguration() {
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setMergeDexFiles(true);
        config.getAnalysisFileConfig().setAndroidPlatformDir(this.platformDirectory.getAbsolutePath());
        String sourceAndSinkFileName = System.getProperty("user.dir") + File.separator + "SourcesAndSinks.txt";
        config.getAnalysisFileConfig().setSourceSinkFile(sourceAndSinkFileName);
        config.getAnalysisFileConfig().setTargetAPKFile(this.apk.getAbsolutePath());
        config.getCallbackConfig().setSerializeCallbacks(true);
        String flowDroidCallbacksFileName = System.getProperty("user.dir") + File.separator + "FlowDroidCallbacks";
        config.getCallbackConfig().setCallbacksFile(flowDroidCallbacksFileName);
        return config;
    }

    // TODO: Finish Testing IFDS Solver Analysis.

    //    public static class IFDSDataFlowTransformer extends SceneTransformer {
    //        @Override
    //        protected void internalTransform(String phaseName, Map<String, String> options) {
    //            JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();
    //            IFDSTabulationProblem<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod, InterproceduralCFG<Unit,
    //                    SootMethod>>
    //                    problem = new IFDSReachingDefinitions(icfg);
    //            //IFDSReachingDefinitions problem = new IFDSReachingDefinitions(icfg);
    //            IFDSSolver<Unit, Pair<Value, Set<DefinitionStmt>>, SootMethod, InterproceduralCFG<Unit, SootMethod>>
    //                    solver = new IFDSSolver<>(problem);
    //
    //            System.out.println("Starting solver");
    //            solver.solve();
    //            //            SootClass clazz = Scene.v().getSootClassUnsafe("com.example.activity.lifecycle
    //            .ActivityA");
    //            //            SootMethod onCreate = clazz.getMethod("onCreate");
    //            //            for (Unit unit : onCreate.getActiveBody().getUnits()) {
    //            //                solver.ifdsResultsAt(unit);
    //            //            }
    //            System.out.println("Done");
    //        }
    //    }
}

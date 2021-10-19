package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.graph.Composition;
import phd.research.jGraph.JGraph;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentViewer {

    private static final Logger logger = LoggerFactory.getLogger(ContentViewer.class);

    private final ApplicationAnalysis analysis;
    private final ContentFilter filter;

    private final Set<String> allPackages;
    private final Set<String> filteredPackages;
    private final Set<SootClass> allClasses;
    private final Set<SootClass> filteredClasses;
    private final Set<SootClass> entryPoints;
    private final Set<SootClass> launchActivities;
    private final Set<SootMethod> allMethods;
    private final Set<SootMethod> filteredMethods;

    public ContentViewer(ApplicationAnalysis analysis) {
        this.analysis = analysis;
        this.filter = this.analysis.getContentFilter();
        this.allPackages = retrieveAllPackages();
        this.filteredPackages = filterPackages();
        this.allClasses = retrieveAllClasses();
        this.filteredClasses = filterClasses(filter);
        this.entryPoints = this.analysis.getEntryPointClasses();
        this.launchActivities = this.analysis.getLaunchActivities();
        this.allMethods = retrieveAllMethods();
        this.filteredMethods = filterMethods();
    }

    protected void printAppDetails() {
        System.out.println("-------------------------------- Analysis Details ---------------------------------\n");

        System.out.println("Base Package Name: " + this.analysis.getBasePackageName());
        System.out.println("Packages: " + this.filteredPackages.size() + " (Total: " + this.allPackages.size() + ")");
        System.out.println();

        System.out.println("Classes: " + this.filteredClasses.size() + " (Total: " + this.allClasses.size() + ")");
        System.out.println("Entry Points: " + this.entryPoints.size());
        System.out.println("Launch Activities: " + this.launchActivities.size());
        System.out.println();

        System.out.println("Methods: " + this.filteredMethods.size() + " (Total: " + this.allMethods.size() + ")");
        System.out.println("Lifecycle Methods: " + this.interfaceManager.lifecycleCount());
        System.out.println("System Callbacks: " + this.interfaceManager.callbackCount());
        System.out.println("Callback Methods: " + this.interfaceManager.listenerCount());
        System.out.println();

        System.out.println("Callback ID's: " + this.interfaceManager.controlCount());
        System.out.println();

        System.out.println("Interface Callback Table");
        System.out.println(this.interfaceManager.getControlListenerTable());

        System.out.println("\n-----------------------------------------------------------------------------------\n");
    }

    protected void printCallGraphDetails() {
        System.out.println("Call Graph Composition Table");
        Composition callGraphComposition = new Composition(this.analysis.getCallGraph());
        System.out.println(callGraphComposition);

        JGraph jCallGraph = this.analysis.getJCallGraph();
        Composition jCallGraphComposition = new Composition(jCallGraph);

        if (!callGraphComposition.equals(jCallGraphComposition))
            System.out.println(jCallGraphComposition);
    }

    protected void printCFGDetails() {
        System.out.println("Control Flow Graph Composition Table");
        Composition controlFlowGraphComposition = new Composition(this.analysis.getControlFlowGraph());
        System.out.println(controlFlowGraphComposition);

        JGraph jControlFlowGraph = this.analysis.getJControlFlowGraph();
        Composition jControlFlowGraphComposition = new Composition(jControlFlowGraph);

        if (!controlFlowGraphComposition.equals(jControlFlowGraphComposition))
            System.out.println(jControlFlowGraphComposition);
    }

    private Set<SootMethod> retrieveAllMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> allMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            List<SootMethod> methods = sootClass.getMethods();
            allMethods.addAll(methods);
        }

        return allMethods;
    }

    private Set<SootMethod> filterMethods(ContentFilter filter) {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> acceptedMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            if (filter.isValidClass(sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                acceptedMethods.addAll(methods);
            }
        }

        return acceptedMethods;
    }

    private Set<SootClass> retrieveAllClasses() {
        return new HashSet<>(Scene.v().getClasses());
    }

    private Set<SootClass> filterClasses(ContentFilter filter) {
        Set<SootClass> classes = new HashSet<>();

        for (SootClass sootClass : this.allClasses) {
            if (filter.isValidPackage(sootClass.getPackageName())) {
                if (filter.isValidClass(sootClass)) {
                    classes.add(sootClass);
                }
            }
        }

        return classes;
    }

    private Set<String> retrieveAllPackages() {
        Set<String> packages = new HashSet<>();
        Chain<SootClass> classes = Scene.v().getClasses();

        for (SootClass sootClass : classes) {
            String packageName = sootClass.getPackageName();
            if (!packageName.equals("")) {
                packages.add(packageName);
            }
        }

        return packages;
    }

    private Set<String> filterPackages() {
        Set<String> packages = new HashSet<>();

        if (!this.allPackages.isEmpty()) {
            for (String currentPackage : this.allPackages) {
                if (filter.isValidPackage(currentPackage)) {
                    packages.add(currentPackage);
                }
            }
        }

        return packages;
    }
}
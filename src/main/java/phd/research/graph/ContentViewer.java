package phd.research.graph;

import phd.research.core.ApplicationAnalysis;
import phd.research.enums.Parts;
import phd.research.helper.Control;
import phd.research.jGraph.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentViewer {

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
    private final Set<SootMethod> lifecycleMethods;
    private final Set<SootMethod> listenerMethods;
    private final Set<SootMethod> callbackMethods;

    public ContentViewer(ApplicationAnalysis analysis) {
        this.analysis = analysis;
        this.filter = this.analysis.getFilter();

        this.allPackages = retrieveAllPackages();
        this.filteredPackages = filterPackages();
        this.allClasses = retrieveAllClasses();
        this.filteredClasses = filterClasses(filter);
        this.entryPoints = this.analysis.getEntryPointClasses();
        this.launchActivities = this.analysis.getLaunchActivities();
        this.allMethods = retrieveAllMethods();
        this.filteredMethods = filterMethods();
        this.callbackMethods = filterCallbackMethods();
        this.lifecycleMethods = filterLifecycleMethods();
        this.listenerMethods = filterListenerMethods();
    }

    public void printAppDetails() {
        System.out.println("------------------------------- Analysis Details -------------------------------");

        System.out.println("Base Package Name: " + this.analysis.getBasePackageName());
        System.out.println("Packages: " + this.filteredPackages.size() + " (Total: " + this.allPackages.size() + ")");
        System.out.println();

        System.out.println("Classes: " + this.filteredClasses.size() + " (Total: " + this.allClasses.size() + ")");
        System.out.println("Entry Points: " + this.entryPoints.size());
        System.out.println("Launch Activities: " + this.launchActivities.size());
        System.out.println();

        System.out.println("Methods: " + this.filteredMethods.size() + " (Total: " + this.allMethods.size() + ")");
        System.out.println("Lifecycle Methods: " + this.lifecycleMethods.size());
        System.out.println("System Callbacks: " + this.callbackMethods.size());
        System.out.println("Callback Methods: " + this.listenerMethods.size());
        System.out.println();

        System.out.println("Controls: " + this.analysis.getControls().size());

        System.out.println("--------------------------------------------------------------------------------");
    }

    @SuppressWarnings("unused")
    public void printCallbackTable() {
        String separator = "--------------------------------------------------------------------------------\n";
        String stringFormat = "\t%-15s\t%-30s\t%-30s\t%-30s\n";

        System.out.println("----------------------------- Control Callback Map -----------------------------");
        if (this.analysis.getControls().isEmpty()) {
            System.out.println("Control Callback Map is Empty!");
        } else {
            System.out.printf((stringFormat) + "%n", "WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD");
            System.out.println(separator);

            for (Control control : this.analysis.getControls()) {
                Integer interfaceID = control.getId();
                String textID = control.getTextId();
                SootMethod listener = control.getClickListener();

                if (listener != null) {
                    System.out.printf((stringFormat) + "%n", interfaceID, textID,
                            listener.getDeclaringClass().getShortName(), listener.getName());
                } else {
                    System.out.printf((stringFormat) + "%n", interfaceID, null, null, null);
                }
            }
        }
        System.out.println(separator);
    }

    @SuppressWarnings("unused")
    public void printCallGraphDetails() {
        GraphComposition callGraphComposition = new GraphComposition(this.analysis.getCallGraph());
        System.out.println("Call Graph Composition: " + callGraphComposition);
    }

    public void printCFGDetails() {
        GraphComposition controlFlowGraphComposition = new GraphComposition(this.analysis.getControlFlowGraph());
        System.out.println("CFG Composition: " + controlFlowGraphComposition);
    }

    @SuppressWarnings("unused")
    public void printList(boolean filtered, Parts part) {
        switch(part) {
            case methods:
                if (filtered)
                    printList(this.filteredMethods);
                else
                    printList(this.allMethods);
                break;
            case classes:
                if (filtered)
                    printList(this.filteredClasses);
                else
                    printList(this.allClasses);
                break;
            case packages:
                if (filtered)
                    printList(this.filteredPackages);
                else
                    printList(this.allPackages);
                break;
        }
    }

    public void writeContentsToFile() {
        GraphWriter writer = new GraphWriter();
        writer.writeContent("All_Packages", this.allPackages);
        writer.writeContent("Filtered_Packages", this.filteredPackages);
        writer.writeContent("All_Classes", this.allClasses);
        writer.writeContent("Filtered_Classes", this.filteredClasses);
        writer.writeContent("All_Methods", this.allMethods);
        writer.writeContent("Filtered_Methods", this.filteredMethods);
        writer.writeContent("Lifecycle_Methods", this.lifecycleMethods);
        writer.writeContent("Listener_Methods", this.listenerMethods);
        writer.writeContent("Other_Callbacks", this.callbackMethods);
    }

    private Set<SootMethod> filterLifecycleMethods() {
        Set<SootMethod> lifecycleMethods = new HashSet<>();

        for (SootMethod method : this.filteredMethods) {
            if (filter.isLifecycleMethod(method)) {
                lifecycleMethods.add(method);
            }
        }

        return lifecycleMethods;
    }

    private Set<SootMethod> filterCallbackMethods() {
        Set<SootMethod> callbackMethods = new HashSet<>();

        for (SootMethod method : this.filteredMethods) {
            if (filter.isCallbackMethod(method)) {
                callbackMethods.add(method);
            }
        }

        return callbackMethods;
    }

    private Set<SootMethod> filterListenerMethods() {
        Set<SootMethod> listenerMethods = new HashSet<>();

        for (SootMethod method : this.filteredMethods) {
            if (filter.isListenerMethod(method)) {
                listenerMethods.add(method);
            }
        }

        return listenerMethods;
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

    private Set<SootMethod> filterMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> acceptedMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            if (this.filter.isValidClass(sootClass)) {
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

    private static void printList(Set<?> list) {
        int counter = 0;
        int numberOfPrints = 10;
        for (Object item : list) {
            if (counter < numberOfPrints) {
                if (item instanceof String)
                    System.out.println(item);
                else if (item instanceof SootClass)
                    System.out.println(((SootClass) item).getName());
                else if (item instanceof Vertex)
                    System.out.println(((Vertex) item).getLabel());
                else if (item instanceof SootMethod) {
                    SootMethod method = (SootMethod) item;
                    System.out.println(method.getDeclaringClass().getName() + ":" + method.getName());
                }
            } else {
                int remaining = list.size() - numberOfPrints;
                System.out.println("+ " + remaining + " more!");
                break;
            }
            counter++;
        }
        System.out.println();
    }
}
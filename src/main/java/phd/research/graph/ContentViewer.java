package phd.research.graph;

import phd.research.core.ApplicationAnalysis;
import phd.research.enums.Parts;
import phd.research.helper.Control;
import phd.research.jGraph.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContentViewer {

    private final ApplicationAnalysis analysis;
    private final ContentFilter filter;

    private Set<String> allPackages;
    private Set<String> filteredPackages;
    private Set<SootClass> allClasses;
    private Set<SootClass> filteredClasses;
    private Set<SootMethod> allMethods;
    private Set<SootMethod> filteredMethods;
    private Set<SootMethod> lifecycleMethods;
    private Set<SootMethod> listenerMethods;
    private Set<SootMethod> callbackMethods;

    public ContentViewer(ApplicationAnalysis analysis) {
        this.analysis = analysis;
        this.filter = this.analysis.getFilter();
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

    public Set<String> getAllPackages() {
        if (this.allPackages == null) {
            this.allPackages = retrieveAllPackages();
        }
        return this.allPackages;
    }

    public Set<String> getFilteredPackages() {
        if (this.filteredPackages == null) {
            this.filteredPackages = filterPackages();
        }
        return this.filteredPackages;
    }

    public Set<SootClass> getAllClasses() {
        if (this.allClasses == null) {
            this.allClasses = retrieveAllClasses();
        }
        return this.allClasses;
    }

    public Set<SootClass> getFilteredClasses() {
        if (this.filteredClasses == null) {
            this.filteredClasses = filterClasses();
        }
        return this.filteredClasses;
    }

    public Set<SootMethod> getAllMethods() {
        if (this.allMethods == null) {
            this.allMethods = retrieveAllMethods();
        }
        return this.allMethods;
    }

    public Set<SootMethod> getFilteredMethods() {
        if (this.filteredMethods == null) {
            this.filteredMethods = filterMethods();
        }
        return this.filteredMethods;
    }

    public Set<SootMethod> getLifecycleMethods() {
        if (this.lifecycleMethods == null) {
            this.lifecycleMethods = filterLifecycleMethods();
        }
        return this.lifecycleMethods;
    }

    public Set<SootMethod> getListenerMethods() {
        if (this.listenerMethods == null) {
            this.listenerMethods = filterListenerMethods();
        }
        return this.listenerMethods;
    }

    public Set<SootMethod> getCallbackMethods() {
        if (this.callbackMethods == null) {
            this.callbackMethods = filterCallbackMethods();
        }
        return this.callbackMethods;
    }

    public void printAppDetails() {
        System.out.println("------------------------------- Analysis Details -------------------------------");

        System.out.println("Base Package Name: " + this.analysis.getBasePackageName());
        System.out.println("Packages: " + getFilteredPackages().size() + " (Total: " + getAllPackages().size() + ")");
        System.out.println();

        System.out.println("Classes: " + getFilteredClasses().size() + " (Total: " + getAllClasses().size() + ")");
        System.out.println("Entry Points: " + this.analysis.getEntryPointClasses().size());
        System.out.println("Launch Activities: " + this.analysis.getLaunchActivities().size());
        System.out.println();

        System.out.println("Methods: " + getFilteredMethods().size() + " (Total: " + getAllMethods().size() + ")");
        System.out.println("Lifecycle Methods: " + getLifecycleMethods().size());
        System.out.println("Listener Methods: " + getListenerMethods().size());
        System.out.println("Other Callbacks: " + getCallbackMethods().size());
        System.out.println();

        System.out.println("Controls: " + this.analysis.getControls().size());

        System.out.println("--------------------------------------------------------------------------------");
    }

    @SuppressWarnings("unused")
    public void writeContentsToFile() throws IOException {
        GraphWriter writer = new GraphWriter();
        writer.writeContent("packages", getAllPackages());
        writer.writeContent("filtered_packages", getFilteredPackages());
        writer.writeContent("classes", getAllClasses());
        writer.writeContent("filtered_classes", getFilteredClasses());
        writer.writeContent("methods", getAllMethods());
        writer.writeContent("filtered_methods", getFilteredMethods());
        writer.writeContent("lifecycle", getLifecycleMethods());
        writer.writeContent("listener", getListenerMethods());
        writer.writeContent("callbacks", getCallbackMethods());
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

    @SuppressWarnings("unused")
    public void printCFGDetails() {
        GraphComposition controlFlowGraphComposition = new GraphComposition(this.analysis.getControlFlowGraph());
        System.out.println("CFG Composition: " + controlFlowGraphComposition);
    }

    @SuppressWarnings("unused")
    public void printList(boolean filtered, Parts part) {
        switch (part) {
            case methods:
                if (filtered)
                    printList(getFilteredMethods());
                else
                    printList(getAllMethods());
                break;
            case classes:
                if (filtered)
                    printList(getFilteredClasses());
                else
                    printList(getAllClasses());
                break;
            case packages:
                if (filtered)
                    printList(getFilteredPackages());
                else
                    printList(getAllPackages());
                break;
        }
    }

    private Set<SootMethod> filterCallbackMethods() {
        Set<SootMethod> callbackMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (this.filter.isCallbackMethod(method)) {
                callbackMethods.add(method);
            }
        }

        this.callbackMethods = callbackMethods;
        return callbackMethods;
    }

    private Set<SootMethod> filterLifecycleMethods() {
        Set<SootMethod> lifecycleMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (this.filter.isLifecycleMethod(method)) {
                lifecycleMethods.add(method);
            }
        }

        this.lifecycleMethods = lifecycleMethods;
        return lifecycleMethods;
    }

    private Set<SootMethod> filterListenerMethods() {
        Set<SootMethod> listenerMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (this.filter.isListenerMethod(method)) {
                listenerMethods.add(method);
            }
        }

        this.listenerMethods = listenerMethods;
        return listenerMethods;
    }

    private Set<SootMethod> retrieveAllMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> allMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            List<SootMethod> methods = sootClass.getMethods();
            allMethods.addAll(methods);
        }

        this.allMethods = allMethods;
        return allMethods;
    }

    private Set<SootMethod> filterMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> acceptedMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            if (this.filter.isValidClass(sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    if (this.filter.isValidMethod(method)) {
                        acceptedMethods.add(method);
                    }
                }
            }
        }

        this.filteredMethods = acceptedMethods;
        return acceptedMethods;
    }

    private Set<SootClass> retrieveAllClasses() {
        Set<SootClass> classes = new HashSet<>(Scene.v().getClasses());

        this.allClasses = classes;
        return classes;
    }

    private Set<SootClass> filterClasses() {
        Chain<SootClass> allClasses = Scene.v().getClasses();
        Set<SootClass> filteredClasses = new HashSet<>();

        for (SootClass sootClass : allClasses) {
            if (this.filter.isValidPackage(sootClass.getPackageName())) {
                if (this.filter.isValidClass(sootClass)) {
                    filteredClasses.add(sootClass);
                }
            }
        }

        this.filteredClasses = filteredClasses;
        return filteredClasses;
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

        this.allPackages = packages;
        return packages;
    }

    private Set<String> filterPackages() {
        Set<String> packages = new HashSet<>();

        if (this.allPackages == null) {
            this.allPackages = retrieveAllPackages();
        }

        if (!this.allPackages.isEmpty()) {
            for (String currentPackage : this.allPackages) {
                if (this.filter.isValidPackage(currentPackage)) {
                    packages.add(currentPackage);
                }
            }
        }

        this.filteredPackages = packages;
        return packages;
    }
}
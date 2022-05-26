package phd.research.graph;

import phd.research.core.DroidGraph;
import phd.research.core.FlowDroidUtils;
import phd.research.core.FrameworkMain;
import phd.research.enums.Parts;
import phd.research.helper.Control;
import phd.research.jGraph.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class Viewer {

    private final DroidGraph analysis;

    private Set<SootClass> allClasses;
    private Set<SootClass> filteredClasses;
    private Set<SootMethod> allMethods;
    private Set<SootMethod> filteredMethods;
    private Set<SootMethod> lifecycleMethods;
    private Set<SootMethod> listenerMethods;
    private Set<SootMethod> otherCallbackMethods;
    private Set<SootMethod> possibleCallbackMethods;

    public Viewer(DroidGraph analysis) {
        this.analysis = analysis;
    }

    public Set<SootClass> getAllClasses() {
        if (this.allClasses == null)
            this.allClasses = Viewer.retrieveAllClasses();

        return this.allClasses;
    }

    public Set<SootClass> getFilteredClasses() {
        if (this.filteredClasses == null)
            this.filteredClasses = Viewer.filterClasses();

        return this.filteredClasses;
    }

    public Set<SootMethod> getAllMethods() {
        if (this.allMethods == null)
            this.allMethods = Viewer.retrieveAllMethods();

        return this.allMethods;
    }

    public Set<SootMethod> getFilteredMethods() {
        if (this.filteredMethods == null)
            this.filteredMethods = Viewer.filterMethods();

        return this.filteredMethods;
    }

    public Set<SootMethod> getLifecycleMethods() {
        if (this.lifecycleMethods == null)
            this.lifecycleMethods = filterLifecycleMethods();

        return this.lifecycleMethods;
    }

    public Set<SootMethod> getListenerMethods() {
        if (this.listenerMethods == null)
            this.listenerMethods = filterListenerMethods();

        return this.listenerMethods;
    }

    public Set<SootMethod> getOtherCallbackMethods() {
        if (this.otherCallbackMethods == null)
            this.otherCallbackMethods = filterOtherCallbackMethods();

        return this.otherCallbackMethods;
    }

    public Set<SootMethod> getPossibleCallbacksMethods() {
        if (this.possibleCallbackMethods == null)
            this.possibleCallbackMethods = filterPossibleCallbackMethods();

        return this.possibleCallbackMethods;
    }

    public void printAppDetails() {
        System.out.println("------------------------------- Analysis Details -------------------------------");

        System.out.println("\tBase Package Name: " + FlowDroidUtils.getBasePackageName(FrameworkMain.getApk()));
        System.out.println();

        System.out.println("\tClasses: " + getFilteredClasses().size() + " (Total: " + getAllClasses().size() + ")");
        System.out.println("\tEntry Points: " + FlowDroidUtils.getEntryPointClasses(FrameworkMain.getApk()).size());
        System.out.println("\tLaunch Activities: " + FlowDroidUtils.getLaunchActivities(FrameworkMain.getApk()).size());
        System.out.println();

        System.out.println("\tMethods: " + getFilteredMethods().size() + " (Total: " + getAllMethods().size() + ")");
        System.out.println("\tLifecycle Methods: " + getLifecycleMethods().size());
        System.out.println("\tListener Methods: " + getListenerMethods().size());
        System.out.println("\tOther Callbacks: " + getOtherCallbackMethods().size());
        System.out.println("\tPossible Callbacks: " + getPossibleCallbacksMethods().size());
        System.out.println();

        System.out.println("\tControls: " + this.analysis.getControls().size());

        System.out.println("--------------------------------------------------------------------------------");
    }

    public void writeContentsToFile() throws IOException {
        Writer.writeContent(FrameworkMain.getOutputDirectory(), "classes", getFilteredClasses());
        Writer.writeContent(FrameworkMain.getOutputDirectory(), "methods", getFilteredMethods());
        Writer.writeContent(FrameworkMain.getOutputDirectory(), "lifecycle", getLifecycleMethods());
        Writer.writeContent(FrameworkMain.getOutputDirectory(), "listener", getListenerMethods());
        Writer.writeContent(FrameworkMain.getOutputDirectory(), "other", getOtherCallbackMethods());
        Writer.writeContent(FrameworkMain.getOutputDirectory(), "possible", getPossibleCallbacksMethods());
    }

    public void printUnassignedCallbacks() {
        String separator = "--------------------------------------------------------------------------------";
        String stringFormat = "\t%-35s\t%-20s\t%-10s\n";

        System.out.println("----------------------------- Unassigned Callbacks -----------------------------");
        System.out.printf((stringFormat), "LISTENER CLASS", "LISTENER METHOD", "POSSIBLE");
        System.out.println(separator);

        for (SootMethod method : this.getListenerMethods()) {
            if (!findCallbackControl(method)) {
                System.out.printf((stringFormat), method.getDeclaringClass().getShortName(), method.getName(), false);
            }
        }

        for (SootMethod method : this.getPossibleCallbacksMethods()) {
            if (!findCallbackControl(method)) {
                System.out.printf((stringFormat), method.getDeclaringClass().getShortName(), method.getName(), true);
            }
        }

        System.out.println(separator);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean findCallbackControl(SootMethod method) {
        for (Control control : this.analysis.getControls()) {
            SootMethod controlClickListener = control.getClickListener();
            if (controlClickListener != null && controlClickListener.equals(method))
                return true;
        }

        return false;
    }

    public void printCallbackTable() {
        String separator = "--------------------------------------------------------------------------------";
        String stringFormat = "\t%-15s\t%-35s\t%-15s\t%-15s\n";

        System.out.println("----------------------------- Control Callback Map -----------------------------");
        if (this.analysis.getControls().isEmpty())
            System.out.println("Control Callback Map is Empty!");
        else {
            System.out.printf((stringFormat), "WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD");
            System.out.println(separator);

            for (Control control : this.analysis.getControls()) {
                SootMethod listener = control.getClickListener();

                if (listener != null)
                    System.out.printf((stringFormat), control.getControlResource().getResourceID(),
                            control.getControlResource().getResourceName(),
                            listener.getDeclaringClass().getShortName(), listener.getName());
                else
                    System.out.printf((stringFormat), control.getControlResource().getResourceID(),
                            control.getControlResource().getResourceName(), null, null);
            }
        }
        System.out.println(separator);
    }

    public void printCallGraphDetails() {
        Composition callGraphComposition = new Composition(this.analysis.getCallGraph());
        System.out.println(callGraphComposition.toTableString("Call Graph Composition"));
    }

    public void printCFGDetails() {
        Composition controlFlowGraphComposition = new Composition(this.analysis.getControlFlowGraph());
        System.out.println(controlFlowGraphComposition.toTableString("CFG Composition"));
    }

    @SuppressWarnings("unused")
    public void printList(boolean filtered, Parts part) {
        switch (part) {
            case methods:
                if (filtered)
                    Viewer.printList(getFilteredMethods());
                else
                    Viewer.printList(getAllMethods());
                break;
            case classes:
                if (filtered)
                    Viewer.printList(getFilteredClasses());
                else
                    Viewer.printList(getAllClasses());
                break;
        }
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

    private static Set<SootMethod> retrieveAllMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> allMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            List<SootMethod> methods = sootClass.getMethods();
            allMethods.addAll(methods);
        }

        return allMethods;
    }

    private static Set<SootMethod> filterMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> acceptedMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            if (Filter.isValidClass(null, null, sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    if (Filter.isValidMethod(null, null, method))
                        acceptedMethods.add(method);
                }
            }
        }

        return acceptedMethods;
    }

    private static Set<SootClass> retrieveAllClasses() {
        return new HashSet<>(Scene.v().getClasses());
    }

    private static Set<SootClass> filterClasses() {
        Chain<SootClass> allClasses = Scene.v().getClasses();
        Set<SootClass> filteredClasses = new HashSet<>();

        for (SootClass sootClass : allClasses) {
            if (Filter.isValidClass(null, null, sootClass))
                filteredClasses.add(sootClass);
        }

        return filteredClasses;
    }

    private Set<SootMethod> filterLifecycleMethods() {
        Set<SootMethod> lifecycleMethods = new HashSet<>();

        if (this.filteredMethods == null)
            this.filteredMethods = Viewer.filterMethods();

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isLifecycleMethod(method))
                lifecycleMethods.add(method);
        }

        this.lifecycleMethods = lifecycleMethods;
        return lifecycleMethods;
    }

    private Set<SootMethod> filterListenerMethods() {
        Set<SootMethod> listenerMethods = new HashSet<>();

        if (this.filteredMethods == null)
            this.filteredMethods = Viewer.filterMethods();

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isListenerMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
                listenerMethods.add(method);
        }

        this.listenerMethods = listenerMethods;
        return listenerMethods;
    }

    private Set<SootMethod> filterOtherCallbackMethods() {
        Set<SootMethod> callbackMethods = new HashSet<>();

        if (this.filteredMethods == null)
            this.filteredMethods = Viewer.filterMethods();

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isOtherCallbackMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
                callbackMethods.add(method);
        }

        this.otherCallbackMethods = callbackMethods;
        return callbackMethods;
    }

    private Set<SootMethod> filterPossibleCallbackMethods() {
        Set<SootMethod> callbackMethods = new HashSet<>();

        if (this.filteredMethods == null)
            this.filteredMethods = Viewer.filterMethods();

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isPossibleListenerMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
                callbackMethods.add(method);
        }

        this.possibleCallbackMethods = callbackMethods;
        return callbackMethods;
    }
}
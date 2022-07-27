package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FlowDroidUtils;
import phd.research.enums.Parts;
import phd.research.helper.Control;
import phd.research.jGraph.Vertex;
import phd.research.ui.UiControls;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
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

    private static final Logger logger = LoggerFactory.getLogger(Viewer.class);

    private final File collectedCallbacksFile;
    private final UiControls uiControls;

    private Set<SootClass> allClasses;
    private Set<SootClass> filteredClasses;
    private Set<SootMethod> allMethods;
    private Set<SootMethod> filteredMethods;
    private Set<SootMethod> lifecycleMethods;
    private Set<SootMethod> listenerMethods;
    private Set<SootMethod> otherCallbackMethods;
    private Set<SootMethod> possibleCallbackMethods;

    public Viewer(File collectedCallbacksFile, UiControls uiControls) {
        if (!collectedCallbacksFile.exists()) {
            logger.error("Collected Callback File Does Not Exist!: " + collectedCallbacksFile);
        }

        this.collectedCallbacksFile = collectedCallbacksFile;
        this.uiControls = uiControls;
    }

    public static void printCallGraphDetails(Graph<Vertex, DefaultEdge> callGraph) {
        Composition callGraphComposition = new Composition(callGraph);
        System.out.println(callGraphComposition.toTableString("Call Graph Composition"));
    }

    public static void printCFGDetails(Graph<Vertex, DefaultEdge> controlFlowGraph) {
        Composition controlFlowGraphComposition = new Composition(controlFlowGraph);
        System.out.println(controlFlowGraphComposition.toTableString("CFG Composition"));
    }

    private static void printMethodUnitsToConsole(String className, String methodName) {
        // For Testing Purposes Only. E.g. className: com.example.android.lifecycle.ActivityA, methodName: onCreate
        System.out.println("**** Printing method units: " + className + " " + methodName + " ****");
        SootClass sc = Scene.v().getSootClass(className);
        for (SootMethod method : sc.getMethods()) {
            if (method.getName().contains(methodName)) {
                if (method.hasActiveBody()) {
                    for (Unit unit : method.getActiveBody().getUnits()) {
                        System.out.println(unit.toString());
                    }
                }
            }
        }
        System.out.println("**** END ****");
    }

    private static void printList(Set<?> list) {
        int counter = 0;
        int numberOfPrints = 10;
        for (Object item : list) {
            if (counter < numberOfPrints) {
                if (item instanceof String) {
                    System.out.println(item);
                } else if (item instanceof SootClass) {
                    System.out.println(((SootClass) item).getName());
                } else if (item instanceof Vertex) {
                    System.out.println(((Vertex) item).getLabel());
                } else if (item instanceof SootMethod) {
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
                    if (Filter.isValidMethod(null, null, method)) {
                        acceptedMethods.add(method);
                    }
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
            if (Filter.isValidClass(null, null, sootClass)) {
                filteredClasses.add(sootClass);
            }
        }

        return filteredClasses;
    }

    public Set<SootClass> getAllClasses() {
        if (this.allClasses == null) {
            this.allClasses = Viewer.retrieveAllClasses();
        }

        return this.allClasses;
    }

    public Set<SootClass> getFilteredClasses() {
        if (this.filteredClasses == null) {
            this.filteredClasses = Viewer.filterClasses();
        }

        return this.filteredClasses;
    }

    public Set<SootMethod> getAllMethods() {
        if (this.allMethods == null) {
            this.allMethods = Viewer.retrieveAllMethods();
        }

        return this.allMethods;
    }

    public Set<SootMethod> getFilteredMethods() {
        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
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
            this.listenerMethods = filterListenerMethods(this.collectedCallbacksFile);
        }

        return this.listenerMethods;
    }

    public Set<SootMethod> getOtherCallbackMethods() {
        if (this.otherCallbackMethods == null) {
            this.otherCallbackMethods = filterOtherCallbackMethods(this.collectedCallbacksFile);
        }

        return this.otherCallbackMethods;
    }

    public Set<SootMethod> getPossibleCallbacksMethods() {
        if (this.possibleCallbackMethods == null) {
            this.possibleCallbackMethods = filterPossibleCallbackMethods(this.collectedCallbacksFile);
        }

        return this.possibleCallbackMethods;
    }

    public void writeContentsToFile(String outputDirectory) throws IOException {
        Writer.writeContent(outputDirectory, "classes", this.getFilteredClasses());
        Writer.writeContent(outputDirectory, "methods", this.getFilteredMethods());
        Writer.writeContent(outputDirectory, "lifecycle", this.getLifecycleMethods());
        Writer.writeContent(outputDirectory, "listener", this.getListenerMethods());
        Writer.writeContent(outputDirectory, "other", this.getOtherCallbackMethods());
        Writer.writeContent(outputDirectory, "possible", this.getPossibleCallbacksMethods());
        Writer.writeContent(outputDirectory, "controls", this.uiControls.getControls());
    }

    public void printUnassignedCallbacks() {
        String separator = "--------------------------------------------------------------------------------";
        String stringFormat = "\t%-35s\t%-20s\t%-10s\n";

        System.out.println("----------------------------- Unassigned Callbacks -----------------------------");
        System.out.printf((stringFormat), "LISTENER CLASS", "LISTENER METHOD", "POSSIBLE");
        System.out.println(separator);

        for (SootMethod method : this.getListenerMethods()) {
            if (!this.uiControls.hasControl(method)) {
                System.out.printf((stringFormat), method.getDeclaringClass().getShortName(), method.getName(), false);
            }
        }

        for (SootMethod method : this.getPossibleCallbacksMethods()) {
            if (!this.uiControls.hasControl(method)) {
                System.out.printf((stringFormat), method.getDeclaringClass().getShortName(), method.getName(), true);
            }
        }

        System.out.println(separator);
    }

    public void printList(boolean filtered, Parts part) {
        switch (part) {
            case methods:
                if (filtered) {
                    Viewer.printList(this.getFilteredMethods());
                } else {
                    Viewer.printList(this.getAllMethods());
                }
                break;
            case classes:
                if (filtered) {
                    Viewer.printList(this.getFilteredClasses());
                } else {
                    Viewer.printList(this.getAllClasses());
                }
                break;
        }
    }

    public void printAppDetails(File apk) {
        logger.info("Base package name: " + FlowDroidUtils.getBasePackageName(apk));
        logger.info("Number of entry point classes: " + FlowDroidUtils.getEntryPointClasses(apk).size());
        logger.info("Number of launch activities: " + FlowDroidUtils.getLaunchActivities(apk).size());
        logger.info("Number of classes: " + this.getFilteredClasses().size() + " (Total: " + this.getAllClasses().size() + ")");
        logger.info("Number of methods: " + this.getFilteredMethods().size() + " (Total: " + this.getAllMethods().size() + ")");
        logger.info("Number of lifecycle methods: " + this.getLifecycleMethods().size());
        logger.info("Number of listener methods: " + this.getListenerMethods().size());
        logger.info("Number of other callbacks: " + this.getOtherCallbackMethods().size());
        logger.info("Number of possible callbacks: " + this.getPossibleCallbacksMethods().size());
        logger.info("Number of UI controls: " + this.uiControls.getControls().size());
    }

    public void printCallbackTable() {
        String separator = "--------------------------------------------------------------------------------";
        String stringFormat = "\t%-15s\t%-35s\t%-15s\t%-15s\n";

        System.out.println("----------------------------- Control Callback Map -----------------------------");
        if (this.uiControls.getControls().isEmpty()) {
            System.out.println("Control Callback Map is Empty!");
        } else {
            System.out.printf((stringFormat), "WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD");
            System.out.println(separator);

            for (Control control : this.uiControls.getControls()) {
                SootMethod listener = control.getClickListener();

                if (listener != null) {
                    System.out.printf((stringFormat), control.getControlResource().getResourceID(),
                            control.getControlResource().getResourceName(), listener.getDeclaringClass().getShortName(),
                            listener.getName()
                                     );
                } else {
                    System.out.printf((stringFormat), control.getControlResource().getResourceID(),
                            control.getControlResource().getResourceName(), null, null
                                     );
                }
            }
        }
        System.out.println(separator);
    }

    private Set<SootMethod> filterLifecycleMethods() {
        Set<SootMethod> lifecycleMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isLifecycleMethod(method)) {
                lifecycleMethods.add(method);
            }
        }

        this.lifecycleMethods = lifecycleMethods;
        return lifecycleMethods;
    }

    private Set<SootMethod> filterListenerMethods(File callbacksFile) {
        Set<SootMethod> listenerMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isListenerMethod(callbacksFile, method)) {
                listenerMethods.add(method);
            }
        }

        this.listenerMethods = listenerMethods;
        return listenerMethods;
    }

    private Set<SootMethod> filterOtherCallbackMethods(File callbacksFile) {
        Set<SootMethod> callbackMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isOtherCallbackMethod(callbacksFile, method)) {
                callbackMethods.add(method);
            }
        }

        this.otherCallbackMethods = callbackMethods;
        return callbackMethods;
    }

    private Set<SootMethod> filterPossibleCallbackMethods(File callbacksFile) {
        Set<SootMethod> callbackMethods = new HashSet<>();

        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        for (SootMethod method : this.filteredMethods) {
            if (Filter.isPossibleListenerMethod(callbacksFile, method)) {
                callbackMethods.add(method);
            }
        }

        this.possibleCallbackMethods = callbackMethods;
        return callbackMethods;
    }
}
package phd.research.graph;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.core.FlowDroidUtils;
import phd.research.core.UiControls;
import phd.research.helper.StringTable;
import phd.research.vertices.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.util.Chain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */

public class Viewer {

    private static final Logger logger = LoggerFactory.getLogger(Viewer.class);

    @NotNull
    private final File collectedCallbacksFile;
    @NotNull
    private final UiControls uiControls;

    private Collection<SootClass> allClasses;
    private Collection<SootClass> filteredClasses;
    private Collection<SootMethod> allMethods;
    private Collection<SootMethod> filteredMethods;
    private Collection<SootMethod> lifecycleMethods;
    private Collection<SootMethod> listenerMethods;
    private Collection<SootMethod> otherCallbackMethods;
    private Collection<SootMethod> possibleListenerMethods;

    public Viewer(File collectedCallbacksFile, UiControls uiControls) {
        this.collectedCallbacksFile = Objects.requireNonNull(collectedCallbacksFile);
        this.uiControls = Objects.requireNonNull(uiControls);
    }

    public static void outputCGDetails(File directory, Graph<Vertex, DefaultEdge> graph) throws IOException {
        Writer.writeString(directory, "CG_Composition", new Composition(graph).toTableString());
    }

    public static void outputCFGDetails(File directory, Graph<Vertex, DefaultEdge> graph) throws IOException {
        Writer.writeString(directory, "CFG_Composition", new Composition(graph).toTableString());
    }

    @SuppressWarnings("unused")
    private static void printMethodUnitsToConsole(String className, String methodName) {
        // For Testing Purposes Only. E.g. className: com.example.android.lifecycle.ActivityA, methodName: onCreate
        System.out.println("**** Printing method units: " + className + " " + methodName + " ****");
        SootClass clazz = Scene.v().getSootClass(className);
        for (SootMethod method : clazz.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (method.hasActiveBody()) {
                    for (Unit unit : method.getActiveBody().getUnits()) {
                        System.out.println(unit.toString());
                    }
                }
            }
        }
        System.out.println("**** END ****");
    }


    @SuppressWarnings("unused")
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

    private static Collection<SootMethod> retrieveAllMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Collection<SootMethod> allMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            List<SootMethod> methods = sootClass.getMethods();
            allMethods.addAll(methods);
        }

        return allMethods;
    }

    private static Collection<SootMethod> filterMethods() {
        Chain<SootClass> classes = Scene.v().getClasses();
        Collection<SootMethod> filteredMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            if (Filter.isValidClass(sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    if (Filter.isValidMethod(method)) {
                        filteredMethods.add(method);
                    }
                }
            }
        }

        return filteredMethods;
    }

    private static Collection<SootClass> retrieveAllClasses() {
        return new HashSet<>(Scene.v().getClasses());
    }

    private static Collection<SootClass> filterClasses() {
        Chain<SootClass> allClasses = Scene.v().getClasses();
        Collection<SootClass> filteredClasses = new HashSet<>();

        for (SootClass sootClass : allClasses) {
            if (Filter.isValidClass(sootClass)) {
                filteredClasses.add(sootClass);
            }
        }

        return filteredClasses;
    }

    public String getCallbackTable() throws XmlPullParserException, IOException {
        List<Control> controls = new ArrayList<>(this.uiControls.getControls());
        String[][] data = new String[controls.size() + 1][];
        data[0] = new String[]{"WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD"};
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);
            data[i + 1] = new String[]{String.valueOf(control.getControlResource().getResourceID()),
                    control.getControlResource().getResourceName(), control.getControlActivity().getName(),
                    (control.getClickListener() != null ? control.getClickListener().getSignature() : "NULL")};
        }
        return StringTable.tableWithLines(data, true);
    }

    public String getUnassignedCallbackTable() throws FileNotFoundException {
        // TODO: Where am I filtering the assigned callbacks here? Don't think I am.
        List<SootMethod> listenerMethods = new ArrayList<>(this.getListenerMethods());
        List<SootMethod> possibleListeners = new ArrayList<>(this.getPossibleListenerMethods());
        String[][] data = new String[listenerMethods.size() + possibleListeners.size() + 1][];
        data[0] = new String[]{"LISTENER CLASS", "LISTENER METHOD", "POSSIBLE"};
        for (int i = 0; i < listenerMethods.size(); i++) {
            SootMethod method = listenerMethods.get(i);
            data[i + 1] =
                    new String[]{method.getDeclaringClass().getShortName(), method.getName(), String.valueOf(false)};
        }
        int offset = listenerMethods.size() + 1;
        for (int j = 0; j < possibleListeners.size(); j++) {
            SootMethod method = possibleListeners.get(j);
            data[j + offset] =
                    new String[]{method.getDeclaringClass().getShortName(), method.getName(), String.valueOf(true)};
        }
        return StringTable.tableWithLines(data, true);
    }

    public void writeAnalysisToFile(File directory) throws XmlPullParserException, IOException {
        // TODO: Verify file contents for ActivityLifecycle, MoClock and VolumeControl.
        // TODO: Make sure method list does not include other method types. No duplicates.
        Writer.writeCollection(directory, "Class_List", this.getFilteredClasses());
        Writer.writeCollection(directory, "Methods_List", this.getFilteredMethods());
        Writer.writeCollection(directory, "Lifecycle_List", this.getLifecycleMethods());
        Writer.writeCollection(directory, "Listener_List", this.getListenerMethods());
        Writer.writeCollection(directory, "Other_Callback_List", this.getOtherCallbackMethods());
        Writer.writeCollection(directory, "Possible_Callback_List", this.getPossibleListenerMethods());
        Writer.writeCollection(directory, "Control_List", this.uiControls.getControls());
        Writer.writeString(directory, "Callback_Table", this.getCallbackTable());
        Writer.writeString(directory, "Unassigned_Callback_Table", this.getUnassignedCallbackTable());
    }

    public void printAppDetails(File apk) throws XmlPullParserException, IOException {
        // TODO: Make sure method count does not include other method types. No duplicates.
        logger.info(String.format("Package name: %s", FlowDroidUtils.getBasePackageName(apk)));
        logger.info(String.format("Found %s entry points.", FlowDroidUtils.getEntryPointClasses(apk).size()));
        logger.info(String.format("Found %s launch activities.", FlowDroidUtils.getLaunchActivities(apk).size()));
        logger.info(String.format("Found %s app classes (Total:%s)", this.getFilteredClasses().size(),
                this.getAllClasses().size()
                                 ));
        logger.info(String.format("Found %s app methods (Total:%s)", this.getFilteredMethods().size(),
                this.getAllMethods().size()
                                 ));
        logger.info(String.format("Found %s lifecycle methods.", this.getLifecycleMethods().size()));
        logger.info(String.format("Found %s listener methods.", this.getListenerMethods().size()));
        logger.info(String.format("Found %s other callbacks.", this.getOtherCallbackMethods().size()));
        logger.info(String.format("Found %s possible callbacks.", this.getPossibleListenerMethods().size()));
        logger.info(String.format("Found %s UI controls.", this.uiControls.getControls().size()));
    }

    public Collection<SootClass> getAllClasses() {
        if (this.allClasses == null) {
            this.allClasses = Viewer.retrieveAllClasses();
        }

        return this.allClasses;
    }

    public Collection<SootClass> getFilteredClasses() {
        if (this.filteredClasses == null) {
            this.filteredClasses = Viewer.filterClasses();
        }

        return this.filteredClasses;
    }

    public Collection<SootMethod> getAllMethods() {
        if (this.allMethods == null) {
            this.allMethods = Viewer.retrieveAllMethods();
        }

        return this.allMethods;
    }

    public Collection<SootMethod> getFilteredMethods() {
        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        return this.filteredMethods;
    }

    public Collection<SootMethod> getLifecycleMethods() {
        if (this.lifecycleMethods == null) {
            this.lifecycleMethods = filterLifecycleMethods();
        }

        return this.lifecycleMethods;
    }

    public Collection<SootMethod> getListenerMethods() throws FileNotFoundException {
        if (this.listenerMethods == null) {
            this.listenerMethods = filterListenerMethods(this.collectedCallbacksFile);
        }

        return this.listenerMethods;
    }

    public Collection<SootMethod> getOtherCallbackMethods() throws FileNotFoundException {
        if (this.otherCallbackMethods == null) {
            this.otherCallbackMethods = filterOtherCallbackMethods(this.collectedCallbacksFile);
        }

        return this.otherCallbackMethods;
    }

    public Collection<SootMethod> getPossibleListenerMethods() throws FileNotFoundException {
        if (this.possibleListenerMethods == null) {
            this.possibleListenerMethods = filterPossibleListenerMethods(this.collectedCallbacksFile);
        }

        return this.possibleListenerMethods;
    }

    private Collection<SootMethod> filterLifecycleMethods() {
        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        Collection<SootMethod> lifecycleMethods = new HashSet<>();
        for (SootMethod method : this.filteredMethods) {
            if (Filter.isLifecycleMethod(method)) {
                lifecycleMethods.add(method);
            }
        }

        this.lifecycleMethods = lifecycleMethods;
        return lifecycleMethods;
    }

    private Collection<SootMethod> filterListenerMethods(File callbacksFile) throws FileNotFoundException {
        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        Collection<SootMethod> methods = new HashSet<>();
        for (SootMethod method : this.filteredMethods) {
            if (Filter.isListenerMethod(callbacksFile, method)) {
                methods.add(method);
            }
        }

        this.listenerMethods = methods;
        return methods;
    }

    private Collection<SootMethod> filterOtherCallbackMethods(File callbacksFile) throws FileNotFoundException {
        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        Collection<SootMethod> methods = new HashSet<>();
        for (SootMethod method : this.filteredMethods) {
            if (Filter.isOtherCallbackMethod(callbacksFile, method)) {
                methods.add(method);
            }
        }

        this.otherCallbackMethods = methods;
        return methods;
    }

    private Collection<SootMethod> filterPossibleListenerMethods(File callbacksFile) throws FileNotFoundException {
        if (this.filteredMethods == null) {
            this.filteredMethods = Viewer.filterMethods();
        }

        Collection<SootMethod> methods = new HashSet<>();
        for (SootMethod method : this.filteredMethods) {
            if (Filter.isPossibleListenerMethod(callbacksFile, method)) {
                methods.add(method);
            }
        }

        this.possibleListenerMethods = methods;
        return methods;
    }
}
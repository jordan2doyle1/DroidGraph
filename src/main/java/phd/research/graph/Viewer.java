package phd.research.graph;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.StringTable;
import phd.research.core.FlowDroidUtils;
import phd.research.core.UiControls;
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

    @NotNull
    private final File collectedCallbacksFile;
    @NotNull
    private final UiControls uiControls;

    private Collection<SootClass> filteredClasses;
    private Collection<SootMethod> filteredMethods;
    private Collection<SootMethod> lifecycleMethods;
    private Collection<SootMethod> listenerMethods;
    private Collection<SootMethod> possibleListenerMethods;
    private Collection<SootMethod> otherCallbackMethods;

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

    @SuppressWarnings("unused")
    private static String getCollectionSummary(Collection<?> collection) {
        int counter = 0;
        int numberOfPrints = 10;
        StringBuilder builder = new StringBuilder();
        for (Object item : collection) {
            if (counter < numberOfPrints) {
                builder.append(item.toString()).append("\n");
                counter++;
            } else {
                int remaining = collection.size() - numberOfPrints;
                builder.append("+ ").append(remaining).append(" more!");
                break;
            }
        }
        return builder.toString();
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

    public void writeAnalysisToFile(File directory, File apk) throws XmlPullParserException, IOException {
        //TODO: Verify file contents for ActivityLifecycle, MoClock and VolumeControl.
        //TODO: Make sure method list does not include other method types. No duplicates.
        Writer.writeCollection(directory, "entry_points", FlowDroidUtils.getEntryPointClasses(apk));
        Writer.writeCollection(directory, "launch_activities", FlowDroidUtils.getLaunchActivities(apk));
        Writer.writeCollection(directory, "lifecycle_methods", this.getLifecycleMethods());
        Writer.writeCollection(directory, "listener_methods", this.getListenerMethods());
        Writer.writeCollection(directory, "possible_callbacks", this.getPossibleListenerMethods());
        Writer.writeCollection(directory, "other_callbacks", this.getOtherCallbackMethods());
        Writer.writeCollection(directory, "filtered_classes", this.getFilteredClasses());
        Writer.writeCollection(directory, "filtered_methods", this.getFilteredMethods());
        Writer.writeCollection(directory, "app_controls", this.uiControls.getControls());
        Writer.writeString(directory, "control_callbacks", this.getCallbackTable());
        Writer.writeString(directory, "unassigned_callbacks", this.getUnassignedCallbackTable());
    }

    public Collection<SootClass> getFilteredClasses() {
        if (this.filteredClasses == null) {
            this.filteredClasses = Viewer.filterClasses();
        }

        return this.filteredClasses;
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
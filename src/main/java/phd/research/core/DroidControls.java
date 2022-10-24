package phd.research.core;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.StringTable;
import phd.research.graph.Control;
import phd.research.graph.Filter;
import phd.research.graph.Writer;
import phd.research.helper.MenuFileParser;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */

public class DroidControls {

    private static final Logger LOGGER = LoggerFactory.getLogger(DroidControls.class);

    @NotNull
    private final FlowDroidAnalysis flowDroidAnalysis;

    private final Collection<Control> controls;

    public DroidControls(FlowDroidAnalysis flowDroidAnalysis) throws XmlPullParserException, IOException {
        this.flowDroidAnalysis = Objects.requireNonNull(flowDroidAnalysis);
        this.controls = processFlowDroidControls();
    }

    private static String getResourceName(String name) {
        int index = name.lastIndexOf("/");
        name = index != -1 ? name.replace(name.substring(0, index + 1), "") : name;
        return name.contains(".xml") ? name.replace(".xml", "") : name;
    }

    public Collection<Control> getControls() {
        return this.controls;
    }

    public FlowDroidAnalysis getFlowDroidAnalysis() {
        return this.flowDroidAnalysis;
    }

    public void writeControlsToFile(File directory) throws IOException {
        Writer.writeCollection(directory, "app_controls", this.getControls());
        Writer.writeString(directory, "control_callbacks", this.getControlCallbackTableString());
    }

    public String getControlCallbackTableString() {
        List<Control> controls = new ArrayList<>(this.getControls());
        String[][] data = new String[controls.size() + 1][];
        data[0] = new String[]{"WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD"};
        for (int i = 0; i < controls.size(); i++) {
            Control control = controls.get(i);

            StringBuilder builder = new StringBuilder("[");
            control.getClickListeners().forEach(l -> builder.append(l.getName()).append(","));
            if (builder.charAt(builder.length() - 1) != '[') {
                builder.replace(builder.length() - 1, builder.length(), "]");
            } else {
                builder.append("]");
            }

            data[i + 1] = new String[]{String.valueOf(control.getControlResource().getResourceID()),
                    control.getControlResource().getResourceName(), control.getControlActivity().getName(),
                    builder.toString()};
        }
        return StringTable.tableWithLines(data, true);
    }

    private List<ARSCFileParser.AbstractResource> getResourcesWithId(int resourceId) {
        ARSCFileParser.ResType resType = this.flowDroidAnalysis.getResources().findResourceType(resourceId);
        return resType != null ? resType.getAllResources(resourceId) : new ArrayList<>();
    }

    private ARSCFileParser.AbstractResource findLayoutWithMenuId(MultiMap<String, AndroidLayoutControl> layoutControls,
            int id) throws RuntimeException {
        for (String resourceFileName : layoutControls.keySet()) {
            for (AndroidLayoutControl control : layoutControls.get(resourceFileName)) {
                if (control.getAdditionalAttributes() == null) {
                    continue;
                }

                for (String attributeKey : control.getAdditionalAttributes().keySet()) {
                    if (attributeKey.equals("menu")) {
                        if ((int) control.getAdditionalAttributes().get(attributeKey) == id) {
                            return this.flowDroidAnalysis.getResources()
                                    .findResourceByName("layout", getResourceName(resourceFileName));
                        }
                    }
                }
            }
        }

        throw new RuntimeException(String.format("Could not find menu resource %s in layouts.", id));
    }

    private Collection<Control> processResourceControls(String fileName, ARSCFileParser.AbstractResource layoutResource,
            MultiMap<String, AndroidLayoutControl> flowDroidControls) throws RuntimeException {

        Collection<Control> controls = new HashSet<>();
        SootClass layoutClass = this.findClassLinkedWithLayout(layoutResource);

        for (AndroidLayoutControl control : flowDroidControls.get(fileName)) {
            if (control.getID() == -1) {
                continue;
            }

            List<ARSCFileParser.AbstractResource> controlResources = getResourcesWithId(control.getID());
            if (controlResources.isEmpty()) {
                LOGGER.error(String.format("Could not find control with id %s.", control.getID()));
                continue;
            } else if (controlResources.size() > 1) {
                LOGGER.warn(String.format("Found multiple controls with id %s, returning first.", control.getID()));
            }
            // TODO: Should loop through returned resources to retrieve the correct control rather than just the first?
            ARSCFileParser.AbstractResource controlResource = controlResources.get(0);

            Collection<SootMethod> listeners = new ArrayList<>();
            if (control.getClickListener() != null) {
                listeners = Filter.findCallbackMethods(control.getClickListener());
                if (listeners.size() > 1) {
                    LOGGER.warn(String.format("Found multiple listeners with control id %s.", control.getID()));
                }
            }

            Control droidControl = new Control(controlResource, layoutResource, layoutClass, listeners);
            controls.add(droidControl);
        }
        return controls;
    }

    private Collection<Control> processFlowDroidControls() {
        LayoutFileParser layoutParser = this.flowDroidAnalysis.getLayoutFileParser();
        MultiMap<String, AndroidLayoutControl> layoutControls = layoutParser.getUserControls();
        Collection<Control> controls = new HashSet<>();

        for (String layoutFileName : layoutControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource =
                    this.flowDroidAnalysis.getResources().findResourceByName("layout", getResourceName(layoutFileName));
            if (Filter.isValidLayout(layoutResource.getResourceName())) {
                try {
                    controls.addAll(processResourceControls(layoutFileName, layoutResource, layoutControls));
                } catch (RuntimeException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        MenuFileParser menuParser = this.flowDroidAnalysis.getMenuFileParser();
        MultiMap<String, AndroidLayoutControl> menuControls = menuParser.getUserControls();

        for (String menuFileName : menuControls.keySet()) {
            ARSCFileParser.AbstractResource menuResource =
                    this.flowDroidAnalysis.getResources().findResourceByName("menu", getResourceName(menuFileName));
            ARSCFileParser.AbstractResource layoutResource;
            try {
                layoutResource = findLayoutWithMenuId(layoutControls, menuResource.getResourceID());
                if (Filter.isValidLayout(layoutResource.getResourceName())) {
                    continue;
                }
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                continue;
            }

            try {
                controls.addAll(processResourceControls(menuFileName, layoutResource, menuControls));
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
            }
        }

        return controls;
    }

    private SootClass findClassLinkedWithLayout(ARSCFileParser.AbstractResource layout) throws RuntimeException {
        for (SootClass clazz : Scene.v().getClasses()) {
            if (Filter.isValidClass(clazz)) {
                SootClass layoutClass = recursiveClassSearch(clazz, layout.getResourceID());
                if (layoutClass != null) {
                    return layoutClass;
                }
            }
        }
        throw new RuntimeException(
                String.format("Could not find class linked with layout resource %s (%s).", layout.getResourceID(),
                        layout.getResourceName()
                             ));
    }

    private SootClass recursiveClassSearch(SootClass clazz, int id) {
        for (SootMethod method : clazz.getMethods()) {
            try {
                method.retrieveActiveBody();
            } catch (RuntimeException ignored) {
                continue;
            }

            if (method.hasActiveBody() && searchMethodInvokeExprForId(method, id)) {
                return clazz;
            }
        }

        if (clazz.hasSuperclass()) {
            return recursiveClassSearch(clazz.getSuperclassUnsafe(), id);
        }
        return null;
    }

    private boolean searchMethodInvokeExprForId(SootMethod method, int id) {
        PatchingChain<Unit> units = method.getActiveBody().getUnits();
        Map<Value, List<String>> variableDeclarations = new HashMap<>();
        for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
            Unit unit = iterator.next();

            final boolean[] searchStatus = {false};
            unit.apply(new AbstractStmtSwitch<Stmt>() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    super.caseInvokeStmt(stmt);
                    InvokeExpr expr = stmt.getInvokeExpr();
                    if (expr.getMethod().getName().equals("setContentView") ||
                            expr.getMethod().getName().equals("inflate")) {
                        if (expr.getArgCount() > 0 && expr.getArg(0).toString().equals(String.valueOf(id))) {
                            searchStatus[0] = true;
                        }
                    } else if (expr.getMethod().getName().equals("<init>") &&
                            expr.getMethod().getDeclaringClass().getShortName().equals("RemoteViews")) {
                        if (expr.getArgCount() > 0) {
                            if (expr.getArg(1) instanceof Constant &&
                                    expr.getArg(1).toString().equals(String.valueOf(id))) {
                                searchStatus[0] = true;
                            } else if (variableDeclarations.containsKey(expr.getArg(1))) {
                                List<String> assignmentValues = variableDeclarations.get(expr.getArg(1));
                                for (String value : assignmentValues) {
                                    if (value.equals(String.valueOf(id))) {
                                        searchStatus[0] = true;
                                        break;
                                    }
                                }
                            } else if (findVariableValue(expr.getArg(1)).equals(String.valueOf(id))) {
                                searchStatus[0] = true;
                            }
                        }
                    }
                }

                @Override
                public void caseAssignStmt(AssignStmt stmt) {
                    super.caseAssignStmt(stmt);
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr expr = stmt.getInvokeExpr();
                        if (expr.getMethod().getName().equals("setContentView") ||
                                expr.getMethod().getName().equals("inflate")) {
                            if (expr.getArgCount() > 0 && expr.getArg(0).toString().equals(String.valueOf(id))) {
                                searchStatus[0] = true;
                            }
                        }
                    }

                    if (stmt.getRightOpBox().getValue() instanceof Constant) {
                        if (variableDeclarations.containsKey(stmt.getLeftOp())) {
                            List<String> assignmentValues = variableDeclarations.get(stmt.getLeftOp());
                            assignmentValues.add(stmt.getRightOpBox().getValue().toString());
                            variableDeclarations.put(stmt.getLeftOp(), assignmentValues);
                        } else {
                            variableDeclarations.put(stmt.getLeftOp(), new ArrayList<>(
                                    Collections.singletonList(stmt.getRightOpBox().getValue().toString())));
                        }
                    }
                }
            });

            if (searchStatus[0]) {
                return true;
            }
        }
        return false;
    }

    private String findVariableValue(Value variable) {
        return variable.toString();
    }
}
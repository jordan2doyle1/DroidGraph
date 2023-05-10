package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.Timer;
import phd.research.graph.Control;
import phd.research.helper.MenuFileParser;
import phd.research.singletons.FlowDroidAnalysis;
import phd.research.singletons.GraphSettings;
import phd.research.utility.Filter;
import phd.research.utility.Writer;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class DroidControls {

    private static final Logger LOGGER = LoggerFactory.getLogger(DroidControls.class);

    private final Collection<Control> controls;

    public DroidControls() {
        this.controls = this.processFlowDroidControls();
    }

    private static String getResourceName(String name) {
        int index = name.lastIndexOf("/");
        name = index != -1 ? name.replace(name.substring(0, index + 1), "") : name;
        return name.contains(".xml") ? name.replace(".xml", "") : name;
    }

    private static Collection<SootMethod> findCallbackMethods(String methodName) {
        // Warning: if name is not specific, false positives may be returned. e.g. methodName = onClick()
        Collection<SootMethod> methods = new ArrayList<>();
        Scene.v().getClasses().forEach(clazz -> clazz.getMethods().stream().filter(Filter::isValidMethod)
                .filter(method -> method.getName().equals(methodName)).forEach(methods::add));
        return methods;
    }

    public Collection<Control> getControls() {
        return this.controls;
    }

    private Collection<Control> processFlowDroidControls() {
        Timer timer = new Timer();
        LOGGER.info("Parsing app controls... (" + timer.start(true) + ")");

        LayoutFileParser layoutParser = FlowDroidAnalysis.v().getLayoutFileParser();
        MultiMap<String, AndroidLayoutControl> layoutControls = layoutParser.getUserControls();
        Collection<Control> controls = new HashSet<>();

        try {
            Writer.writeMultiMap(GraphSettings.v().getOutputDirectory(), "layout_controls.txt", layoutControls);
        } catch (IOException e) {
            LOGGER.error("Failed to output layout controls. " + e.getMessage());
        }

        for (String layoutFileName : layoutControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource = FlowDroidAnalysis.v().getResources()
                    .findResourceByName("layout", DroidControls.getResourceName(layoutFileName));
            if (Filter.isValidLayout(layoutResource.getResourceName())) {
                try {
                    controls.addAll(this.processResourceControls(layoutFileName, layoutResource, layoutControls));
                } catch (RuntimeException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        MenuFileParser menuParser = FlowDroidAnalysis.v().getMenuFileParser();
        MultiMap<String, AndroidLayoutControl> menuControls = menuParser.getUserControls();

        try {
            Writer.writeMultiMap(GraphSettings.v().getOutputDirectory(), "menu_controls.txt", menuControls);
        } catch (IOException e) {
            LOGGER.error("Failed to output menu controls. " + e.getMessage());
        }

        for (String menuFileName : menuControls.keySet()) {
            ARSCFileParser.AbstractResource menuResource = FlowDroidAnalysis.v().getResources()
                    .findResourceByName("menu", DroidControls.getResourceName(menuFileName));
            ARSCFileParser.AbstractResource layoutResource;
            try {
                layoutResource = this.findLayoutWithMenuId(layoutControls, menuResource.getResourceID());
                if (Filter.isValidLayout(layoutResource.getResourceName())) {
                    continue;
                }
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
                continue;
            }

            try {
                controls.addAll(this.processResourceControls(menuFileName, layoutResource, menuControls));
            } catch (RuntimeException e) {
                LOGGER.error(e.getMessage());
            }
        }

        LOGGER.info("(" + timer.end() + ") Parsing app controls took " + timer.secondsDuration() + " second(s).");
        return controls;
    }

    private Collection<Control> processResourceControls(String fileName, ARSCFileParser.AbstractResource layoutResource,
            MultiMap<String, AndroidLayoutControl> flowDroidControls) {

        Collection<Control> controls = new HashSet<>();
        SootClass layoutClass = this.findClassLinkedWithLayout(layoutResource);

        for (AndroidLayoutControl control : flowDroidControls.get(fileName)) {
            if (control.getID() == -1) {
                continue;
            }

            List<ARSCFileParser.AbstractResource> controlResources = this.getResourcesWithId(control.getID());
            if (controlResources.isEmpty()) {
                LOGGER.error(String.format("Could not find control with id %s.", control.getID()));
                continue;
            } else if (controlResources.size() > 1) {

                LOGGER.warn(String.format("Found multiple controls with id %s, returning first.", control.getID()));
            }

            //TODO: Loop through resources to retrieve the correct control rather than the first?
            ARSCFileParser.AbstractResource controlResource = controlResources.get(0);

            Collection<SootMethod> listeners = new ArrayList<>();
            if (control.getClickListener() != null) {
                listeners = DroidControls.findCallbackMethods(control.getClickListener());
                if (listeners.size() > 1) {
                    LOGGER.warn(String.format("Found multiple listeners with control id %s.", control.getID()));
                }
            }

            Control droidControl = new Control(controlResource.getResourceID(), controlResource.getResourceName(),
                    layoutResource.getResourceID(), layoutResource.getResourceName(), layoutClass.getName(),
                    listeners.stream().map(SootMethod::getSignature).collect(Collectors.toList())
            );
            controls.add(droidControl);
        }
        return controls;
    }

    private ARSCFileParser.AbstractResource findLayoutWithMenuId(MultiMap<String, AndroidLayoutControl> layoutControls,
            int id) {
        for (String resourceFileName : layoutControls.keySet()) {
            for (AndroidLayoutControl control : layoutControls.get(resourceFileName)) {
                if (control.getAdditionalAttributes() == null) {
                    continue;
                }

                for (String attributeKey : control.getAdditionalAttributes().keySet()) {
                    if (attributeKey.equals("menu")) {
                        if ((int) control.getAdditionalAttributes().get(attributeKey) == id) {
                            return FlowDroidAnalysis.v().getResources()
                                    .findResourceByName("layout", DroidControls.getResourceName(resourceFileName));
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Could not find menu resource " + id + " in layouts.");
    }

    private List<ARSCFileParser.AbstractResource> getResourcesWithId(int resourceId) {
        ARSCFileParser.ResType resType = FlowDroidAnalysis.v().getResources().findResourceType(resourceId);
        return resType != null ? resType.getAllResources(resourceId) : new ArrayList<>();
    }

    private SootClass findClassLinkedWithLayout(ARSCFileParser.AbstractResource layout) {
        for (SootClass clazz : Scene.v().getClasses()) {
            if (Filter.isValidClass(clazz)) {
                SootClass layoutClass = this.recursiveClassSearch(clazz, layout.getResourceID());
                if (layoutClass != null) {
                    return layoutClass;
                }
            }
        }
        throw new RuntimeException("Could not find class linked with layout resource " + layout.getResourceID() + " (" +
                layout.getResourceName() + ").");
    }

    private SootClass recursiveClassSearch(SootClass clazz, int id) {
        for (SootMethod method : clazz.getMethods()) {
            try {
                method.retrieveActiveBody();
            } catch (RuntimeException ignored) {
                continue;
            }

            if (method.hasActiveBody() && this.searchMethodInvokeExprForId(method, id)) {
                return clazz;
            }
        }

        if (clazz.hasSuperclass()) {
            return this.recursiveClassSearch(clazz.getSuperclassUnsafe(), id);
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
                            } else if (expr.getArg(1).toString().equals(String.valueOf(id))) {
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
}
package phd.research.core;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.graph.Control;
import phd.research.graph.Filter;
import phd.research.helper.MenuFileParser;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class DroidControls {

    private static final Logger logger = LoggerFactory.getLogger(DroidControls.class);

    @NotNull
    private final File callbacksFile;
    @NotNull
    private final File apk;

    private final ARSCFileParser resources;
    private final Collection<Control> controls;

    public DroidControls(File callbacksFile, File apk) throws XmlPullParserException, IOException {
        this.callbacksFile = Objects.requireNonNull(callbacksFile);
        this.apk = Objects.requireNonNull(apk);
        this.resources = FlowDroidUtils.getResources(this.apk);
        this.controls = processFlowDroidControls();
    }

    private static String getResourceName(String name) {
        int index = name.lastIndexOf("/");
        name = index != -1 ? name.replace(name.substring(0, index + 1), "") : name;
        return name.contains(".xml") ? name.replace(".xml", "") : name;
    }

    private static Collection<SootMethod> searchForCallbackMethods(File callbacksFile, String methodName)
            throws FileNotFoundException {
        // Warning: if method name is not specific enough, false positives may be returned. e.g. methodName = onClick()
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbacksFile);
        Collection<SootMethod> methods = new ArrayList<>();
        callbacks.getCallbackMethods().keySet().forEach(clazz -> callbacks.getCallbackMethods().get(clazz).stream()
                .filter(definition -> definition.getTargetMethod().getName().equals(methodName))
                .forEach(callback -> methods.add(callback.getTargetMethod())));
        return methods;
    }

    public Collection<Control> getControls() {
        return this.controls;
    }

    public Collection<Control> getControlsWithListener(SootMethod listener) {
        return this.getControls().stream()
                .filter(c -> !c.getClickListeners().isEmpty() && c.getClickListeners().contains(listener))
                .collect(Collectors.toList());
    }

    private List<ARSCFileParser.AbstractResource> getResourcesWithId(int resourceId) {
        ARSCFileParser.ResType resType = this.resources.findResourceType(resourceId);
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
                            return resources.findResourceByName("layout", getResourceName(resourceFileName));
                        }
                    }
                }
            }
        }

        throw new RuntimeException(String.format("Could not find menu resource %s in layouts.", id));
    }

    private Collection<Control> processResourceControls(String fileName, ARSCFileParser.AbstractResource layoutResource,
            MultiMap<String, AndroidLayoutControl> flowDroidControls) throws FileNotFoundException, RuntimeException {

        Collection<Control> controls = new HashSet<>();
        SootClass layoutClass = this.findClassLinkedWithLayout(layoutResource);

        for (AndroidLayoutControl control : flowDroidControls.get(fileName)) {
            if (control.getID() == -1) {
                continue;
            }

            List<ARSCFileParser.AbstractResource> controlResources = getResourcesWithId(control.getID());
            if (controlResources.isEmpty()) {
                logger.error(String.format("Could not find control with id %s.", control.getID()));
                continue;
            } else if (controlResources.size() > 1) {
                logger.warn(String.format("Found multiple controls with id %s, returning first.", control.getID()));
            }
            // TODO: Should loop through returned resources to retrieve the correct control rather than just the first?
            ARSCFileParser.AbstractResource controlResource = controlResources.get(0);

            Collection<SootMethod> listeners = new ArrayList<>();
            if (control.getClickListener() != null) {
                listeners = DroidControls.searchForCallbackMethods(this.callbacksFile, control.getClickListener());
                if (listeners.size() > 1) {
                    logger.warn(String.format("Found multiple listeners with control id %s.", control.getID()));
                }
            }

            Control droidControl = new Control(controlResource, layoutResource, layoutClass, listeners);
            controls.add(droidControl);
        }
        return controls;
    }

    private Collection<Control> processFlowDroidControls() throws XmlPullParserException, IOException {
        LayoutFileParser layoutParser = FlowDroidUtils.getLayoutFileParser(this.apk);
        MultiMap<String, AndroidLayoutControl> layoutControls = layoutParser.getUserControls();
        Collection<Control> controls = new HashSet<>();

        for (String layoutFileName : layoutControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource =
                    this.resources.findResourceByName("layout", getResourceName(layoutFileName));
            if (Filter.isBlackListedResource(layoutResource.getResourceName())) {
                continue;
            }

            try {
                controls.addAll(processResourceControls(layoutFileName, layoutResource, layoutControls));
            } catch (RuntimeException e) {
                logger.error(e.getMessage());
            }
        }

        MenuFileParser menuParser = FlowDroidUtils.getMenuFileParser(this.apk);
        MultiMap<String, AndroidLayoutControl> menuControls = menuParser.getUserControls();

        for (String menuFileName : menuControls.keySet()) {
            ARSCFileParser.AbstractResource menuResource =
                    this.resources.findResourceByName("menu", getResourceName(menuFileName));
            ARSCFileParser.AbstractResource layoutResource;
            try {
                layoutResource = findLayoutWithMenuId(layoutControls, menuResource.getResourceID());
                if (Filter.isBlackListedResource(layoutResource.getResourceName())) {
                    continue;
                }
            } catch (RuntimeException e) {
                logger.error(e.getMessage());
                continue;
            }

            try {
                controls.addAll(processResourceControls(menuFileName, layoutResource, menuControls));
            } catch (RuntimeException e) {
                logger.error(e.getMessage());
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
                    } else if (expr.getMethod().getName().equals("<init>") && expr.getMethod().getDeclaringClass().getShortName().equals("RemoteViews")) {
                        if (expr.getArgCount() > 0) {
                            if (expr.getArg(1) instanceof Constant && expr.getArg(1).toString().equals(String.valueOf(id))) {
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
                            variableDeclarations.put(stmt.getLeftOp(),
                                    new ArrayList<>(
                                            Collections.singletonList(stmt.getRightOpBox().getValue().toString())
                                    )
                                                    );
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
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

public class UiControls {

    private static final Logger logger = LoggerFactory.getLogger(UiControls.class);

    @NotNull
    private final File collectedCallbacksFile;
    @NotNull
    private final File apk;

    private final Collection<Control> controls;

    public UiControls(File collectedCallbacksFile, File apk) throws XmlPullParserException, IOException {
        this.collectedCallbacksFile = Objects.requireNonNull(collectedCallbacksFile);
        this.apk = Objects.requireNonNull(apk);
        this.controls = processFlowDroidControls();
    }

    private static String getResourceName(String name) {
        int index = name.lastIndexOf("/");
        name = index != -1 ? name.replace(name.substring(0, index + 1), "") : name;
        return name.contains(".xml") ? name.replace(".xml", "") : name;
    }

    private static List<ARSCFileParser.AbstractResource> getResourcesWithId(ARSCFileParser resources, int resourceId) {
        ARSCFileParser.ResType resType = resources.findResourceType(resourceId);
        return resType != null ? resType.getAllResources(resourceId) : new ArrayList<>();
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

    private Collection<Control> processFlowDroidControls() throws XmlPullParserException, IOException {
        ARSCFileParser resources = FlowDroidUtils.getResources(this.apk);
        LayoutFileParser layoutParser = FlowDroidUtils.getLayoutFileParser(this.apk);
        MultiMap<String, AndroidLayoutControl> flowDroidControls = layoutParser.getUserControls();

        Collection<Control> uiControls = processMenuControls(resources);

        for (String layoutFile : flowDroidControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource =
                    resources.findResourceByName("layout", getResourceName(layoutFile));
            if (Filter.isBlackListedLayout(layoutResource.getResourceName())) {
                continue;
            }

            SootClass callbackClass = this.findClassLinkedWithLayout(layoutResource.getResourceID());
            if (callbackClass == null) {
                logger.error(String.format("Could not find class linked with layout resource %s(%s).",
                        layoutResource.getResourceName(), layoutResource.getResourceID()
                                          ));
                continue;
            }
            logger.debug("Linked " + callbackClass + " with " + layoutResource.getResourceName());

            for (AndroidLayoutControl control : flowDroidControls.get(layoutFile)) {
                if (control.getID() == -1) {
                    continue;
                }

                List<ARSCFileParser.AbstractResource> controlResources =
                        UiControls.getResourcesWithId(resources, control.getID());
                if (controlResources.isEmpty()) {
                    logger.error(String.format("Could not find resource with id %s (%s).", control.getID(), control));
                    continue;
                } else if (controlResources.size() > 1) {
                    logger.warn(String.format("Found multiple resources with id %s, returning the first.",
                            control.getID()
                                             ));
                }
                ARSCFileParser.AbstractResource controlResource = controlResources.get(0);

                Collection<SootMethod> clickListeners = new ArrayList<>();
                if (control.getClickListener() != null) {
                    clickListeners = UiControls.searchForCallbackMethods(this.collectedCallbacksFile,
                            control.getClickListener()
                                                                        );
                    if (clickListeners.isEmpty()) {
                        logger.debug(String.format("No click listener method with id %s.", control.getID()));
                    } else if (controlResources.size() > 1) {
                        logger.warn(String.format("Found multiple click listeners with id %s.", control.getID()));
                    }
                }

                Control newControl = new Control(controlResource, layoutResource, callbackClass, clickListeners);
                uiControls.add(newControl);
            }
        }

        return uiControls;
    }

    private Collection<Control> processMenuControls(ARSCFileParser resources)
            throws XmlPullParserException, IOException {
        MenuFileParser menuParser =
                new MenuFileParser(FlowDroidUtils.getBasePackageName(this.apk), FlowDroidUtils.getResources(this.apk));
        menuParser.parseLayoutFileDirect(this.apk.getAbsolutePath());
        MultiMap<String, AndroidLayoutControl> flowDroidControls = menuParser.getUserControls();

        Set<Control> controls = new HashSet<>();
        for (String layoutFile : flowDroidControls.keySet()) {
            if (layoutFile.equals("res/menu/navigation.xml")) {
                ARSCFileParser.AbstractResource layoutResource =
                        resources.findResourceByName("menu", getResourceName(layoutFile));

                // TODO: Extract to it's own method.
                SootClass callbackClass = null;
                for (String file : flowDroidControls.keySet()) {
                    for (AndroidLayoutControl control : flowDroidControls.get(file)) {
                        if (control.getAdditionalAttributes() != null) {
                            for (String key : control.getAdditionalAttributes().keySet()) {
                                if (key.equals("menu")) {
                                    if ((int) control.getAdditionalAttributes().get(key) ==
                                            layoutResource.getResourceID()) {
                                        ARSCFileParser.AbstractResource layout =
                                                resources.findResourceByName("layout", getResourceName(file));
                                        callbackClass = this.findClassLinkedWithLayout(layout.getResourceID());
                                    }
                                }
                            }
                        }
                    }
                }

                if (callbackClass == null) {
                    logger.error("No class found for layout resource " + layoutResource.getResourceID() + ": " +
                            layoutResource.getResourceName());
                    continue;
                }
                logger.info("Linked " + callbackClass + " with " + layoutResource.getResourceName());

                for (AndroidLayoutControl control : flowDroidControls.get(layoutFile)) {
                    if (control.getID() == -1) {
                        continue;
                    }

                    List<ARSCFileParser.AbstractResource> controlResources =
                            UiControls.getResourcesWithId(resources, control.getID());
                    if (controlResources == null || controlResources.isEmpty()) {
                        logger.error("No resource found with ID " + control.getID() + ": " + control);
                        continue;
                    } else if (controlResources.size() > 1) {
                        logger.warn("Multiple resources with ID " + control.getID() + ", returning the first.");
                    }
                    ARSCFileParser.AbstractResource controlResource = controlResources.get(0);

                    Collection<SootMethod> clickListeners = new ArrayList<>();
                    if (control.getClickListener() != null) {
                        clickListeners = UiControls.searchForCallbackMethods(this.collectedCallbacksFile,
                                control.getClickListener()
                                                                            );
                        if (clickListeners.isEmpty()) {
                            logger.debug(String.format("No click listener method with id %s.", control.getID()));
                        } else if (controlResources.size() > 1) {
                            logger.warn(String.format("Found multiple click listeners with id %s.", control.getID()));
                        }
                    }

                    Control newControl = new Control(controlResource, layoutResource, callbackClass, clickListeners);
                    controls.add(newControl);
                }
            }
        }

        return controls;
    }

    private SootClass findClassLinkedWithLayout(int layoutId) {
        for (SootClass clazz : Scene.v().getClasses()) {
            if (Filter.isValidClass(clazz)) {
                SootClass layoutClass = recursiveClassSearch(clazz, "setContentView", layoutId);
                if (layoutClass == null) {
                    layoutClass = recursiveClassSearch(clazz, "inflate", layoutId);
                    if (layoutClass != null) {
                        return layoutClass;
                    }
                } else {
                    return layoutClass;
                }
            }
        }
        return null;
        //TODO: Try not to return null.
    }

    private SootClass recursiveClassSearch(SootClass clazz, String invokeMethodName, int id) {
        for (SootMethod method : clazz.getMethods()) {
            try {
                method.retrieveActiveBody();
            } catch (RuntimeException ignored) {
                continue;
            }

            if (method.hasActiveBody() && searchMethodInvokeExprForId(method, invokeMethodName, id)) {
                return clazz;
            }
        }

        if (clazz.hasSuperclass()) {
            return recursiveClassSearch(clazz.getSuperclassUnsafe(), invokeMethodName, id);
        }
        return null;
        //TODO: Try not to return null.
    }

    private boolean searchMethodInvokeExprForId(SootMethod method, String invokeMethodName, int id) {
        PatchingChain<Unit> units = method.getActiveBody().getUnits();
        for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
            Unit unit = iterator.next();

            final boolean[] searchStatus = {false};
            unit.apply(new AbstractStmtSwitch<Stmt>() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    super.caseInvokeStmt(stmt);
                    InvokeExpr expr = stmt.getInvokeExpr();
                    if (expr.getMethod().getName().equals(invokeMethodName)) {
                        if (expr.getArgCount() > 0 && expr.getArg(0).toString().equals(String.valueOf(id))) {
                            searchStatus[0] = true;
                        }
                    }
                }

                @Override
                public void caseAssignStmt(AssignStmt stmt) {
                    super.caseAssignStmt(stmt);
                    if (stmt.containsInvokeExpr()) {
                        InvokeExpr expr = stmt.getInvokeExpr();
                        if (expr.getMethod().getName().equals(invokeMethodName)) {
                            if (expr.getArgCount() > 0 && expr.getArg(0).toString().equals(String.valueOf(id))) {
                                searchStatus[0] = true;
                            }
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
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
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.io.*;
import java.util.*;

/**
 * @author Jordan Doyle
 */

@SuppressWarnings("CommentedOutCode")
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

        if (index != -1) {
            name = name.replace(name.substring(0, index + 1), "");
        }

        if (name.contains(".xml")) {
            name = name.replace(".xml", "");
        }

        return name;
    }

    private static SootMethod searchForCallbackMethod(File callbacksFile, String methodName)
            throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbacksFile);
        SootMethod foundMethod = null;

        for (SootClass currentClass : callbacks.getCallbackMethods().keySet()) {
            for (AndroidCallbackDefinition callbackDefinition : callbacks.getCallbackMethods().get(currentClass)) {
                if (callbackDefinition.getTargetMethod().getName().equals(methodName)) {
                    if (foundMethod != null) {
                        logger.error("Multiple callbacks with the name " + methodName + ".");
                        return null;
                    }
                    foundMethod = callbackDefinition.getTargetMethod();
                }
            }
        }

        return foundMethod;
    }

    private static List<ARSCFileParser.AbstractResource> getResourcesWithId(ARSCFileParser resources, int resourceId) {
        ARSCFileParser.ResType resType = resources.findResourceType(resourceId);
        if (resType == null) {
            return null;
        }

        return resType.getAllResources(resourceId);
    }

    //    private static ARSCFileParser.AbstractResource getResourceById(ARSCFileParser resources, int resourceId) {
    //        ARSCFileParser.ResType resType = resources.findResourceType(resourceId);
    //        if (resType == null) {
    //            return null;
    //        }
    //
    //        List<ARSCFileParser.AbstractResource> foundResources = resType.getAllResources(resourceId);
    //        if (foundResources.isEmpty()) {
    //            return null;
    //        }
    //
    //        if (foundResources.size() > 1) {
    //            logger.warn("Multiple resources with ID " + resourceId + ", returning the first.");
    //        }
    //
    //        return foundResources.get(0);
    //    }

    public Collection<Control> getControls() {
        return this.controls;
    }

    public Control getListenerControl(SootMethod listener) {
        Control foundControl = null;
        for (Control control : this.getControls()) {
            if (control.getClickListener() != null) {
                if (control.getClickListener().equals(listener)) {
                    if (foundControl != null) {
                        logger.error("Found multiple controls with the same listener: " + listener.getSignature());
                        return null;
                    }
                    foundControl = control;
                }
            }
        }

        return foundControl;
    }

    public Control getControl(SootClass activity, String resourceName) {
        for (Control control : this.getControls()) {
            if (control.getControlActivity().equals(activity) &&
                    control.getControlResource().getResourceName().equals(resourceName)) {
                return control;
            }
        }
        return null;
    }

    private void addControlListeners(File collectedUiCallbackLinks) throws IOException {
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(collectedUiCallbackLinks));
        while ((line = bufferedReader.readLine()) != null) {
            String[] controlListenerTuple = line.split(",");
            Control control = getControl(Scene.v().getSootClass(controlListenerTuple[0]), controlListenerTuple[1]);
            if (control != null) {
                SootMethod listener = Scene.v().getMethod(controlListenerTuple[2]);
                if (listener != null) {
                    control.setClickListener(listener);
                }
            }
        }
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
                logger.error("No class found for layout resource " + layoutResource.getResourceID() + ": " +
                        layoutResource.getResourceName());
                continue;
            }
            logger.debug("Linked " + callbackClass + " with " + layoutResource.getResourceName());

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

                SootMethod clickListener = null;
                if (control.getClickListener() != null) {
                    clickListener = UiControls.searchForCallbackMethod(this.collectedCallbacksFile, control.getClickListener());
                }

                if (clickListener == null) {
                    logger.debug("No click listener method with ID: " + control.getID());
                }

                Control newControl = new Control(controlResource, layoutResource, callbackClass, clickListener);
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

                    SootMethod clickListener = null;
                    if (control.getClickListener() != null) {
                        clickListener = UiControls.searchForCallbackMethod(this.collectedCallbacksFile,
                                control.getClickListener()
                                                                          );
                    }

                    if (clickListener == null) {
                        logger.error("No click listener method with ID: " + control.getID());
                    }

                    Control newControl = new Control(controlResource, layoutResource, callbackClass, clickListener);
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
    }

    private SootClass recursiveClassSearch(SootClass clazz, String invokeMethodName, int id) {
        for (SootMethod method : clazz.getMethods()) {
            try {
                method.retrieveActiveBody();
            } catch (RuntimeException ignored) {
                continue;
            }

            if (!method.hasActiveBody()) {
                continue;
            }

            if (searchMethodInvokeExprForId(method, invokeMethodName, id)) {
                return clazz;
            }
        }

        if (clazz.hasSuperclass()) {
            return recursiveClassSearch(clazz.getSuperclassUnsafe(), invokeMethodName, id);
        }
        return null;
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
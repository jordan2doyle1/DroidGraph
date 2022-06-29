package phd.research.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FlowDroidUtils;
import phd.research.helper.Control;
import phd.research.helper.Status;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class UiControls {

    private static final Logger logger = LoggerFactory.getLogger(UiControls.class);

    private final File collectedCallbacksFile;
    private final String apk;

    private Set<Control> controls;

    public UiControls(File collectedCallbacksFile, String apk) {
        if (!collectedCallbacksFile.exists()) {
            logger.error("Collected Callbacks File Does Not Exist!:" + collectedCallbacksFile);
        }

        this.collectedCallbacksFile = collectedCallbacksFile;
        this.apk = apk;
    }

    protected static String getResourceName(String name) {
        int index = name.lastIndexOf("/");

        if (index != -1) {
            name = name.replace(name.substring(0, index + 1), "");
        }

        if (name.contains(".xml")) {
            name = name.replace(".xml", "");
        }

        return name;
    }

    protected static SootMethod searchForCallbackMethod(File callbacksFile, String methodName) {
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(callbacksFile);
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

    protected static ARSCFileParser.AbstractResource getResourceById(ARSCFileParser resources, int resourceId) {
        ARSCFileParser.ResType resType = resources.findResourceType(resourceId);
        if (resType == null) {
            return null;
        }

        List<ARSCFileParser.AbstractResource> foundResources = resType.getAllResources(resourceId);
        if (foundResources.isEmpty()) {
            return null;
        }

        if (foundResources.size() > 1) {
            logger.warn("Multiple resources with ID " + resourceId + ", returning the first.");
        }

        return foundResources.get(0);
    }

    public Set<Control> getControls() {
        if (this.controls == null) {
            this.controls = getAllControls();
        }

        return this.controls;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasControl(SootMethod listener) {
        for (Control control : this.getControls()) {
            SootMethod controlClickListener = control.getClickListener();
            if (controlClickListener != null && controlClickListener.equals(listener)) {
                return true;
            }
        }

        return false;
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
            if (control.getControlResource() != null && control.getControlActivity() != null) {
                if (control.getControlActivity().equals(activity) &&
                        control.getControlResource().getResourceName().equals(resourceName)) {
                    return control;
                }
            }
        }

        return null;
    }

    public Control getControl(SootClass activity, int resourceId) {
        for (Control control : this.getControls()) {
            if (control.getControlResource() != null && control.getControlActivity() != null) {
                if (control.getControlActivity().equals(activity) &&
                        control.getControlResource().getResourceID() == resourceId) {
                    return control;
                }
            }
        }

        return null;
    }

    private Set<Control> getAllControls() {
        ARSCFileParser resources = FlowDroidUtils.getResources(this.apk);
        LayoutFileParser layoutParser = FlowDroidUtils.getLayoutFileParser(this.apk);

        Set<Control> uiControls = new HashSet<>();
        MultiMap<String, AndroidLayoutControl> userControls;
        if (layoutParser != null) {
            userControls = layoutParser.getUserControls();
        } else {
            logger.error("Problem getting Layout File Parser. Can't get UI Controls!");
            return uiControls;
        }

        for (String layoutFile : userControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource =
                    resources.findResourceByName("layout", getResourceName(layoutFile));

            SootClass callbackClass = this.findLayoutClass(layoutResource.getResourceID());
            if (callbackClass == null) {
                logger.error("No class found for layout resource " + layoutResource.getResourceID() + ": " + layoutResource.getResourceName());
                continue;
            }

            logger.info("Linked " + callbackClass + " with " + layoutResource.getResourceName());
            for (AndroidLayoutControl control : userControls.get(layoutFile)) {
                if (control.getID() == -1) {
                    continue;
                }

                ARSCFileParser.AbstractResource controlResource =
                        UiControls.getResourceById(resources, control.getID());
                if (controlResource == null) {
                    logger.error("No resource found with ID " + control.getID() + ": " + control);
                    continue;
                }

                SootMethod clickListener = null;
                if (control.getClickListener() != null) {
                    clickListener =
                            UiControls.searchForCallbackMethod(this.collectedCallbacksFile, control.getClickListener());
                }

                if (clickListener == null) {
                    logger.error("No click listener method with ID: " + control.getID());
                }

                Control newControl = new Control(controlResource, layoutResource, callbackClass, clickListener);
                uiControls.add(newControl);
            }
        }

        return uiControls;
    }

    private SootClass findAnySetContentView(int layoutId, SootClass entryClass) {
        for (SootMethod method : entryClass.getMethods()) {
            try {
                method.retrieveActiveBody();
            } catch (RuntimeException ignored) {
                continue;
            }

            if (!method.hasActiveBody()) {
                continue;
            }

            if (searchForSetContentView(layoutId, method)) {
                return entryClass;
            }
        }

        return null;
    }

    private boolean searchForSetContentView(int layoutId, SootMethod method) {
        PatchingChain<Unit> units = method.getActiveBody().getUnits();
        for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
            Unit unit = iterator.next();

            Status searchStatus = new Status();
            unit.apply(new AbstractStmtSwitch<Stmt>() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    super.caseInvokeStmt(stmt);

                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    if (invokeExpr.getMethod().getName().equals("setContentView")) {
                        if (invokeExpr.getArg(0).toString().equals(String.valueOf(layoutId))) {
                            searchStatus.foundClass(true);
                        }
                    }
                }
            });

            if (searchStatus.isClassFound()) {
                return true;
            }
        }

        return false;
    }

    private SootClass findOnCreateSetContentView(int layoutId, SootClass entryClass) {
        SootMethod onCreateMethod;
        try {
            onCreateMethod = entryClass.getMethodByNameUnsafe("onCreate");
        } catch (AmbiguousMethodException ignored) {
            return null;
        }

        if (onCreateMethod == null) {
            return null;
        }

        try {
            onCreateMethod.retrieveActiveBody();
        } catch (RuntimeException ignored) {
            return null;
        }

        if (searchForSetContentView(layoutId, onCreateMethod)) {
            return entryClass;
        }

        return null;
    }

    private SootClass findLayoutClassRecursively(int layoutId, SootClass entryClass, boolean onCreate) {
        SootClass layoutClass;
        if (onCreate) {
            layoutClass = findOnCreateSetContentView(layoutId, entryClass);
        } else {
            layoutClass = findAnySetContentView(layoutId, entryClass);
        }

        if (layoutClass == null && entryClass.hasSuperclass()) {
            layoutClass = findLayoutClassRecursively(layoutId, entryClass.getSuperclassUnsafe(), onCreate);
        }

        return layoutClass;
    }

    protected SootClass findLayoutClass(int layoutId) {
        for (SootClass entryClass : FlowDroidUtils.getEntryPointClasses(apk)) {
            SootClass layoutClass = findLayoutClassRecursively(layoutId, entryClass, true);

            if (layoutClass == null) {
                layoutClass = findLayoutClassRecursively(layoutId, entryClass, false);
            }

            if (layoutClass != null) {
                return layoutClass;
            }
        }

        return null;
    }

    private void addControlListeners(File collectedUiCallbackLinks) {
        if (!collectedUiCallbackLinks.exists()) {
            logger.error("Link File Does Not Exist!: " + collectedUiCallbackLinks);
        }

        String line;
        try {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

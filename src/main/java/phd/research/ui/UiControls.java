package phd.research.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FlowDroidUtils;
import phd.research.helper.Control;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public Control getControl(SootMethod callback) {
        for (Control control : this.getControls()) {
            if (control.getClickListener() != null) {
                if (control.getClickListener().equals(callback)) {
                    return control;
                }
            }
        }

        return null;
    }

    public Control getControl(String resourceName) {
        for (Control control : this.getControls()) {
            if (control.getControlResource() != null) {
                if (control.getControlResource().getResourceName().equals(resourceName)) {
                    return control;
                }
            }
        }

        return null;
    }

    public Control getControl(int resourceId) {
        for (Control control : this.getControls()) {
            if (control.getControlResource() != null) {
                if (control.getControlResource().getResourceID() == resourceId) {
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

            for (AndroidLayoutControl control : userControls.get(layoutFile)) {
                if (control.getID() == -1) {
                    continue;
                }

                ARSCFileParser.AbstractResource controlResource = UiControls.getResourceById(resources,
                        control.getID());
                if (controlResource == null) {
                    logger.error("No resource found with ID " + control.getID() + ".");
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

                uiControls.add(new Control(control.hashCode(), controlResource, layoutResource, clickListener));
            }
        }

        return uiControls;
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

    private void addControlListeners(File collectedUiCallbackLinks) {
        if (!collectedUiCallbackLinks.exists()) {
            logger.error("Link File Does Not Exist!: " + collectedUiCallbackLinks);
        }

        String line;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(collectedUiCallbackLinks));
            while ((line = bufferedReader.readLine()) != null) {
                String[] controlListenerPair = line.split(":");
                Control control = getControl(controlListenerPair[0]);
                if (control != null) {
                    SootMethod listener = Scene.v().getMethod(controlListenerPair[1]);
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

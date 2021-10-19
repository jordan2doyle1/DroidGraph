package phd.research.core;

import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import phd.research.enums.Type;
import phd.research.graph.ContentFilter;
import phd.research.helper.Control;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.callbacks.CallbackDefinition;

import java.io.IOException;
import java.util.*;

/**
 * @author Jordan Doyle
 */
public class InterfaceManager {

    private static final Logger logger = LoggerFactory.getLogger(InterfaceManager.class);

    private final Set<SootMethod> lifecycleMethods;
    private final Set<SootMethod> callbackMethods;
    private final Set<SootMethod> listenerMethods;
    private final Set<Control> controls;

    public InterfaceManager() {
        this.lifecycleMethods = new HashSet<>();
        this.callbackMethods = new HashSet<>();
        this.listenerMethods = new HashSet<>();
        this.controls = new HashSet<>();
    }

    public boolean isLifecycle(SootMethod method) {
        return this.lifecycleMethods.contains(method);
    }

    public int lifecycleCount() {
        return this.lifecycleMethods.size();
    }

    public Set<SootMethod> getCallbackMethods() {
        return this.callbackMethods;
    }

    public boolean isCallback(SootMethod method) {
        return this.callbackMethods.contains(method);
    }

    public int callbackCount() {
        return this.callbackMethods.size();
    }

    public boolean isListener(SootMethod method) {
        return this.listenerMethods.contains(method);
    }

    public int listenerCount() {
        return this.getCallbackMethods().size();
    }

    public int controlCount() {
        return this.controls.size();
    }

    public String getControlListenerTable() {
        StringBuilder stringBuilder = new StringBuilder();
        String separator = "--------------------------------------------------------------------------------\n";
        String stringFormat = "\t%-15s\t%-30s\t%-30s\t%-30s\n";

        if (this.controls.isEmpty()) {
            stringBuilder.append(separator).append("\tControl Listener Table is Empty!!!\n").append(separator);
        } else {
            stringBuilder.append(separator).append(
                    String.format(stringFormat, "WIDGET ID", "WIDGET TEXT ID", "LISTENER CLASS", "LISTENER METHOD")
            ).append(separator);

            for (Control control : this.controls) {
                Integer interfaceID = control.getId();
                String textID = control.getTextId();
                SootMethod listener = control.getClickListener();

                if (listener != null) {
                    stringBuilder.append(String.format(stringFormat, interfaceID, textID,
                            listener.getDeclaringClass().getShortName(), listener.getName())
                    );
                } else {
                    stringBuilder.append(String.format(stringFormat, interfaceID, null, null, null));
                }
            }
            stringBuilder.append(separator);
        }

        return stringBuilder.toString();
    }

    public Type getMethodType(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass"))
            return Type.dummyMethod;
        else if (isListener(method))
            return Type.listener;
        else if (isLifecycle(method))
            return Type.lifecycle;
        else if (isCallback(method))
            return Type.callback;
        else
            return Type.method;
    }

    public Control getControl(SootMethod callback) {
        for (Control control : this.controls) {
            if (control.getClickListener().equals(callback)) {
                return control;
            }
        }
        return null;
    }

    public Control getControl(String textId) {
        for (Control control : this.controls) {
            if (control.getTextId().equals(textId)) {
                return control;
            }
        }
        return null;
    }

    public void extractUI(SetupApplication app, ContentFilter filter) {
        Map<SootClass, Set<CallbackDefinition>> customCallbacks = new HashMap<>();

        for (Pair<SootClass, AndroidCallbackDefinition> callbacks : app.droidGraphCallbacks) {
            if (customCallbacks.containsKey(callbacks.getO1())) {
                customCallbacks.get(callbacks.getO1()).add(callbacks.getO2());
            } else {
                Set<CallbackDefinition> callbackDefinitions = new HashSet<>();
                callbackDefinitions.add(callbacks.getO2());
                customCallbacks.put(callbacks.getO1(), callbackDefinitions);
            }
        }

        logger.info("Retrieving callback methods and linking controls...");
        logger.info("FlowDroid found " + customCallbacks.size() + " classes with callbacks.");
        Set<SootMethod> controlCallbackSet = new HashSet<>();

        for (Map.Entry<SootClass, Set<CallbackDefinition>> entry : customCallbacks.entrySet()) {
            Set<CallbackDefinition> callbacks = entry.getValue();
            logger.info("FlowDroid found " + callbacks.size() + " callbacks in \"" + entry.getKey().getShortName() + "\"");

            for (CallbackDefinition callback : callbacks) {
                String targetName = callback.getTargetMethod().getName();
                if (targetName.contains("onCreate") || targetName.contains("onStart") || targetName.contains("onResume")
                        || targetName.contains("onRestart") || targetName.contains("onPause") ||
                        targetName.contains("onStop") || targetName.contains("onDestroy")) {
                    this.lifecycleMethods.add(callback.getTargetMethod());
                    logger.debug("Found lifecycle method \"" + callback.getTargetMethod().getName() + "\".");
                } else {
                    SootMethod method = callback.getTargetMethod();

                    if (filter.isValidPackage(method.getDeclaringClass().getPackageName())) {
                        this.listenerMethods.add(method);
                        logger.debug("Found listener method \"" + method.getName() + "\".");
                        controlCallbackSet.add(method);
                    } else {
                        this.callbackMethods.add(method);
                        logger.debug("Found callback method \"" + method.getName() + "\".");
                    }
                }
            }
        }

        logger.info("Found " + this.lifecycleMethods.size() + " lifecycle methods.");
        logger.info("Found " + this.callbackMethods.size() + " callback methods");
        logger.info("Found " + this.listenerMethods.size() + " listener methods");

        Set<Pair<String, AndroidLayoutControl>> nullControls = new HashSet<>();

        for (Pair<String, AndroidLayoutControl> userControl : retrieveLayoutFileParser().getUserControls()) {
            AndroidLayoutControl control = userControl.getO2();

            if (control.getClickListener() != null) {
                SootMethod clickListener = searchCallbackMethods(control.getClickListener());
                if (clickListener != null) {
                    this.controls.add(new Control(control.hashCode(), control.getID(), null, clickListener));
                    controlCallbackSet.remove(clickListener);
                    logger.debug(control.getID() + " linked to \"" + clickListener.getDeclaringClass().getShortName()
                            + "." + clickListener.getName() + "\".");
                } else {
                    logger.error("Problem linking controls with listeners: Two callback methods have the same name.");
                }
            } else {
                if (control.getID() != -1) {
                    nullControls.add(userControl);
                }
            }
        }

        logger.debug("Found " + nullControls.size() + " controls without listeners.");
        logger.debug("Found " + controlCallbackSet.size() + " unassigned listeners.");

        Iterator<SootMethod> iterator = controlCallbackSet.iterator();
        while (iterator.hasNext()) {
            SootMethod listener = iterator.next();
            phd.research.helper.Pair<String, Integer> interfaceID = getInterfaceID(listener);

            if (interfaceID != null) {
                Iterator<Pair<String, AndroidLayoutControl>> controlIterator = nullControls.iterator();
                while (controlIterator.hasNext()) {
                    AndroidLayoutControl control = controlIterator.next().getO2();

                    if (control.getID() == interfaceID.getRight()) {
                        this.controls.add(new Control(control.hashCode(), control.getID(), interfaceID.getLeft(),
                                listener));
                        controlIterator.remove();
                        iterator.remove();
                        logger.debug(control.getID() + ":" + interfaceID.getLeft() + " linked to \"" +
                                listener.getDeclaringClass().getShortName() + "." + listener.getName() + "\".");
                        break;
                    }
                }
            }
        }

        logger.debug(nullControls.size() + " controls without listeners remaining.");
        logger.debug(controlCallbackSet.size() + " unassigned listeners remaining.");

        if (controlCallbackSet.size() > 0) {
            for (SootMethod sootMethod : controlCallbackSet) {
                logger.error("No control linked to \"" + sootMethod.getDeclaringClass().getShortName() + "." +
                        sootMethod.getName() + "\".");
            }
        }

        if (nullControls.size() > 0) {
            for (Pair<String, AndroidLayoutControl> nullControl : nullControls) {
                AndroidLayoutControl control = nullControl.getO2();
                logger.error("No listener linked to " + control.getID() + ".");
            }
        }
    }

    private ARSCFileParser retrieveResources() {
        ARSCFileParser resources = new ARSCFileParser();
        try {
            resources.parse(FrameworkMain.getApk());
        } catch (IOException e) {
            logger.error("Error getting resources: " + e.getMessage());
        }

        return resources;
    }

    private LayoutFileParser retrieveLayoutFileParser() {
        LayoutFileParser layoutFileParser = null;
        try {
            ProcessManifest manifest = new ProcessManifest(FrameworkMain.getApk());
            layoutFileParser = new LayoutFileParser(manifest.getPackageName(), retrieveResources());
        } catch (IOException | XmlPullParserException e) {
            logger.error("Error getting LayoutFileParser: " + e.getMessage());
        }

        if (layoutFileParser != null) {
            layoutFileParser.parseLayoutFileDirect(FrameworkMain.getApk());
        }

        return layoutFileParser;
    }

    private SootMethod searchCallbackMethods(String methodName) {
        SootMethod foundMethod = null;

        for (SootMethod method : this.listenerMethods) {
            if (method.getName().equals(methodName)) {
                if (foundMethod != null) {
                    logger.error("Found multiple callback methods with the same name: " + methodName + ".");
                    return null;
                }
                foundMethod = method;
            }
        }
        return foundMethod;
    }

    private phd.research.helper.Pair<String, Integer> getInterfaceID(final SootMethod callback) {
        PatchingChain<Unit> units = callback.getActiveBody().getUnits();
        final String[] arguments = {""};
        phd.research.helper.Pair<String, Integer> interfaceID = null;

        for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
            Unit unit = iterator.next();

            unit.apply(new AbstractStmtSwitch() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    super.caseInvokeStmt(stmt);

                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    if (invokeExpr.getMethod().getName().equals("println")) {
                        arguments[0] = invokeExpr.getArg(0).toString();
                    }
                }
            });
        }

        String argument = arguments[0].replace("\"", "");
        String textID = null;
        String numberID = null;

        if ((argument.contains(":"))) {
            String[] id = argument.split(":");
            textID = "id/" + id[0];
            numberID = id[1];
        } else if (! argument.equals("")) {
            numberID = argument;
        }

        if (numberID == null) {
            logger.error("Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "."
                    + callback.getName() + "\", no value found in the method. ");
            return null;
        }

        try {
            interfaceID = new phd.research.helper.Pair<>(textID, Integer.parseInt(numberID));
            logger.info("Found control ID " + argument + " in \"" + callback.getDeclaringClass().getShortName() + "." +
                    callback.getName() + "\".");
        } catch (NumberFormatException e) {
            logger.error("Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "."
                    + callback.getName() + "\", could not parse integer: " + numberID + ".");
        }

        return interfaceID;
    }
}

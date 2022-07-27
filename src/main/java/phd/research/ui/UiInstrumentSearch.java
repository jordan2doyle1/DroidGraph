package phd.research.ui;

import heros.solver.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FlowDroidUtils;
import phd.research.helper.Control;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("unused")
@Deprecated
public class UiInstrumentSearch extends UiSearch {

    private static final Logger logger = LoggerFactory.getLogger(UiInstrumentSearch.class);

    public UiInstrumentSearch(String apk) {
        super(apk);
    }

    private Pair<String, Integer> getInterfaceId(final SootMethod callback) {
        PatchingChain<Unit> units = callback.getActiveBody().getUnits();
        final String[] arguments = {""};
        Pair<String, Integer> interfaceID = null;

        for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
            Unit unit = iterator.next();

            unit.apply(new AbstractStmtSwitch<Stmt>() {
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
        } else if (!argument.equals("")) {
            numberID = argument;
        }

        if (numberID == null) {
            logger.error("Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "." +
                    callback.getName() + "\", no value found in the method. ");
            return null;
        }

        try {
            interfaceID = new Pair<>(textID, Integer.parseInt(numberID));
            logger.info("Found control ID " + argument + " in \"" + callback.getDeclaringClass().getShortName() + "." +
                    callback.getName() + "\".");
        } catch (NumberFormatException e) {
            logger.error("Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "." +
                    callback.getName() + "\", could not parse integer: " + numberID + ".");
        }

        return interfaceID;
    }


    public Set<Control> getControlListenerMethods(File collectedCallbacksFile) {
        Set<SootMethod> callbackMethods = new HashSet<>();
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(collectedCallbacksFile);
        for (SootClass sootClass : callbacks.getCallbackMethods().keySet()) {
            for (AndroidCallbackDefinition callbackDefinition : callbacks.getCallbackMethods().get(sootClass)) {
                callbackMethods.add(callbackDefinition.getTargetMethod());
            }
        }

        Set<Control> controls = new HashSet<>();
        Set<Pair<String, AndroidLayoutControl>> nullControls = new HashSet<>();
        LayoutFileParser lfp = FlowDroidUtils.getLayoutFileParser(new File(super.apk));
        if (lfp != null) {
            for (Pair<String, AndroidLayoutControl> userControl : lfp.getUserControls()) {
                AndroidLayoutControl control = userControl.getO2();
                if (control.getClickListener() != null) {
                    SootMethod clickListener = UiControls.searchForCallbackMethod(collectedCallbacksFile,
                            control.getClickListener());
                    if (clickListener != null) {
                        // Below null values will cause NullPointerException. Control class has changed and no longer
                        // works with this outdated code. Commented out to remove warnings.
                        // controls.add(new Control(null, null, null, clickListener));
                        callbackMethods.remove(clickListener);
                        logger.info(
                                control.getID() + " linked to \"" + clickListener.getDeclaringClass().getShortName() +
                                        "." + clickListener.getName() + "\".");
                    } else {
                        logger.error(
                                "Problem linking controls with listeners: Two callback methods have the same name.");
                    }
                } else {
                    if (control.getID() != -1) {
                        nullControls.add(userControl);
                    }
                }
            }
        }

        Iterator<SootMethod> iterator = callbackMethods.iterator();
        while (iterator.hasNext()) {
            SootMethod listener = iterator.next();
            Pair<String, Integer> interfaceID = getInterfaceId(listener);

            if (interfaceID != null) {
                Iterator<Pair<String, AndroidLayoutControl>> controlIterator = nullControls.iterator();
                while (controlIterator.hasNext()) {
                    AndroidLayoutControl control = controlIterator.next().getO2();

                    if (control.getID() == interfaceID.getO2()) {
                        // Below null values will cause NullPointerException. Control class has changed and no longer
                        // works with this outdated code. Commented out to remove warnings.
                        // controls.add(new Control(null, null, null, listener));
                        controlIterator.remove();
                        iterator.remove();
                        logger.info(control.getID() + ":" + interfaceID.getO1() + " linked to \"" +
                                listener.getDeclaringClass().getShortName() + "." + listener.getName() + "\".");
                        break;
                    }
                }
            }
        }

        if (callbackMethods.size() > 0) {
            for (SootMethod sootMethod : callbackMethods) {
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

        return controls;
    }
}

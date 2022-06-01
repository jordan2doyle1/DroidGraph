package phd.research.ui;

import heros.solver.Pair;
import phd.research.core.FlowDroidUtils;
import phd.research.core.FrameworkMain;
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

public class UiInstrumentSearch extends UiSearch {

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
            System.err.println(
                    "Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "." +
                            callback.getName() + "\", no value found in the method. ");
            return null;
        }

        try {
            interfaceID = new Pair<>(textID, Integer.parseInt(numberID));
            System.out.println(
                    "Found control ID " + argument + " in \"" + callback.getDeclaringClass().getShortName() + "." +
                            callback.getName() + "\".");
        } catch (NumberFormatException e) {
            System.out.println(
                    "Failed to get Interface ID from \"" + callback.getDeclaringClass().getShortName() + "." +
                            callback.getName() + "\", could not parse integer: " + numberID + ".");
        }

        return interfaceID;
    }


    public Set<Control> getControlListenerMethods() {
        Set<SootMethod> callbackMethods = new HashSet<>();
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(
                new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"));
        for (SootClass sootClass : callbacks.getCallbackMethods().keySet()) {
            for (AndroidCallbackDefinition callbackDefinition : callbacks.getCallbackMethods().get(sootClass)) {
                callbackMethods.add(callbackDefinition.getTargetMethod());
            }
        }

        Set<Control> controls = new HashSet<>();
        Set<Pair<String, AndroidLayoutControl>> nullControls = new HashSet<>();
        LayoutFileParser lfp = FlowDroidUtils.getLayoutFileParser(super.apk);
        if (lfp != null) {
            for (Pair<String, AndroidLayoutControl> userControl : lfp.getUserControls()) {
                AndroidLayoutControl control = userControl.getO2();
                if (control.getClickListener() != null) {
                    SootMethod clickListener = searchForCallbackMethod(control.getClickListener());
                    if (clickListener != null) {
                        controls.add(new Control(control.hashCode(), null, null, clickListener));
                        callbackMethods.remove(clickListener);
                        System.out.println(
                                control.getID() + " linked to \"" + clickListener.getDeclaringClass().getShortName() +
                                        "." + clickListener.getName() + "\".");
                    } else {
                        System.err.println(
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
                        controls.add(new Control(control.hashCode(), null, null, listener));
                        controlIterator.remove();
                        iterator.remove();
                        System.out.println(control.getID() + ":" + interfaceID.getO1() + " linked to \"" +
                                listener.getDeclaringClass().getShortName() + "." + listener.getName() + "\".");
                        break;
                    }
                }
            }
        }

        if (callbackMethods.size() > 0) {
            for (SootMethod sootMethod : callbackMethods) {
                System.err.println("No control linked to \"" + sootMethod.getDeclaringClass().getShortName() + "." +
                        sootMethod.getName() + "\".");
            }
        }

        if (nullControls.size() > 0) {
            for (Pair<String, AndroidLayoutControl> nullControl : nullControls) {
                AndroidLayoutControl control = nullControl.getO2();
                System.err.println("No listener linked to " + control.getID() + ".");
            }
        }

        return controls;
    }
}

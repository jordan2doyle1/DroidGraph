package phd.research.ui;

import phd.research.core.FlowDroidUtils;
import phd.research.core.FrameworkMain;
import phd.research.graph.Filter;
import phd.research.helper.Control;
import phd.research.helper.Status;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("unused")
public class UiJimpleSearch extends UiSearch {

    public UiJimpleSearch(String apk) {
        super(apk);
    }

    public static void getValueFromGetIdMethod() {
        // For Testing Purposes Only. Trying to get value from getId() method in static Jimple.
        SootClass sc = Scene.v().getSootClass("com.example.android.lifecycle.ActivityA$1");
        for (SootMethod method : sc.getMethods()) {
            if (method.getName().contains("onClick")) {
                if (method.hasActiveBody()) {
                    PatchingChain<Unit> units = method.getActiveBody().getUnits();
                    for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
                        Unit unit = iterator.next();
                        unit.apply(new AbstractStmtSwitch<Stmt>() {
                            @Override
                            public void caseAssignStmt(AssignStmt stmt) {
                                super.caseAssignStmt(stmt);

                                // Get the left and right operand, if the right operand is a virtual invoke with name.
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("getId")) {
                                    System.out.println(unit.getUseBoxes());
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private SootMethod searchForCallbackMethod(SootClass callbackClass, String methodName) {
        SootMethod foundMethod = null;

        for (SootMethod method : callbackClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                if (foundMethod != null) {
                    System.err.println(
                            "Multiple callbacks with the name " + methodName + " in class " + callbackClass + "."
                    );
                    return null;
                }
                foundMethod = method;
            }
        }

        return foundMethod;
    }

    private SootMethod findCallbackMethodAnywhere(int id) {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (Filter.isValidClass(null, null, sootClass)
                    && !Filter.isEntryPointClass(FrameworkMain.getApk(), sootClass)) {
                SootMethod callbackMethod = findCallbackMethod(sootClass, id);
                if (callbackMethod != null) return callbackMethod;
            }
        }

        return null;
    }

    private SootMethod findCallbackMethodInEntryClass(int id) {
        for (SootClass sootClass : FlowDroidUtils.getEntryPointClasses(apk)) {
            SootMethod callbackMethod = findCallbackMethod(sootClass, id);
            if (callbackMethod != null) {
                return callbackMethod;
            }
        }

        return null;
    }

    private SootMethod findCallbackMethod(SootClass callbackClass, int id) {
        for (SootMethod method : callbackClass.getMethods()) {
            if (!method.hasActiveBody()) {
                try {
                    method.retrieveActiveBody();
                } catch (RuntimeException ignored) {
                    continue;
                }
            }

            if (method.hasActiveBody()) {
                Status searchStatus = new Status();
                PatchingChain<Unit> units = method.getActiveBody().getUnits();

                for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
                    Unit unit = iterator.next();

                    unit.apply(new AbstractStmtSwitch<Stmt>() {
                        @Override
                        public void caseAssignStmt(AssignStmt stmt) {
                            super.caseAssignStmt(stmt);

                            if (stmt.containsInvokeExpr()) {
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("findViewById")) {
                                    int intArg = -1;
                                    try {
                                        intArg = Integer.parseInt(invokeExpr.getArg(0).toString());
                                    } catch (NumberFormatException ignored) {
                                        System.err.println("Error: findViewByID() has unknown argument! Stmt: " + stmt);
                                    }

                                    if (intArg != -1 && intArg == id) {
                                        searchStatus.foundView(true);
                                    }
                                }
                            }
                        }

                        @Override
                        public void caseInvokeStmt(InvokeStmt stmt) {
                            super.caseInvokeStmt(stmt);

                            if (searchStatus.isViewFound()) {
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("<init>")) {
                                    searchStatus.foundClass(true);
                                    searchStatus.setFoundClass(invokeExpr.getMethod().getDeclaringClass());
                                }
                            }
                        }
                    });

                    if (searchStatus.isViewFound() && searchStatus.isClassFound()) {
                        if (searchStatus.getFoundClass().getMethodCount() > 2)
                            System.out.println(
                                    "Warning: Class contains multiple callback methods. Using the first method."
                            );

                        for (SootMethod classMethod : searchStatus.getFoundClass().getMethods()) {
                            if (!classMethod.getName().equals("<init>")) return classMethod;
                        }
                    }
                }
            }
        }

        return null;
    }

    public Set<Control> getControlListenerMethods() {
        LayoutFileParser layoutParser = FlowDroidUtils.getLayoutFileParser(super.apk);

        Set<Control> uiControls = new HashSet<>();
        MultiMap<String, AndroidLayoutControl> userControls;
        if (layoutParser != null) userControls = layoutParser.getUserControls();
        else {
            System.err.println("Error: Problem getting Layout File Parser. Can't get UI Controls!");
            return uiControls;
        }

        for (String layoutFile : userControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource = resources.findResourceByName(
                    "layout", getResourceName(layoutFile)
            );

            for (AndroidLayoutControl control : userControls.get(layoutFile)) {
                if (control.getID() == -1) continue;

                ARSCFileParser.AbstractResource controlResource = super.getResourceById(control.getID());
                if (controlResource == null) {
                    System.err.println("Error: No resource found with ID " + control.getID() + ".");
                    continue;
                }

                SootClass callbackClass = super.findLayoutClass(layoutResource.getResourceID());
                if (callbackClass == null) {
                    System.err.println("Error: No class found for layout resource: " + layoutResource.getResourceID());
                    continue;
                }

                SootMethod clickListener;
                if (control.getClickListener() != null)
                    clickListener = searchForCallbackMethod(callbackClass, control.getClickListener());
                else {
                    if ((clickListener = findCallbackMethod(callbackClass, control.getID())) == null) {
                        if ((clickListener = findCallbackMethodInEntryClass(control.getID())) == null)
                            clickListener = findCallbackMethodAnywhere(control.getID());
                    }
                }

                if (clickListener == null)
                    System.err.println("Error: Couldn't find click listener method with ID: " + control.getID());

                uiControls.add(new Control(control.hashCode(), controlResource, layoutResource, clickListener));
            }
        }

        return uiControls;
    }
}

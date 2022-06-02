package phd.research.ui;

import phd.research.core.FlowDroidUtils;
import phd.research.helper.Status;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Iterator;

public class UiSearch {

    protected String apk;
    protected ARSCFileParser resources;

    public UiSearch(String apk) {
        this.apk = apk;
        this.resources = FlowDroidUtils.getResources(this.apk);
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
}

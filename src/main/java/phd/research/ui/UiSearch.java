package phd.research.ui;

import phd.research.core.FlowDroidUtils;
import phd.research.core.FrameworkMain;
import phd.research.helper.Status;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class UiSearch {

    protected String apk;
    protected ARSCFileParser resources;

    public UiSearch(String apk) {
        this.apk = apk;
        this.resources = FlowDroidUtils.getResources(this.apk);
    }

    protected static String getResourceName(String name) {
        int index = name.lastIndexOf("/");

        if (index != -1) name = name.replace(name.substring(0, index + 1), "");

        if (name.contains(".xml")) name = name.replace(".xml", "");

        return name;
    }

    protected ARSCFileParser.AbstractResource getResourceById(int resourceId) {
        ARSCFileParser.ResType resType = resources.findResourceType(resourceId);
        if (resType == null) return null;

        List<ARSCFileParser.AbstractResource> foundResources = resType.getAllResources(resourceId);
        if (foundResources.isEmpty()) return null;

        if (foundResources.size() > 1)
            System.err.println("Error: Multiple resources with ID " + resourceId + ", returning the first.");

        return foundResources.get(0);
    }

    protected SootMethod searchForCallbackMethod(String methodName) {
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(
                new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks")
        );
        SootMethod foundMethod = null;

        for (SootClass currentClass : callbacks.getCallbackMethods().keySet()) {
            for (AndroidCallbackDefinition callbackDefinition : callbacks.getCallbackMethods().get(currentClass)) {
                if (callbackDefinition.getTargetMethod().getName().equals(methodName)) {
                    if (foundMethod != null) {
                        System.err.println("Multiple callbacks with the name " + methodName + ".");
                        return null;
                    }
                    foundMethod = callbackDefinition.getTargetMethod();
                }
            }
        }

        return foundMethod;
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

            if (searchStatus.isClassFound()) return true;
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

        if (onCreateMethod == null) return null;

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
        if (onCreate) layoutClass = findOnCreateSetContentView(layoutId, entryClass);
        else layoutClass = findAnySetContentView(layoutId, entryClass);

        if (layoutClass == null && entryClass.hasSuperclass()) {
            layoutClass = findLayoutClassRecursively(layoutId, entryClass.getSuperclassUnsafe(), onCreate);
        }

        return layoutClass;
    }

    protected SootClass findLayoutClass(int layoutId) {
        for (SootClass entryClass : FlowDroidUtils.getEntryPointClasses(apk)) {
            SootClass layoutClass = findLayoutClassRecursively(layoutId, entryClass, true);

            if (layoutClass == null) layoutClass = findLayoutClassRecursively(layoutId, entryClass, false);

            if (layoutClass != null) return layoutClass;
        }

        return null;
    }
}

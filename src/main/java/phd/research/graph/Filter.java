package phd.research.graph;

import phd.research.core.FrameworkMain;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.util.MultiMap;

/**
 * @author Jordan Doyle
 */
public class Filter {

    // TODO: Need isLifecycle, isListener, isOtherCallback.

    public Filter() {
    }

    public boolean isValidClass(SootClass sootClass) {
        if (sootClass.isJavaLibraryClass() || sootClass.isLibraryClass() || sootClass.isPhantomClass()) {
            return false;
        }

        if (!isValidPackage(sootClass.getPackageName())) {
            return false;
        }

        for (String blacklistClass : FrameworkMain.getClassBlacklist()) {
            if (sootClass.getShortName().contains(blacklistClass))
                return false;
        }

        return true;
    }

    public boolean isValidMethod(SootMethod method) {
        return isValidClass(method.getDeclaringClass());
    }

    public boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public boolean isLifecycleMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        boolean confirmLifecycle  = isLifecycleMethod(method);
        boolean isLifecycle = false;

        SootClass methodClass;
        if (method.getDeclaringClass().hasOuterClass()) {
            methodClass = method.getDeclaringClass().getOuterClass();
        } else {
            methodClass = method.getDeclaringClass();
        }

        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                if (callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Default) {
                    isLifecycle = true;
                }
            }
        }

        if ((confirmLifecycle && !isLifecycle) || (!confirmLifecycle && isLifecycle)) {
            System.out.println("Inconsistent - Confirm: " + confirmLifecycle + " Is: " + isLifecycle);
        }

        return isLifecycle;
    }

    public boolean isCallbackMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        // TODO: Method not working. Something wrong with getDeclaringClass.
        SootClass methodClass;
        if (method.getDeclaringClass().hasOuterClass()) {
            methodClass = method.getDeclaringClass().getOuterClass();
        } else {
            methodClass = method.getDeclaringClass();
        }

        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                System.out.println(method + ": " + callbackDefinition.getCallbackType());
                System.out.println(method + ": " + callbackDefinition.getParentMethod());
                return true;
            }
        }
        return false;
    }

    public boolean isListenerMethod(SootMethod method) {
        // TODO: Confirm that this method works?
        boolean isListener = method.getDeclaringClass().getName().startsWith("android.widget") ||
                method.getDeclaringClass().getName().startsWith("android.view") ||
                method.getDeclaringClass().getName().startsWith("android.content.DialogInterface$");

        // System.out.println( + method + ": " + isListener);

        return method.getDeclaringClass().getName().startsWith("android.widget") ||
                method.getDeclaringClass().getName().startsWith("android.view") ||
                method.getDeclaringClass().getName().startsWith("android.content.DialogInterface$");
    }



    private boolean isValidPackage(String packageName) {
        for (String blacklistPackage : FrameworkMain.getPackageBlacklist()) {
            if (blacklistPackage.startsWith(".")) {
                if (packageName.contains(blacklistPackage))
                    return false;
            } else {
                if (packageName.startsWith(blacklistPackage))
                    return false;
            }
        }
        return true;
    }
}

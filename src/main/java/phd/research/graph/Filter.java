package phd.research.graph;

import phd.research.core.FrameworkMain;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
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

    public boolean isOtherCallbackMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (isLifecycleMethod(method)) {
            return false;
        }

        if (isListenerMethod(callbacks, method)) {
            return false;
        }

        SootClass methodClass = getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                return true;
            }
        }

        return false;
    }

    public boolean isCallbackMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        // TODO: Method not working. Something wrong with getDeclaringClass.
        SootClass methodClass = getParentClass(method);

//        for (SootClass sootClass : callbacks.keySet()) {
//            for (AndroidCallbackDefinition callbackDefinition : callbacks.get(sootClass)) {
//                SootMethod currMethod = callbackDefinition.getTargetMethod();
//                if(!isLifecycleMethod(currMethod) && !isListenerMethod(callbacks, currMethod)) {
//                    if (!method.toString().startsWith("<androidx")) {
//                        System.out.println("Leftover: " + currMethod);
//                    }
//                }
//            }
//        }

        if (isOtherCallbackMethod(callbacks, method)) {
            System.out.println("Leftover: " + method);
        }

        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                // System.out.println(method + ": " + callbackDefinition.getCallbackType());
                // System.out.println(method + ": " + callbackDefinition.getParentMethod());
                return true;
            }
        }
        return false;
    }

    public boolean isListenerMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        SootClass methodClass = getParentClass(method);

        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                if (callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                    return true;
                }
            }
        }

        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                if (callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                    return true;
                }
            }
        }

        if (method.getName().contains("onClick")) {
            if (!method.toString().startsWith("<androidx")) {
                System.out.println("Failed to identify listener (1) - " + method);
            }
        }

        for (Type paramType : method.getParameterTypes()) {
            if (paramType.toString().equals("android.view.View")) {
                if (!method.toString().startsWith("<androidx")) {
                    System.out.println("Failed to identify listener (2) - " + method);
                }
            }

        }

        // "android.widget" "android.view" "android.content.DialogInterface$"

        return false;
    }

    private SootClass getParentClass(SootMethod method) {
        if (method.getDeclaringClass().hasOuterClass()) {
            return method.getDeclaringClass().getOuterClass();
        } else {
            return method.getDeclaringClass();
        }
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

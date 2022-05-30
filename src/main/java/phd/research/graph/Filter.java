package phd.research.graph;

import phd.research.core.FlowDroidUtils;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.util.MultiMap;

import java.io.File;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class Filter {

    public Filter() {

    }

    public static boolean isEntryPointClass(String apk, SootClass sootClass) {
        for (SootClass entryClass : FlowDroidUtils.getEntryPointClasses(apk)) {
            if (entryClass.equals(sootClass)) return true;
        }

        return false;
    }

    public static boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public static boolean isListenerMethod(File callbackFile, SootMethod method) {
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(callbackFile);
        if (callbacks != null) return Filter.isListenerMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    public static boolean isListenerMethod(
            MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        SootClass methodClass = getParentClass(method);

        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method)) {
                if (callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) return true;
            }
        }

        return false;
    }

    public static boolean isOtherCallbackMethod(File callbackFile, SootMethod method) {
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(callbackFile);
        if (callbacks != null) return Filter.isOtherCallbackMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    public static boolean isOtherCallbackMethod(
            MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(callbacks, method)) {
            SootClass methodClass = Filter.getParentClass(method);
            for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
                if (callbackDefinition.getTargetMethod().equals(method)) return true;
            }
        }

        return false;
    }

    public static boolean isPossibleListenerMethod(File callbackFile, SootMethod method) {
        CollectedCallbacks callbacks = FlowDroidUtils.readCollectedCallbacks(callbackFile);
        if (callbacks != null) return Filter.isPossibleListenerMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    public static boolean isPossibleListenerMethod(
            MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(callbacks, method)
                && !Filter.isOtherCallbackMethod(callbacks, method)) {
            for (Type paramType : method.getParameterTypes()) {
                if (paramType.toString().equals("android.view.View")) {
                    if ((!method.toString().startsWith("<androidx"))) return true;
                }
            }
        }

        return false;
    }

    private static SootClass getParentClass(SootMethod method) {
        if (method.getDeclaringClass().hasOuterClass()) return method.getDeclaringClass().getOuterClass();
        else return method.getDeclaringClass();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isValidPackage(Set<String> blacklist, String packageName) {
        if (blacklist == null) {
            return true;
        }

        for (String blacklistPackage : blacklist) {
            if (blacklistPackage.startsWith(".")) {
                if (packageName.contains(blacklistPackage)) return false;
            } else {
                if (packageName.startsWith(blacklistPackage)) return false;
            }
        }

        return true;
    }

    public static boolean isValidClass(Set<String> packageBlacklist, Set<String> classBlacklist, SootClass sootClass) {
        if (SystemClassHandler.v().isClassInSystemPackage(sootClass) || sootClass.isJavaLibraryClass()
                || sootClass.isLibraryClass() || sootClass.isPhantomClass())
            return false;

        if (packageBlacklist != null) {
            if (!isValidPackage(packageBlacklist, sootClass.getPackageName())) return false;
        }

        if (classBlacklist != null) {
            for (String blacklistClass : classBlacklist) {
                if (sootClass.getShortName().contains(blacklistClass)) return false;
            }
        }

        return true;
    }

    public static boolean isValidMethod(Set<String> packageBlacklist, Set<String> classBlacklist, SootMethod method) {
        return Filter.isValidClass(packageBlacklist, classBlacklist, method.getDeclaringClass());
    }
}

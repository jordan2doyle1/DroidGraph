package phd.research.graph;

import phd.research.core.ApplicationAnalysis;
import phd.research.core.FrameworkMain;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.util.MultiMap;

/**
 * @author Jordan Doyle
 */
public class Filter {

    public Filter() {

    }

    public static boolean isValidClass(SootClass sootClass) {
        if (SystemClassHandler.v().isClassInSystemPackage(sootClass) || sootClass.isJavaLibraryClass()
                || !Filter.isValidPackage(sootClass.getPackageName()) || sootClass.isLibraryClass() ||
                sootClass.isPhantomClass())
            return false;

        for (String blacklistClass : FrameworkMain.getClassBlacklist()) {
            if (sootClass.getShortName().contains(blacklistClass))
                return false;
        }

        return true;
    }

    public static boolean isValidMethod(SootMethod method) {
        return Filter.isValidClass(method.getDeclaringClass());
    }

    public static boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public static boolean isListenerMethod(
            MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        SootClass methodClass = getParentClass(method);

        if (callbacks != null) {
            for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
                if (callbackDefinition.getTargetMethod().equals(method)) {
                    if (callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget)
                        return true;
                }
            }
        }

        return false;
    }

    public static boolean isListenerMethod(SootMethod method) {
        CollectedCallbacks callbacks = ApplicationAnalysis.getCallbacks();
        if (callbacks != null)
            return Filter.isListenerMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    @SuppressWarnings("unused")
    private static boolean isListener(SootMethod method) {
        // TODO: Why is one fragment listener method left out?
        // TODO: How does FlowDroid recognise listener methods and can I replicate it here?
        return false;
    }

    public static boolean isOtherCallbackMethod(
            MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (Filter.isLifecycleMethod(method))
            return false;

        if (Filter.isListenerMethod(method))
            return false;

        SootClass methodClass = Filter.getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method))
                return true;
        }

        return false;
    }

    public static boolean isOtherCallbackMethod(SootMethod method) {
        CollectedCallbacks callbacks = ApplicationAnalysis.getCallbacks();
        if (callbacks != null)
            return Filter.isOtherCallbackMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    private static SootClass getParentClass(SootMethod method) {
        if (method.getDeclaringClass().hasOuterClass())
            return method.getDeclaringClass().getOuterClass();
        else
            return method.getDeclaringClass();
    }

    private static boolean isValidPackage(String packageName) {
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

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

    public boolean isValidClass(SootClass sootClass) {
        if (SystemClassHandler.v().isClassInSystemPackage(sootClass) || !isValidPackage(sootClass.getPackageName())
                || sootClass.isJavaLibraryClass() || sootClass.isLibraryClass() || sootClass.isPhantomClass())
            return false;

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

    public boolean isListenerMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
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

    public boolean isListenerMethod(SootMethod method) {
        CollectedCallbacks callbacks = ApplicationAnalysis.getCallbacks();
        if (callbacks != null)
            return isListenerMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    @SuppressWarnings("unused")
    private boolean isListener(SootMethod method) {
        // TODO: Why is one fragment listener method left out?
        // TODO: How does FlowDroid recognise listener methods and can I replicate it here?
        return false;
    }

    public boolean isOtherCallbackMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (isLifecycleMethod(method))
            return false;

        if (isListenerMethod(method))
            return false;

        SootClass methodClass = getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
            if (callbackDefinition.getTargetMethod().equals(method))
                return true;
        }

        return false;
    }

    public boolean isOtherCallbackMethod(SootMethod method) {
        CollectedCallbacks callbacks = ApplicationAnalysis.getCallbacks();
        if (callbacks != null)
            return isOtherCallbackMethod(callbacks.getCallbackMethods(), method);

        return false;
    }

    private SootClass getParentClass(SootMethod method) {
        if (method.getDeclaringClass().hasOuterClass())
            return method.getDeclaringClass().getOuterClass();
        else
            return method.getDeclaringClass();
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

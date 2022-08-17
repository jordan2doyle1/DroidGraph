package phd.research.graph;

import org.xmlpull.v1.XmlPullParserException;
import phd.research.core.FlowDroidUtils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.util.MultiMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Jordan Doyle
 */

public class Filter {

    public static boolean isEntryPointClass(File apk, SootClass clazz) throws XmlPullParserException, IOException {
        return FlowDroidUtils.getEntryPointClasses(apk).stream().anyMatch(e -> e.equals(clazz));
    }

    public static boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public static boolean isListenerMethod(File callbackFile, SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        return Filter.isListenerMethod(callbacks.getCallbackMethods(), method);
    }

    public static boolean isListenerMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        SootClass parentClass = getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(parentClass)) {
            if (callbackDefinition.getTargetMethod().equals(method) &&
                    callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                    return true;
            }
        }
        return false;
    }

    public static boolean isOtherCallbackMethod(File callbackFile, SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        return Filter.isOtherCallbackMethod(callbacks.getCallbackMethods(), method);
    }

    public static boolean isOtherCallbackMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(callbacks, method)) {
            SootClass methodClass = Filter.getParentClass(method);
            for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
                if (callbackDefinition.getTargetMethod().equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPossibleListenerMethod(File callbackFile, SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        return Filter.isPossibleListenerMethod(callbacks.getCallbackMethods(), method);
    }

    public static boolean isPossibleListenerMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks, SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(callbacks, method) && !Filter.isOtherCallbackMethod(callbacks, method)) {
            if ((!method.toString().startsWith("<androidx"))) {
                for (Type paramType : method.getParameterTypes()) {
                    if (paramType.toString().equals("android.view.View")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isValidClass(SootClass clazz) {
        return Filter.isValidClass(null, null, clazz);
    }

    public static boolean isValidClass(Collection<String> packageBlacklist, Collection<String> classBlacklist,
            SootClass clazz) {
        if (SystemClassHandler.v().isClassInSystemPackage(clazz) || clazz.isJavaLibraryClass() ||
                clazz.isLibraryClass() || clazz.isPhantomClass() || Scene.v().isExcluded(clazz)) {
            return false;
        }

        if (packageBlacklist != null) {
            if (!isValidPackage(packageBlacklist, clazz.getPackageName())) {
                return false;
            }
        }

        if (classBlacklist != null) {
            for (String blacklistClass : classBlacklist) {
                if (clazz.getShortName().contains(blacklistClass)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isValidMethod(SootMethod method) {
        return Filter.isValidMethod(null, null, method);
    }

    public static boolean isValidMethod(Collection<String> packageBlacklist, Collection<String> classBlacklist,
            SootMethod method) {
        return Filter.isValidClass(packageBlacklist, classBlacklist, method.getDeclaringClass());
    }

    private static boolean isValidPackage(Collection<String> blacklist, String packageName) {
        if (blacklist == null) {
            return true;
        }

        for (String blacklistPackage : blacklist) {
            if (blacklistPackage.startsWith(".")) {
                if (packageName.contains(blacklistPackage)) {
                    return false;
                }
            } else {
                if (packageName.startsWith(blacklistPackage)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static SootClass getParentClass(SootMethod method) {
        SootClass clazz = method.getDeclaringClass();
        return (clazz.hasOuterClass() ? clazz.getOuterClass() : clazz);
    }
}

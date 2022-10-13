package phd.research.graph;

import phd.research.core.FlowDroidAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.util.SystemClassHandler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class Filter {

    private static final List<String> PACKAGE_BLACKLIST = Filter.loadBlacklist("package_blacklist");
    private static final List<String> CLASS_BLACKLIST = Filter.loadBlacklist("class_blacklist");
    private static final List<String> LAYOUT_BLACKLIST = Filter.loadBlacklist("layout_blacklist");
    private static final CollectedCallbacks COLLECTED_CALLBACKS = Filter.deserializeCallbacks();

    public static boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public static boolean isListenerMethod(SootMethod method) {
        SootClass parentClass = getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : COLLECTED_CALLBACKS.getCallbackMethods().get(parentClass)) {
            if (callbackDefinition.getTargetMethod().equals(method) &&
                    callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOtherCallbackMethod(SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(method)) {
            SootClass clazz = Filter.getParentClass(method);
            for (AndroidCallbackDefinition callbackDefinition : COLLECTED_CALLBACKS.getCallbackMethods().get(clazz)) {
                if (callbackDefinition.getTargetMethod().equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPossibleListenerMethod(SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(method) &&
                !Filter.isOtherCallbackMethod(method) && Filter.isValidMethod(method)) {
            return method.getParameterTypes().stream().anyMatch(type -> type.toString().equals("android.view.View"));
        }
        return false;
    }

    public static Collection<SootMethod> findCallbackMethods(String methodName) {
        // Warning: if method name is not specific enough, false positives may be returned. e.g. methodName = onClick()
        Collection<SootMethod> methods = new ArrayList<>();
        COLLECTED_CALLBACKS.getCallbackMethods().keySet().forEach(
                clazz -> COLLECTED_CALLBACKS.getCallbackMethods().get(clazz).stream()
                        .filter(definition -> definition.getTargetMethod().getName().equals(methodName))
                        .forEach(callback -> methods.add(callback.getTargetMethod())));
        return methods;
    }

    public static boolean isValidLayout(String layout) {
        return !LAYOUT_BLACKLIST.contains(layout);
    }

    public static boolean isValidMethod(SootMethod method) {
        return Filter.isValidClass(method.getDeclaringClass());
    }

    public static boolean isValidClass(SootClass clazz) {
        if (SystemClassHandler.v().isClassInSystemPackage(clazz) || clazz.isJavaLibraryClass() ||
                clazz.isLibraryClass() || clazz.isPhantomClass() || Scene.v().isExcluded(clazz) ||
                !isValidPackage(clazz.getPackageName())) {
            return false;
        }

        return CLASS_BLACKLIST.stream().noneMatch(blacklistedClass -> clazz.getShortName().contains(blacklistedClass));
    }

    private static boolean isValidPackage(String packageName) {
        return PACKAGE_BLACKLIST.stream().noneMatch(
                blacklistedPackage -> blacklistedPackage.startsWith(".") ? packageName.contains(blacklistedPackage) :
                        packageName.startsWith(blacklistedPackage));
    }

    private static List<String> loadBlacklist(String fileName) {
        InputStream resourceStream = Filter.class.getClassLoader().getResourceAsStream(fileName);
        return resourceStream != null ?
                new BufferedReader(new InputStreamReader(resourceStream)).lines().collect(Collectors.toList()) :
                new ArrayList<>();
    }

    private static CollectedCallbacks deserializeCallbacks() throws RuntimeException {
        try {
            return CollectedCallbacksSerializer.deserialize(FlowDroidAnalysis.COLLECTED_CALLBACKS_FILE);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FlowDroidCallbacks file cannot be found: " + e.getMessage());
        }
    }

    private static SootClass getParentClass(SootMethod method) {
        SootClass clazz = method.getDeclaringClass();
        return clazz.hasOuterClass() ? clazz.getOuterClass() : clazz;
    }
}

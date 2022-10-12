package phd.research.graph;

import org.xmlpull.v1.XmlPullParserException;
import phd.research.core.FlowDroidUtils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.util.SystemClassHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class Filter {

    private static final List<String> packageBlacklist = Filter.loadBlacklist("package_blacklist");
    private static final List<String> classBlacklist = Filter.loadBlacklist("class_blacklist");
    private static final List<String> layoutBlacklist = Filter.loadBlacklist("layout_blacklist");
    private static final CollectedCallbacks flowDroidCallbacks = Filter.deserializeCallbacks();

    public static boolean isEntryPointClass(File apk, SootClass clazz) throws XmlPullParserException, IOException {
        return FlowDroidUtils.getEntryPointClasses(apk).stream().anyMatch(e -> e.equals(clazz));
    }

    public static boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public static boolean isListenerMethod(SootMethod method) {
        SootClass parentClass = getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : flowDroidCallbacks.getCallbackMethods().get(parentClass)) {
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
            for (AndroidCallbackDefinition callbackDefinition : flowDroidCallbacks.getCallbackMethods().get(clazz)) {
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

    public static boolean isValidLayout(String layout) {
        return !layoutBlacklist.contains(layout);
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

        return classBlacklist.stream().noneMatch(blacklistedClass -> clazz.getShortName().contains(blacklistedClass));
    }

    private static boolean isValidPackage(String packageName) {
        return packageBlacklist.stream().noneMatch(
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
        File callbacksFile = new File(System.getProperty("user.dir") + File.separator + "FlowDroidCallbacks");
        try {
            return CollectedCallbacksSerializer.deserialize(callbacksFile);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static SootClass getParentClass(SootMethod method) {
        SootClass clazz = method.getDeclaringClass();
        return clazz.hasOuterClass() ? clazz.getOuterClass() : clazz;
    }
}

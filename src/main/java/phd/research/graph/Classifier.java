package phd.research.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.enums.Type;
import phd.research.singletons.FlowDroidAnalysis;
import phd.research.singletons.GraphSettings;
import phd.research.utility.Filter;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Jordan Doyle
 */

public class Classifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(Classifier.class);

    private static final CollectedCallbacks COLLECTED_CALLBACKS = Classifier.deserializeCallbacks();

    private AndroidEntryPointUtils entryPointUtils;

    public Classifier() {

    }

    public static CollectedCallbacks deserializeCallbacks() {
        if (!GraphSettings.v().getFlowDroidCallbacksFile().isFile()) {
            FlowDroidAnalysis.v().runFlowDroid();
        }

        LOGGER.info("Deserializing FlowDroid callbacks file.");
        try {
            return CollectedCallbacksSerializer.deserialize(GraphSettings.v().getFlowDroidCallbacksFile());
        } catch (FileNotFoundException e) {
            throw new RuntimeException("FlowDroid callbacks file cannot be found: " + e.getMessage());
        }
    }

    private static SootClass getParentClass(SootMethod method) {
        SootClass clazz = method.getDeclaringClass();
        return clazz.hasOuterClass() ? clazz.getOuterClass() : clazz;
    }

    public boolean isLifecycleMethod(SootMethod method) {
        if (this.entryPointUtils == null) {
            this.entryPointUtils = new AndroidEntryPointUtils();
        }

        return this.entryPointUtils.isEntryPointMethod(method);
    }

    public boolean isListenerMethod(SootMethod method) {
        SootClass clazz = Classifier.getParentClass(method);
        for (AndroidCallbackDefinition definition : Classifier.COLLECTED_CALLBACKS.getCallbackMethods().get(clazz)) {
            if (definition.getTargetMethod().equals(method) &&
                    definition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                return true;
            }
        }

        return false;
    }

    public boolean isOtherCallbackMethod(SootMethod method) {
        if (this.isLifecycleMethod(method) || this.isListenerMethod(method)) {
            return false;
        }

        SootClass clazz = Classifier.getParentClass(method);
        for (AndroidCallbackDefinition definition : Classifier.COLLECTED_CALLBACKS.getCallbackMethods().get(clazz)) {
            if (definition.getTargetMethod().equals(method)) {
                return true;
            }
        }

        return false;
    }

    public boolean isPossibleListenerMethod(SootMethod method) {
        if (!this.isLifecycleMethod(method) && !this.isListenerMethod(method) && !this.isOtherCallbackMethod(method)) {
            return method.getParameterTypes().stream().anyMatch(type -> type.toString().equals("android.view.View"));
        }

        return false;
    }

    public Type getMethodType(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass")) {
            return Type.DUMMY;
        }
        if (this.isLifecycleMethod(method)) {
            return Type.LIFECYCLE;
        }
        if (this.isListenerMethod(method) || this.isPossibleListenerMethod(method)) {
            return Type.LISTENER;
        }
        if (this.isOtherCallbackMethod(method)) {
            return Type.CALLBACK;
        }
        return Type.METHOD;
    }

    public Map<SootClass, Set<SootClass>> getFragments() {
        Map<SootClass, Set<SootClass>> fragmentsPairs = new HashMap<>();

        Classifier.COLLECTED_CALLBACKS.getFragmentClasses().forEach(p -> {
            if (Filter.isValidClass(p.getO1()) && Filter.isValidClass(p.getO2())) {
                Set<SootClass> fragments = fragmentsPairs.getOrDefault(p.getO1(), new HashSet<>());
                fragments.add(p.getO2());
                fragmentsPairs.put(p.getO1(), fragments);
            }
        });

        return fragmentsPairs;
    }
}

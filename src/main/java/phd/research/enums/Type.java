package phd.research.enums;

import phd.research.graph.Filter;
import soot.SootMethod;

/**
 * @author Jordan Doyle
 */

public enum Type {
    unit, method, callback, listener, lifecycle, control, dummy, unknown;

    public static Type getMethodType(SootMethod method) throws RuntimeException {
        if (method.getDeclaringClass().getName().equals("dummyMainClass")) {
            return Type.dummy;
        }
        if (Filter.isLifecycleMethod(method)) {
            return Type.lifecycle;
        }
        if (Filter.isListenerMethod(method) || Filter.isPossibleListenerMethod(method)) {
            return Type.listener;
        }
        if (Filter.isOtherCallbackMethod(method)) {
            return Type.callback;
        }
        if (Filter.isValidMethod(method)) {
            return Type.method;
        }
        throw new RuntimeException(String.format("Found method %s with unknown type.", method));
    }
}

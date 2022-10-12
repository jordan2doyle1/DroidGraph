package phd.research.enums;

import phd.research.graph.Filter;
import soot.SootMethod;

/**
 * @author Jordan Doyle
 */

public enum Type {
    UNIT, METHOD, CALLBACK, LISTENER, LIFECYCLE, CONTROL, DUMMY, UNKNOWN;

    public static Type getMethodType(SootMethod method) throws RuntimeException {
        if (method.getDeclaringClass().getName().equals("dummyMainClass")) {
            return Type.DUMMY;
        }
        if (Filter.isLifecycleMethod(method)) {
            return Type.LIFECYCLE;
        }
        if (Filter.isListenerMethod(method) || Filter.isPossibleListenerMethod(method)) {
            return Type.LISTENER;
        }
        if (Filter.isOtherCallbackMethod(method)) {
            return Type.CALLBACK;
        }
        if (Filter.isValidMethod(method)) {
            return Type.METHOD;
        }
        throw new RuntimeException(String.format("Found method %s with unknown type.", method));
    }
}

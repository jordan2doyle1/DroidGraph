package phd.research.helper;

import soot.SootClass;
import soot.SootMethod;

public class Status {

    // TODO: Replace this static class with a class that inherits from AbstractStmtSwitch instead.
    // TODO: Will a class called abstract stmt switch survive between units.
    // TODO: Possibly just make the class non static.

    private static boolean foundLayout;
    private static boolean foundView;
    private static boolean foundClass;
    private static boolean foundMethod;
    private static SootClass clazz;
    private static SootMethod method;

    private Status() {

    }

    public static boolean isLayoutFound() {
        return foundLayout;
    }

    public static void foundLayout(boolean found) {
        foundLayout = found;
    }

    public static boolean isViewFound() {
        return foundView;
    }

    public static void foundView(boolean found) {
        foundView = found;
    }

    public static boolean isClassFound() {
        return foundClass;
    }

    public static void foundClass(boolean found) {
        foundClass = found;
    }

    public static void foundMethod(boolean found) {
        foundMethod = found;
    }

    public static SootClass getFoundClass() {
        return clazz;
    }

    public static void setFoundClass(SootClass foundClass) {
        clazz = foundClass;
    }

    public static void setFoundMethod(SootMethod foundMethod) {
        method = foundMethod;
    }

    public static void reset() {
        foundLayout = false;
        foundClass = false;
        foundMethod = false;
        foundView = false;
        clazz = null;
        method = null;
    }
}

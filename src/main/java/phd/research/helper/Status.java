package phd.research.helper;

import soot.SootClass;

public class Status {

    private boolean foundView;
    private boolean foundClass;
    private SootClass clazz;

    public Status() {

    }

    public boolean isViewFound() {
        return foundView;
    }

    public void foundView(boolean found) {
        foundView = found;
    }

    public boolean isClassFound() {
        return foundClass;
    }

    public void foundClass(boolean found) {
        foundClass = found;
    }

    public SootClass getFoundClass() {
        return clazz;
    }

    public void setFoundClass(SootClass foundClass) {
        clazz = foundClass;
    }
}

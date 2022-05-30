package phd.research.ui;

import phd.research.helper.Control;
import soot.SootMethod;

import java.util.HashSet;
import java.util.Set;

public class UiControls {

    private Set<Control> controls;

    public UiControls() {

    }

    public Set<Control> getControls() {
        if (this.controls == null) this.controls = getAllControls();

        return this.controls;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasControl(SootMethod listener) {
        for (Control control : this.getControls()) {
            SootMethod controlClickListener = control.getClickListener();
            if (controlClickListener != null && controlClickListener.equals(listener)) return true;
        }

        return false;
    }

    private Set<Control> getAllControls() {
        Set<Control> controls = new HashSet<>();

        return controls;
    }
}

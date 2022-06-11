package phd.research.helper;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Control {

    private final ARSCFileParser.AbstractResource control;
    private final ARSCFileParser.AbstractResource layout;
    private final SootClass activity;
    private SootMethod clickListener;

    public Control(ARSCFileParser.AbstractResource control, ARSCFileParser.AbstractResource layout, SootClass activity,
            SootMethod clickListener) {
        this.control = Objects.requireNonNull(control);
        this.layout = Objects.requireNonNull(layout);
        this.activity = Objects.requireNonNull(activity);
        this.clickListener = clickListener;
    }

    public ARSCFileParser.AbstractResource getControlResource() {
        return this.control;
    }

    public SootMethod getClickListener() {
        return this.clickListener;
    }

    public void setClickListener(SootMethod method) {
        this.clickListener = method;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Control)) {
            return false;
        }

        Control control = (Control) o;
        return this.control.equals(control.control) && this.layout.equals(control.layout) &&
                this.activity.equals(control.activity) && Objects.equals(this.clickListener, control.clickListener);
    }

    @Override
    public int hashCode() {
        return this.control.hashCode() + this.layout.hashCode() + this.activity.hashCode() +
                this.clickListener.hashCode();
    }
}
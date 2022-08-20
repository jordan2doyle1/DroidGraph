package phd.research.graph;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class Control {

    @Nonnull
    private final ARSCFileParser.AbstractResource control;
    @Nonnull
    private final ARSCFileParser.AbstractResource layout;
    @Nonnull
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

    public ARSCFileParser.AbstractResource getLayoutResource() {
        return this.layout;
    }

    public SootClass getControlActivity() {
        return this.activity;
    }

    public SootMethod getClickListener() {
        return this.clickListener;
    }

    public void setClickListener(SootMethod method) {
        this.clickListener = method;
    }

    @Override
    public String toString() {
        return String.format("%s{control=%s, layout=%s, activity=%s, clickListener=%s}", getClass().getSimpleName(),
                this.control.getResourceName(), this.layout.getResourceName(), this.activity.getShortName(),
                (this.clickListener != null ? this.clickListener.getName() : null)
                            );
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Control)) {
            return false;
        }

        Control that = (Control) o;

        if (!this.control.getResourceName().equals(that.control.getResourceName())) {
            return false;
        }
        if (this.control.getResourceID() != that.control.getResourceID()) {
            return false;
        }
        if (!this.layout.equals(that.layout)) {
            return false;
        }
        return this.activity.equals(that.activity);
    }

    @Override
    public final int hashCode() {
        int result = this.control.hashCode();
        result = 31 * result + this.layout.hashCode();
        result = 31 * result + this.activity.hashCode();
        return result;
    }
}
package phd.research.helper;

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
        return String.format("%s: (%s, %s, %s, %s)", getClass().getSimpleName(), this.control.getResourceName(),
                this.layout.getResourceName(), this.activity.getName(), this.clickListener.getName());
    }

    @Override
    public final boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Control)) {
            return false;
        }

        Control control = (Control) o;
        return Objects.equals(this.control, control.control) && Objects.equals(this.layout, control.layout) &&
                Objects.equals(this.activity, control.activity);
    }

    @Override
    public final int hashCode() {
        return this.control.hashCode() + this.layout.hashCode() + this.activity.hashCode();
    }
}
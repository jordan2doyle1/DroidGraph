package phd.research.graph;

import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class Control implements Serializable {

    @Nonnull
    private final ARSCFileParser.AbstractResource control;
    @Nonnull
    private final ARSCFileParser.AbstractResource layout;
    @Nonnull
    private final SootClass activity;
    @Nonnull
    private Collection<SootMethod> clickListeners;

    public Control(ARSCFileParser.AbstractResource control, ARSCFileParser.AbstractResource layout, SootClass activity,
            Collection<SootMethod> clickListeners) {
        this.control = Objects.requireNonNull(control);
        this.layout = Objects.requireNonNull(layout);
        this.activity = Objects.requireNonNull(activity);
        this.clickListeners = Objects.requireNonNull(clickListeners);
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

    @Nonnull
    public Collection<SootMethod> getClickListeners() {
        return this.clickListeners;
    }

    public void setClickListeners(Collection<SootMethod> listeners) {
        this.clickListeners = Objects.requireNonNull(listeners);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        this.clickListeners.forEach(l -> builder.append(l.getName()).append(","));
        if (builder.charAt(builder.length() - 1) != '[') {
            builder.replace(builder.length() - 1, builder.length(), "]");
        } else {
            builder.append("]");
        }

        return String.format("%s{control=%s, layout=%s, activity=%s, clickListener=%s}", getClass().getSimpleName(),
                this.control.getResourceName(), this.layout.getResourceName(), this.activity.getShortName(), builder
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
        if (!layout.equals(that.layout)) {
            return false;
        }
        if (!activity.equals(that.activity)) {
            return false;
        }
        return clickListeners.equals(that.clickListeners);
    }

    @Override
    public final int hashCode() {
        int result = control.hashCode();
        result = 31 * result + layout.hashCode();
        result = 31 * result + activity.hashCode();
        result = 31 * result + clickListeners.hashCode();
        return result;
    }
}
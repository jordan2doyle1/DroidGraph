package phd.research.graph;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class Control implements Serializable {

    private final int controlId;
    private final int layoutId;

    @Nonnull
    private final String controlName;
    @Nonnull
    private final String layoutName;
    @Nonnull
    private final String activity;

    @Nonnull
    private Collection<String> listeners;

    public Control(int controlId, String controlName, int layoutId, String layoutName, String activity,
            Collection<String> listeners) {
        this.controlId = controlId;
        this.controlName = Objects.requireNonNull(controlName);
        this.layoutId = layoutId;
        this.layoutName = Objects.requireNonNull(layoutName);
        this.activity = Objects.requireNonNull(activity);
        this.listeners = Objects.requireNonNull(listeners);
    }

    public int getControlId() {
        return this.controlId;
    }

    @Nonnull
    public String getControlName() {
        return this.controlName;
    }

    public int getLayoutId() {
        return this.layoutId;
    }

    @Nonnull
    public String getLayoutName() {
        return this.layoutName;
    }

    @Nonnull
    public String getActivity() {
        return this.activity;
    }

    @Nonnull
    public Collection<String> getListeners() {
        return this.listeners;
    }

    public void setListeners(Collection<String> listeners) {
        this.listeners = Objects.requireNonNull(listeners);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{controlId=" + controlId + ", layoutId=" + layoutId + ", controlName='" +
                controlName + "', layoutName='" + layoutName + "', activity='" + activity + "', listeners=" +
                listeners + '}';
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

        return this.controlId == that.controlId && this.layoutId == that.layoutId &&
                controlName.equals(that.controlName) && layoutName.equals(that.layoutName) &&
                activity.equals(that.activity);
    }

    @Override
    public final int hashCode() {
        int result = controlId;
        result = 31 * result + layoutId;
        result = 31 * result + controlName.hashCode();
        result = 31 * result + layoutName.hashCode();
        result = 31 * result + activity.hashCode();
        return result;
    }
}
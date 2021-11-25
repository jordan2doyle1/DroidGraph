package phd.research.helper;

import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Control {

    private final int hashcode;
    private final ARSCFileParser.AbstractResource control;
    private final ARSCFileParser.AbstractResource layout;
    private final SootMethod clickListener;

    public Control(int hashcode, ARSCFileParser.AbstractResource control, ARSCFileParser.AbstractResource layout,
                   SootMethod clickListener) {
        this.hashcode = hashcode;
        this.control = control;
        this.layout = layout;
        this.clickListener = clickListener;
    }

    public ARSCFileParser.AbstractResource getControlResource() {
        return this.control;
    }

    public ARSCFileParser.AbstractResource getLayoutResource() {
        return this.layout;
    }

    public SootMethod getClickListener() {
        return this.clickListener;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Control))
            return false;

        Control control = (Control) o;
        return this.hashcode == control.hashcode && Objects.equals(this.clickListener, control.clickListener) &&
                this.control.equals(control.control) && this.layout.equals(control.layout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hashcode, this.control, this.layout, this.clickListener);
    }
}
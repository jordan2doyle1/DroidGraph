package phd.research.helper;

import soot.SootMethod;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Control {

    private final int hashcode;
    private final int id;
    private final String textId;
    private final SootMethod clickListener;

    public Control(int hashcode, int id, String textId, SootMethod clickListener) {
        this.hashcode = hashcode;
        this.id = id;
        this.textId = textId;
        this.clickListener = clickListener;
    }

    public int getId() {
        return this.id;
    }

    public String getTextId() {
        return this.textId;
    }

    public SootMethod getClickListener() {
        return this.clickListener;
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
        return this.hashcode == control.hashcode && Objects.equals(this.clickListener, control.clickListener) &&
                this.id == control.id && this.textId.equals(control.textId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hashcode, this.id, this.textId, this.clickListener);
    }
}
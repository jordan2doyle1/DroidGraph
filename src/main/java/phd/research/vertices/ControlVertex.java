package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import phd.research.graph.Control;

import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class ControlVertex extends DefaultVertex {

    @NotNull
    private final Control control;

    public ControlVertex(Control control) {
        super(Type.CONTROL);
        this.control = Objects.requireNonNull(control);
    }

    public ControlVertex(int id, Control control) {
        super(id, Type.CONTROL);
        this.control = Objects.requireNonNull(control);
    }

    @NotNull
    public Control getControl() {
        return this.control;
    }

    public Color getColor() {
        return Color.RED;
    }

    public Shape getShape() {
        return Shape.CIRCLE;
    }

    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = super.getAttributes();

        attributes.put("controlId", DefaultAttribute.createAttribute(this.control.getControlId()));
        attributes.put("control", DefaultAttribute.createAttribute(this.control.getControlName()));
        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("layoutId", DefaultAttribute.createAttribute(this.control.getLayoutId()));
        attributes.put("layout", DefaultAttribute.createAttribute(this.control.getLayoutName()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("activity", DefaultAttribute.createAttribute(this.control.getActivity()));
        attributes.put("listeners", DefaultAttribute.createAttribute(this.control.getListeners().toString()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));

        return attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + super.getId() + ", type=" + super.getType() + ", control='" +
                this.control + "', visit=" + super.hasVisit() + ", localVisit=" + super.hasLocalVisit() + "}";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ControlVertex)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        ControlVertex that = (ControlVertex) o;
        if (!this.control.equals(that.control)) {
            return false;
        }

        return that.canEqual(this);
    }

    @Override
    public final int hashCode() {
        int result = super.hashCode();
        result = 31 * result + control.hashCode();
        return result;
    }

    @Override
    public final boolean canEqual(Object o) {
        return (o instanceof ControlVertex);
    }
}

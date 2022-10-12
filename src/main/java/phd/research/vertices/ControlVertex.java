package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import phd.research.graph.Control;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class ControlVertex extends Vertex implements Serializable {

    @NotNull
    private final Control control;

    public ControlVertex(Control control) {
        super(Type.CONTROL);
        this.control = Objects.requireNonNull(control);
        super.setLabel("Control{control=" + control.getControlResource().getResourceName() + "(" +
                control.getControlResource().getResourceID() + ")}");
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

    public Style getStyle() {
        return Style.FILLED;
    }

    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = super.getAttributes();

        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));

        return attributes;
    }

    @Override
    public String toString() {
        return String.format("Control{label='%s', visit=%s, localVisit=%s, control=%s}", super.getLabel(),
                super.hasVisit(), super.hasLocalVisit(), this.control.getControlResource().getResourceName() + "(" +
                        this.control.getControlResource().getResourceID() + ")"
                            );
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

        if (!control.equals(that.control)) {
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

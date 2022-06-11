package phd.research.jGraph;

import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import phd.research.helper.Control;

import java.util.Map;

public class ControlVertex extends Vertex {

    private final Control control;

    public ControlVertex(Control control) {
        super(Type.control);
        this.control = control;
        this.label = this.control.getControlResource().getResourceName() + " (" +
                this.control.getControlResource().getResourceID() + ")";
    }

    public Color getColor() {
        return Color.red;
    }

    public Shape getShape() {
        return Shape.circle;
    }

    public Style getStyle() {
        return Style.filled;
    }

    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = super.getAttributes();

        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));

        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof ControlVertex)) {
            return false;
        }

        ControlVertex vertex = (ControlVertex) o;
        return this.control.equals(vertex.control);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.control.hashCode();
    }
}

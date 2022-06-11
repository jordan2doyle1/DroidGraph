package phd.research.jGraph;

import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.Unit;

import java.util.Map;

public class UnitVertex extends Vertex {

    private final Unit unit;

    public UnitVertex(Unit unit) {
        super(Type.unit);
        this.unit = unit;
        this.label = this.unit.toString();
    }

    public Color getColor() {
        return Color.yellow;
    }

    public Shape getShape() {
        return Shape.box;
    }

    public Style getStyle() {
        return Style.filled;
    }

    @Override
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

        if (!(o instanceof UnitVertex)) {
            return false;
        }

        UnitVertex vertex = (UnitVertex) o;
        return this.unit.equals(vertex.unit);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.unit.hashCode();
    }
}

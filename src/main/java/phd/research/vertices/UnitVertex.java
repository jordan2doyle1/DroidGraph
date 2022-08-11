package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.Unit;

import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class UnitVertex extends Vertex {

    @NotNull
    private final Unit unit;

    public UnitVertex(Unit unit) {
        super(Type.unit);
        this.unit = Objects.requireNonNull(unit);
        super.setLabel("Unit{unit=" + unit + "}");
    }

    @NotNull
    public Unit getUnit() {
        return this.unit;
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
    public String toString() {
        return "Unit{label='" + super.getLabel() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + ", unit=" + unit + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnitVertex)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        UnitVertex vertex = (UnitVertex) o;

        return unit.equals(vertex.unit);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + unit.hashCode();
        return result;
    }
}

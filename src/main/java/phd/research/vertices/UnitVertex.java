package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.Unit;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class UnitVertex extends Vertex implements Serializable {

    @NotNull
    private final Unit unit;

    public UnitVertex(Unit unit) {
        super(Type.UNIT);
        this.unit = Objects.requireNonNull(unit);
        super.setLabel(String.format("Unit{unit=%s}", unit));
    }

    @NotNull
    public Unit getUnit() {
        return this.unit;
    }

    public Color getColor() {
        return Color.YELLOW;
    }

    public Shape getShape() {
        return Shape.BOX;
    }

    public Style getStyle() {
        return Style.FILLED;
    }

    @Override
    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = super.getAttributes();

        attributes.put("unit", DefaultAttribute.createAttribute(this.getUnit().toString()));
        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));

        return attributes;
    }

    @Override
    public String toString() {
        return String.format("Unit{label='%s', visit=%s, localVisit=%s, unit=%s}", super.getLabel(), super.hasVisit(),
                super.hasLocalVisit(), this.unit
                            );
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnitVertex)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        UnitVertex that = (UnitVertex) o;

        if (!this.unit.equals(that.unit)) {
            return false;
        }

        return that.canEqual(this);
    }

    @Override
    public final int hashCode() {
        int result = super.hashCode();
        result = 31 * result + unit.hashCode();
        return result;
    }

    @Override
    public final boolean canEqual(Object o) {
        return (o instanceof UnitVertex);
    }
}

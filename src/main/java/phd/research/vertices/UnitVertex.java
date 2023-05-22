package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;

import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class UnitVertex extends DefaultVertex {

    @NotNull
    private final String methodSignature;
    @NotNull
    private final String unit;

    public UnitVertex(String methodSignature, String unit) {
        super(Type.UNIT);
        this.methodSignature = Objects.requireNonNull(methodSignature);
        this.unit = Objects.requireNonNull(unit);
    }

    public UnitVertex(int id, String methodSignature, String unit) {
        super(id, Type.UNIT);
        this.methodSignature = Objects.requireNonNull(methodSignature);
        this.unit = Objects.requireNonNull(unit);
    }

    @NotNull
    public String getMethodSignature() {
        return this.methodSignature;
    }

    @NotNull
    public String getUnit() {
        return this.unit;
    }

    public Color getColor() {
        return Color.YELLOW;
    }

    public Shape getShape() {
        return Shape.BOX;
    }

    @Override
    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = super.getAttributes();

        attributes.put("method", DefaultAttribute.createAttribute(this.getMethodSignature()));
        attributes.put("unit", DefaultAttribute.createAttribute(this.getUnit()));
        attributes.put("label", DefaultAttribute.createAttribute(this.getUnit()));
        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name().toLowerCase()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name().toLowerCase()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name().toLowerCase()));

        return attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + super.getId() + ", type=" + super.getType() +
                ", methodSignature='" + this.getMethodSignature() + "', unit='" + unit + "', visit=" +
                super.hasVisit() + ", localVisit=" + super.hasLocalVisit() + "}";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UnitVertex)) {
            return false;
        }

//        if (!super.equals(o)) {
//            return false;
//        }

        UnitVertex that = (UnitVertex) o;
        if (super.getType() != that.getType()) {
            return false;
        }

        if (!methodSignature.equals(that.methodSignature) || !unit.equals(that.unit)) {
            return false;
        }

        return that.canEqual(this);
    }

    @Override
    public final int hashCode() {
        int result = super.hashCode();
        result = 31 * result + methodSignature.hashCode();
        result = 31 * result + unit.hashCode();
        return result;
    }

    @Override
    public final boolean canEqual(Object o) {
        return (o instanceof UnitVertex);
    }
}

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

public class MethodVertex extends DefaultVertex {

    @NotNull
    private final String methodSignature;

    public MethodVertex(String methodSignature) {
        this(Type.METHOD, methodSignature);
    }

    public MethodVertex(int id, String methodSignature) {
        this(id, Type.METHOD, methodSignature);
    }

    public MethodVertex(Type type, String methodSignature) {
        super(type);
        this.methodSignature = Objects.requireNonNull(methodSignature);
    }

    public MethodVertex(int id, Type type, String methodSignature) {
        super(id, type);
        this.methodSignature = Objects.requireNonNull(methodSignature);
    }

    @NotNull
    public String getMethodSignature() {
        return this.methodSignature;
    }

    public Color getColor() {
        return Color.GREEN;
    }

    public Shape getShape() {
        return Shape.ELLIPSE;
    }

    @Override
    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = super.getAttributes();

        attributes.put("method", DefaultAttribute.createAttribute(this.getMethodSignature()));
        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));

        return attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + super.getId() + ", type=" + super.getType() +
                ", methodSignature='" + this.getMethodSignature() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + "}";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MethodVertex)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        MethodVertex that = (MethodVertex) o;
        if (!this.methodSignature.equals(that.methodSignature)) {
            return false;
        }

        return that.canEqual(this);
    }

    @Override
    public final int hashCode() {
        int result = super.hashCode();
        result = 31 * result + methodSignature.hashCode();
        return result;
    }

    @Override
    public final boolean canEqual(Object o) {
        return (o instanceof MethodVertex);
    }
}

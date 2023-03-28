package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class DefaultVertex implements Vertex, Serializable {

    private static int currentIdSequenceValue = 0;

    private final int id;
    @NotNull
    private final Type type;

    private boolean visit;
    private boolean localVisit;

    public DefaultVertex(Type type) {
        this(currentIdSequenceValue++, type);
    }

    public DefaultVertex(int id, Type type) {
        this.id = id;
        if (currentIdSequenceValue < id) {
            currentIdSequenceValue = id;
        }

        this.type = Objects.requireNonNull(type);
        this.visit = false;
        this.localVisit = false;
    }

    public static void resetIdSequence() {
        DefaultVertex.currentIdSequenceValue = 0;
    }

    public int getId() {
        return this.id;
    }

    @NotNull
    public Type getType() {
        return this.type;
    }

    public boolean hasVisit() {
        return this.visit;
    }

    public void visit() {
        this.visit = true;
    }

    public void visitReset() {
        this.visit = false;
    }

    public boolean hasLocalVisit() {
        return this.localVisit;
    }

    public void localVisit() {
        this.localVisit = true;
    }

    public void localVisitReset() {
        this.localVisit = false;
    }

    @Override
    public Color getColor() {
        return Color.GREY;
    }

    @Override
    public Shape getShape() {
        return Shape.PENTAGON;
    }

    @Override
    public Style getStyle() {
        return Style.FILLED;
    }


    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = new HashMap<>();

        attributes.put("type", DefaultAttribute.createAttribute(this.type.name()));
        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));

        return attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", type=" + type + ", visit=" + visit + ", localVisit=" +
                localVisit + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof DefaultVertex)) {
            return false;
        }

        DefaultVertex that = (DefaultVertex) o;
        if (this.id != that.id || this.type != that.type) {
            return false;
        }

        return that.canEqual(this);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + type.hashCode();
        return result;
    }

    public boolean canEqual(Object o) {
        return (o instanceof DefaultVertex);
    }
}

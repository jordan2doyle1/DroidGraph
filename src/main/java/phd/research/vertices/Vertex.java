package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Type;
import phd.research.helper.API;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

@API
public class Vertex implements Serializable {

    @NotNull
    private final Type type;
    @NotNull
    private String label;

    private boolean visit;
    private boolean localVisit;

    public Vertex(Type type) {
        this(type, "Vertex{type=" + type + "}");
    }

    public Vertex(Type type, String label) {
        this.type = Objects.requireNonNull(type);
        this.label = Objects.requireNonNull(label);
        this.visit = false;
        this.localVisit = false;
    }

    @API
    @NotNull
    public Type getType() {
        return this.type;
    }

    @API
    @NotNull
    public String getLabel() {
        return this.label;
    }

    @API
    public void setLabel(String label) {
        this.label = Objects.requireNonNull(label);
    }

    @API
    public boolean hasVisit() {
        return this.visit;
    }

    @API
    public void visit() {
        this.visit = true;
    }

    @API
    public void visitReset() {
        this.visit = false;
    }

    @API
    public boolean hasLocalVisit() {
        return this.localVisit;
    }

    @API
    public void localVisit() {
        this.localVisit = true;
    }

    @API
    public void localVisitReset() {
        this.localVisit = false;
    }

    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = new HashMap<>();

        attributes.put("type", DefaultAttribute.createAttribute(this.type.name()));
        attributes.put("label", DefaultAttribute.createAttribute(this.label));

        return attributes;
    }

    @Override
    public String toString() {
        return "Vertex{type=" + type + ", label='" + label + "', visit=" + visit + ", localVisit=" + localVisit + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vertex)) {
            return false;
        }

        Vertex vertex = (Vertex) o;

        if (type != vertex.type) {
            return false;
        }

        if (!label.equals(vertex.label)) {
            return false;
        }

        return vertex.canEqual(this);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + label.hashCode();
        return result;
    }

    public boolean canEqual(Object o) {
        return (o instanceof Vertex);
    }
}

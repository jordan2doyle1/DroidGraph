package phd.research.jGraph;

import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Vertex {

    private final Type type;

    private boolean visited;
    private boolean localVisit;

    protected String label;

    public Vertex(Type type) {
        this.type = type;
        this.visited = false;
        this.localVisit = false;
    }

    public Vertex(Type type, String label) {
        this.type = type;
        this.label = label;
        this.visited = false;
        this.localVisit = false;
    }

    public String getLabel() {
        return this.label;
    }

    public Type getType() {
        return this.type;
    }

    public boolean hasVisited() {
        return this.visited;
    }

    public void visit() {
        this.visited = true;
    }

    public void visitReset() {
        this.visited = false;
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

    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = new HashMap<>();

        attributes.put("label", DefaultAttribute.createAttribute(this.label));
        attributes.put("type", DefaultAttribute.createAttribute(this.type.name()));

        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Vertex)) {
            return false;
        }

        Vertex vertex = (Vertex) o;
        return this.type == vertex.type && Objects.equals(this.label, vertex.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.label);
    }
}

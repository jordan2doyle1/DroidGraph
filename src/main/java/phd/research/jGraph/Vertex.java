package phd.research.jGraph;

import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.SootClass;
import soot.SootMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Vertex {

    private final Integer id;
    private final String label;
    private final Type type;
    private final SootClass sootClass;
    private final SootMethod sootMethod;
    private boolean visited;
    private boolean localVisit;

    public Vertex(Object hashcode, String label, Type type, SootMethod method) {
        this.id = (Integer) hashcode;
        this.type = type;
        this.label = label;
        this.sootMethod = method;
        this.sootClass = method.getDeclaringClass();
        this.visited = false;
        this.localVisit = false;
    }

    public Vertex(Object hashcode, String label, Type type) {
        this.id = (Integer) hashcode;
        this.type = type;
        this.label = label;
        this.sootMethod = null;
        this.sootClass = null;
        this.visited = false;
        this.localVisit = false;
    }

    public Integer getID() {
        return this.id;
    }

    public String getLabel() {
        return this.label;
    }

    public Type getType() {
        return this.type;
    }

    public SootClass getSootClass() {
        return this.sootClass;
    }

    public SootMethod getSootMethod() {
        return this.sootMethod;
    }

    public Color getColor() {
        switch (this.type) {
            case statement:
                return Color.yellow;
            case method:
                return Color.green;
            case dummyMethod:
                return Color.black;
            case control:
                return Color.red;
            case listener:
                return Color.orange;
            case lifecycle:
                return Color.blue;
        }
        return null;
    }

    public Shape getShape() {
        switch (this.type) {
            case statement:
                return Shape.box;
            case method:
                return Shape.ellipse;
            case dummyMethod:
                return Shape.parallelogram;
            case control:
                return Shape.circle;
            case listener:
                return Shape.octagon;
            case lifecycle:
                return Shape.diamond;
        }
        return null;
    }

    public Style getStyle() {
        return Style.filled;
    }

    public Map<String, Attribute> getAttributes() {
        Map<String, Attribute> attributes = new HashMap<>();

        attributes.put("label", DefaultAttribute.createAttribute(this.label));
        attributes.put("color", DefaultAttribute.createAttribute(this.getColor().name()));
        attributes.put("shape", DefaultAttribute.createAttribute(this.getShape().name()));
        attributes.put("style", DefaultAttribute.createAttribute(this.getStyle().name()));
        attributes.put("type", DefaultAttribute.createAttribute(this.type.name()));

        return attributes;
    }

    public boolean hasVisited() {
        return this.visited;
    }

    public void visit() {
        this.visited = true;
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Vertex)) {
            return false;
        }

        Vertex vertex = (Vertex) o;
        return this.id.equals(vertex.id) && Objects.equals(this.label, vertex.label)
                && this.type == vertex.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.label);
    }
}

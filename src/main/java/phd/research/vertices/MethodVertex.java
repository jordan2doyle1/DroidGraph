package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.SootMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class MethodVertex extends Vertex {

    @NotNull
    private final SootMethod method;

    public MethodVertex(SootMethod method) {
        this(Type.method, method);
    }

    public MethodVertex(Type type, SootMethod method) {
        super(type);
        this.method = Objects.requireNonNull(method);
        super.setLabel(createLabel(method));
    }

    private static String removePackageName(String name) {
        int index = (name.toLowerCase().contains("dummymainmethod") ? name.lastIndexOf("_") : name.lastIndexOf("."));
        return (index != -1 ? name.replace(name.substring(0, index + 1), "") : name);
    }

    @NotNull
    public SootMethod getMethod() {
        return this.method;
    }

    public Color getColor() {
        return Color.green;
    }

    public Shape getShape() {
        return Shape.ellipse;
    }

    public Style getStyle() {
        return Style.filled;
    }

    private String createLabel(SootMethod method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(super.getType().name().substring(0, 1).toUpperCase())
                .append(super.getType().name().substring(1).toLowerCase()).append("{method=<")
                .append(MethodVertex.removePackageName(method.getDeclaringClass().getName())).append(": ")
                .append(MethodVertex.removePackageName(method.getReturnType().toString())).append(" ")
                .append(MethodVertex.removePackageName(method.getName())).append("(");

        List<soot.Type> parameters = method.getParameterTypes();
        for (int i = 0; i < method.getParameterCount(); i++) {
            stringBuilder.append(MethodVertex.removePackageName(parameters.get(i).toString()));
            if (i != (method.getParameterCount() - 1)) {
                stringBuilder.append(",");
            }
        }

        stringBuilder.append(")>");
        return stringBuilder.toString();
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
        return "Method{label='" + super.getLabel() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + ", method=" + this.method.getSignature() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MethodVertex)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MethodVertex vertex = (MethodVertex) o;

        return method.equals(vertex.method);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }
}

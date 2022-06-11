package phd.research.jGraph;

import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.SootMethod;

import java.util.List;
import java.util.Map;

public class MethodVertex extends Vertex {
    private final SootMethod method;

    public MethodVertex(SootMethod method) {
        super(Type.method);
        this.method = method;
        this.label = getLabel(this.method);
    }

    public MethodVertex(Type type, SootMethod method) {
        super(type);
        this.method = method;
        this.label = getLabel(this.method);
    }

    private static String removePackageName(String name) {
        int index = name.lastIndexOf(".");

        if (name.contains("dummyMainMethod")) {
            index = name.lastIndexOf("_");
        }

        if (index != -1) {
            name = name.replace(name.substring(0, index + 1), "");
        }

        return name;
    }

    private static String getLabel(SootMethod method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<").append(MethodVertex.removePackageName(method.getDeclaringClass().getName()))
                .append(": ").append(MethodVertex.removePackageName(method.getReturnType().toString())).append(" ")
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

        if (!(o instanceof MethodVertex)) {
            return false;
        }

        MethodVertex vertex = (MethodVertex) o;
        return this.method.equals(vertex.getMethod());
    }

    @Override
    public int hashCode() {
        return super.hashCode() + this.method.hashCode();
    }
}

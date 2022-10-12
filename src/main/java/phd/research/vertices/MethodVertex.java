package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import soot.SootMethod;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class MethodVertex extends Vertex implements Serializable {

    @NotNull
    private final SootMethod method;

    public MethodVertex(SootMethod method) {
        this(Type.METHOD, method);
    }

    public MethodVertex(Type type, SootMethod method) {
        super(type);
        this.method = Objects.requireNonNull(method);
        super.setLabel(createLabel(method));
    }

    public static MethodVertex createMethodVertex(SootMethod method) throws RuntimeException {
        switch (Type.getMethodType(method)) {
            case DUMMY:
                return new DummyVertex(method);
            case LIFECYCLE:
                return new LifecycleVertex(method);
            case LISTENER:
                return new ListenerVertex(method);
            case CALLBACK:
                return new CallbackVertex(method);
            case METHOD:
                return new MethodVertex(method);
            default:
                throw new RuntimeException(String.format("Method %s has unknown type.", method));
        }
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
        return Color.GREEN;
    }

    public Shape getShape() {
        return Shape.ELLIPSE;
    }

    public Style getStyle() {
        return Style.FILLED;
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
        return String.format("Method{label='%s', visit=%s, localVisit=%s, method=%s}", super.getLabel(),
                super.hasVisit(), super.hasLocalVisit(), this.method.getSignature()
                            );
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

        if (!this.method.equals(that.method)) {
            return false;
        }

        return that.canEqual(this);
    }

    @Override
    public final int hashCode() {
        int result = super.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }

    @Override
    public final boolean canEqual(Object o) {
        return (o instanceof MethodVertex);
    }
}

package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;

/**
 * @author Jordan Doyle
 */

public class CallbackVertex extends MethodVertex {

    public CallbackVertex(String methodSignature) {
        super(Type.CALLBACK, methodSignature);
    }

    public CallbackVertex(int id, String methodSignature) {
        super(id, Type.CALLBACK, methodSignature);
    }

    @Override
    public Color getColor() {
        return Color.PURPLE;
    }

    @Override
    public Shape getShape() {
        return Shape.HEXAGON;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + super.getId() + ", type=" + super.getType() +
                ", methodSignature='" + super.getMethodSignature() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + "}";
    }
}

package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;

/**
 * @author Jordan Doyle
 */

public class LifecycleVertex extends MethodVertex {

    public LifecycleVertex(String methodSignature) {
        super(Type.LIFECYCLE, methodSignature);
    }

    public LifecycleVertex(int id, String methodSignature) {
        super(id, Type.LIFECYCLE, methodSignature);
    }

    @Override
    public Color getColor() {
        return Color.BLUE;
    }

    @Override
    public Shape getShape() {
        return Shape.DIAMOND;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + super.getId() + ", type=" + super.getType() +
                ", methodSignature='" + super.getMethodSignature() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + "}";
    }
}

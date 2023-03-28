package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;

/**
 * @author Jordan Doyle
 */

public class DummyVertex extends MethodVertex {

    public DummyVertex(String methodSignature) {
        super(Type.DUMMY, methodSignature);
    }

    public DummyVertex(int id, String methodSignature) {
        super(id, Type.DUMMY, methodSignature);
    }

    @Override
    public Color getColor() {
        return Color.BLACK;
    }

    @Override
    public Shape getShape() {
        return Shape.PARALLELOGRAM;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + super.getId() + ", type=" + super.getType() +
                ", methodSignature='" + super.getMethodSignature() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + "}";
    }
}

package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;

/**
 * @author Jordan Doyle
 */

public class ListenerVertex extends MethodVertex {

    public ListenerVertex(String methodSignature) {
        super(Type.LISTENER, methodSignature);
    }

    public ListenerVertex(int id, String methodSignature) {
        super(id, Type.LISTENER, methodSignature);
    }

    @Override
    public Color getColor() {
        return Color.ORANGE;
    }

    @Override
    public Shape getShape() {
        return Shape.OCTAGON;
    }
}

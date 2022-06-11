package phd.research.jGraph;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

public class ListenerVertex extends MethodVertex {

    public ListenerVertex(SootMethod method) {
        super(Type.listener, method);
    }

    @Override
    public Color getColor() {
        return Color.orange;
    }

    @Override
    public Shape getShape() {
        return Shape.octagon;
    }
}

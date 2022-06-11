package phd.research.jGraph;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

public class LifecycleVertex extends MethodVertex {

    public LifecycleVertex(SootMethod method) {
        super(Type.lifecycle, method);
    }

    @Override
    public Color getColor() {
        return Color.blue;
    }

    @Override
    public Shape getShape() {
        return Shape.diamond;
    }
}

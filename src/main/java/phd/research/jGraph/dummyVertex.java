package phd.research.jGraph;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

public class dummyVertex extends MethodVertex {

    public dummyVertex(SootMethod method) {
        super(Type.dummyMethod, method);
    }

    @Override
    public Color getColor() {
        return Color.black;
    }

    @Override
    public Shape getShape() {
        return Shape.parallelogram;
    }
}
package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

/**
 * @author Jordan Doyle
 */

public class DummyVertex extends MethodVertex {

    public DummyVertex(SootMethod method) {
        super(Type.dummy, method);
    }

    @Override
    public Color getColor() {
        return Color.black;
    }

    @Override
    public Shape getShape() {
        return Shape.parallelogram;
    }

    @Override
    public String toString() {
        return "Dummy{label='" + super.getLabel() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + ", method=" + this.getMethod().getSignature() + "}";
    }
}

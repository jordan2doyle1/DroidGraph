package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

/**
 * @author Jordan Doyle
 */

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

    @Override
    public String toString() {
        return "Lifecycle{label='" + super.getLabel() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + ", method=" + this.getMethod().getSignature() + "}";
    }
}

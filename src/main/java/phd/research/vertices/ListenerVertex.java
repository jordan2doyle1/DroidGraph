package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

/**
 * @author Jordan Doyle
 */

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

    @Override
    public String toString() {
        return "Listener{label='" + super.getLabel() + "', visit=" + super.hasVisit() + ", localVisit=" +
                super.hasLocalVisit() + ", method=" + this.getMethod() + "}";
    }
}

package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

import java.io.Serializable;

/**
 * @author Jordan Doyle
 */

public class ListenerVertex extends MethodVertex implements Serializable {

    public ListenerVertex(SootMethod method) {
        super(Type.LISTENER, method);
    }

    @Override
    public Color getColor() {
        return Color.ORANGE;
    }

    @Override
    public Shape getShape() {
        return Shape.OCTAGON;
    }

    @Override
    public String toString() {
        return String.format("Listener{label='%s', visit=%s, localVisit=%s, method=%s}", super.getLabel(),
                super.hasVisit(), super.hasLocalVisit(), this.getMethod().getSignature()
                            );
    }
}

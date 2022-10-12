package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

import java.io.Serializable;

/**
 * @author Jordan Doyle
 */

public class CallbackVertex extends MethodVertex implements Serializable {

    public CallbackVertex(SootMethod method) {
        super(Type.CALLBACK, method);
    }

    @Override
    public Color getColor() {
        return Color.PURPLE;
    }

    @Override
    public Shape getShape() {
        return Shape.HEXAGON;
    }

    @Override
    public String toString() {
        return String.format("Callback{label='%s', visit=%s, localVisit=%s, method=%s}", super.getLabel(),
                super.hasVisit(), super.hasLocalVisit(), this.getMethod().getSignature()
                            );
    }
}

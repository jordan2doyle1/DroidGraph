package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

import java.io.Serializable;

/**
 * @author Jordan Doyle
 */

public class LifecycleVertex extends MethodVertex implements Serializable {

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
        return String.format("Lifecycle{label='%s', visit=%s, localVisit=%s, method=%s}", super.getLabel(),
                super.hasVisit(), super.hasLocalVisit(), this.getMethod().getSignature()
                            );
    }
}

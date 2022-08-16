package phd.research.vertices;

import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Type;
import soot.SootMethod;

import java.io.Serializable;

/**
 * @author Jordan Doyle
 */

public class DummyVertex extends MethodVertex implements Serializable {

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
        return String.format("Dummy{label='%s', visit=%s, localVisit=%s, method=%s}", super.getLabel(),
                super.hasVisit(), super.hasLocalVisit(), this.getMethod().getSignature()
                            );
    }
}

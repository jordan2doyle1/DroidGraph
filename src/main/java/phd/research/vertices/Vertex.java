package phd.research.vertices;

import org.jgrapht.nio.Attribute;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;

import java.util.Map;

/**
 * @author Jordan Doyle
 */

public interface Vertex {

    int getId();

    Type getType();

    boolean hasVisit();

    void visit();

    void visitReset();

    boolean hasLocalVisit();

    void localVisit();

    void localVisitReset();

    Map<String, Attribute> getAttributes();

    Color getColor();

    Shape getShape();

    Style getStyle();
}

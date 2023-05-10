package phd.research.vertices;

import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Doyle
 */

public class ListenerVertexTest {

    private ListenerVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        this.vertex = new ListenerVertex(MethodVertexTest.SIGNATURE);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong type returned.", Type.LISTENER, this.vertex.getType());
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, this.vertex.getMethodSignature());
    }

    @Test
    public void testBaseConstructor() {
        ListenerVertex listenerVertex = new ListenerVertex(45, MethodVertexTest.SIGNATURE);
        assertEquals("Wrong type returned.", Type.LISTENER, listenerVertex.getType());
        assertEquals("Wrong id returned.", 45, listenerVertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, listenerVertex.getMethodSignature());
    }

    @Test(expected = NullPointerException.class)
    public void testMethodNullException() {
        new ListenerVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.LISTENER.name(), attributes.get("type").getValue());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE,
                attributes.get("method").getValue()
                    );
        assertEquals("Wrong color attribute returned.", Color.ORANGE.name(), attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.OCTAGON.name(), attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "ListenerVertex{id=0, type=LISTENER, methodSignature='" + MethodVertexTest.SIGNATURE +
                        "', visit=false, " + "localVisit=false}", this.vertex.toString()
                    );
    }
}
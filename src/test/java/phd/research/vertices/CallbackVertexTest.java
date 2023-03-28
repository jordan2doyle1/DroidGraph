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

public class CallbackVertexTest {

    private CallbackVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        this.vertex = new CallbackVertex(MethodVertexTest.SIGNATURE);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong type returned.", Type.CALLBACK, this.vertex.getType());
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, this.vertex.getMethodSignature());
    }

    @Test
    public void testBaseConstructor() {
        CallbackVertex callbackVertex = new CallbackVertex(45, MethodVertexTest.SIGNATURE);
        assertEquals("Wrong type returned.", Type.CALLBACK, callbackVertex.getType());
        assertEquals("Wrong id returned.", 45, callbackVertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, callbackVertex.getMethodSignature());
    }

    @Test(expected = NullPointerException.class)
    public void testMethodNullException() {
        new CallbackVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();
        assertEquals("Wrong number of attributes returned.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.CALLBACK.name(), attributes.get("type").getValue());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE,
                attributes.get("method").getValue()
                    );
        assertEquals("Wrong color attribute returned.", Color.PURPLE.name(), attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.HEXAGON.name(), attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attributes.get("style").getValue());
    }


    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "CallbackVertex{id=0, type=CALLBACK, methodSignature='" + MethodVertexTest.SIGNATURE +
                        "', visit=false, localVisit=false}", this.vertex.toString()
                    );
    }
}

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

public class LifecycleVertexTest {

    private LifecycleVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        this.vertex = new LifecycleVertex(MethodVertexTest.SIGNATURE);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong type returned.", Type.LIFECYCLE, this.vertex.getType());
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, this.vertex.getMethodSignature());
    }

    @Test
    public void testBaseConstructor() {
        LifecycleVertex lifecycleVertex = new LifecycleVertex(45, MethodVertexTest.SIGNATURE);
        assertEquals("Wrong type returned.", Type.LIFECYCLE, lifecycleVertex.getType());
        assertEquals("Wrong id returned.", 45, lifecycleVertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, lifecycleVertex.getMethodSignature());
    }

    @Test(expected = NullPointerException.class)
    public void testMethodNullException() {
        new LifecycleVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.LIFECYCLE.name(), attributes.get("type").getValue());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE,
                attributes.get("method").getValue()
                    );
        assertEquals("Wrong color attribute returned.", Color.BLUE.name(), attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.DIAMOND.name(), attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attributes.get("style").getValue());
    }


    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "LifecycleVertex{id=0, type=LIFECYCLE, methodSignature='" + MethodVertexTest.SIGNATURE +
                        "', visit=false, " + "localVisit=false}", this.vertex.toString()
                    );
    }
}
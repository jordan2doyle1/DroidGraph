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

public class DummyVertexTest {

    private DummyVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();

        this.vertex = new DummyVertex(MethodVertexTest.SIGNATURE);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong type returned.", Type.DUMMY, this.vertex.getType());
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, this.vertex.getMethodSignature());
    }

    @Test
    public void testBaseConstructor() {
        DummyVertex dummyVertex = new DummyVertex(45, MethodVertexTest.SIGNATURE);
        assertEquals("Wrong type returned.", Type.DUMMY, dummyVertex.getType());
        assertEquals("Wrong id returned.", 45, dummyVertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, dummyVertex.getMethodSignature());
    }

    @Test(expected = NullPointerException.class)
    public void testMethodNullException() {
        new DummyVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();
        assertEquals("Wrong number of attributes returned.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.DUMMY.name(), attributes.get("type").getValue());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE,
                attributes.get("method").getValue()
                    );
        assertEquals("Wrong color attribute returned.", Color.BLACK.name(), attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.PARALLELOGRAM.name(), attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attributes.get("style").getValue());
    }


    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "DummyVertex{id=0, type=DUMMY, methodSignature='" + MethodVertexTest.SIGNATURE + "', visit=false, " +
                        "localVisit=false}", this.vertex.toString()
                    );
    }
}
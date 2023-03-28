package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
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

public class MethodVertexTest {

    public static final String SIGNATURE =
            "<com.example.android.lifecycle.ActivityA: void onClick()(android.view.View)>";

    private MethodVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        this.vertex = new MethodVertex(MethodVertexTest.SIGNATURE);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong type returned.", Type.METHOD, this.vertex.getType());
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, this.vertex.getMethodSignature());
    }

    @Test
    public void testBaseConstructor() {
        MethodVertex methodVertex = new MethodVertex(45, MethodVertexTest.SIGNATURE);
        assertEquals("Wrong type returned.", Type.METHOD, methodVertex.getType());
        assertEquals("Wrong id returned.", 45, methodVertex.getId());
        assertEquals("Wrong method returned.", MethodVertexTest.SIGNATURE, methodVertex.getMethodSignature());
    }

    @Test(expected = NullPointerException.class)
    public void testMethodNullException() {
        new MethodVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();

        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.METHOD.name(), attributes.get("type").getValue());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE,
                attributes.get("method").getValue()
                    );
        assertEquals("Wrong color attribute returned.", Color.GREEN.name(), attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.ELLIPSE.name(), attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "MethodVertex{id=0, type=METHOD, methodSignature='" + MethodVertexTest.SIGNATURE +
                        "', visit=false, localVisit=false}", this.vertex.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(MethodVertex.class).withRedefinedSuperclass().withIgnoredFields("visit", "localVisit")
                .verify();
    }
}
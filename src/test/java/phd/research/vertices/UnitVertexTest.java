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

public class UnitVertexTest {

    private final String UNIT = "$r0 := @this <com.example.android.lifecycle.ActivityA: init()()>";

    private UnitVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        this.vertex = new UnitVertex(MethodVertexTest.SIGNATURE, this.UNIT);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong type returned.", Type.UNIT, this.vertex.getType());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE, this.vertex.getMethodSignature());
        assertEquals("Wrong unit returned.", this.UNIT, this.vertex.getUnit());
    }

    @Test
    public void testBaseConstructor() {
        UnitVertex unitVertex = new UnitVertex(45, MethodVertexTest.SIGNATURE, this.UNIT);
        assertEquals("Wrong id returned.", 45, unitVertex.getId());
        assertEquals("Wrong type returned.", Type.UNIT, unitVertex.getType());
        assertEquals("Wrong method signature returned.", MethodVertexTest.SIGNATURE, unitVertex.getMethodSignature());
        assertEquals("Wrong unit returned.", this.UNIT, unitVertex.getUnit());
    }

    @Test(expected = NullPointerException.class)
    public void testMethodNullException() {
        new UnitVertex(null, this.UNIT);
    }

    @Test(expected = NullPointerException.class)
    public void testUnitNullException() {
        new UnitVertex(MethodVertexTest.SIGNATURE, null);
    }

    @Test
    public void testGetAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 6, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.UNIT.name(), attributes.get("type").getValue());
        assertEquals("Wrong type attribute returned.", this.UNIT, attributes.get("unit").getValue());
        assertEquals("Wrong type attribute returned.", MethodVertexTest.SIGNATURE, attributes.get("method").getValue());
        assertEquals("Wrong color attribute returned.", Color.YELLOW.name(), attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.BOX.name(), attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "UnitVertex{id=0, type=UNIT, methodSignature='" + MethodVertexTest.SIGNATURE + "', unit='" + this.UNIT +
                        "', visit=false, localVisit=false}", this.vertex.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(UnitVertex.class).withRedefinedSuperclass().withIgnoredFields("visit", "localVisit")
                .verify();
    }
}
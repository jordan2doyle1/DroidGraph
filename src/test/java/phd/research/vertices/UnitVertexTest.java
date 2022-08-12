package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import soot.Unit;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class UnitVertexTest {

    // TODO: All tests failing because I can't mock a toString() method. Need to find a better way of adding the unit
    //  object to the label and toString() method.

    private final String SUMMARY = "";
    private final String LABEL = "Unit{unit=}";

    UnitVertex v;

    @Before
    public void setUp() {
        Unit unit = mock(Unit.class);
        v = new UnitVertex(unit);
    }

    @Test
    public void testConstructor() {
        assertEquals("Type should be 'unit'.", Type.unit, this.v.getType());
        assertEquals("Wrong label returned.", LABEL, this.v.getLabel());
        assertEquals("Wrong unit returned.", SUMMARY, this.v.getUnit().toString());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new UnitVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", "unit", attributes.get("type").getValue());
        assertEquals("Wrong label attribute returned.", LABEL, attributes.get("label").getValue());
        assertEquals("Wrong color attribute returned.", "yellow", attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", "box", attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "Unit{label='" + LABEL + "', visit=false, localVisit=false, unit=" + SUMMARY + "}", this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Vertex.class).withRedefinedSuperclass().verify();
    }
}
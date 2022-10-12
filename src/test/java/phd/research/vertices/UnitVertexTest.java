package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import soot.Unit;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author Jordan Doyle
 */

public class UnitVertexTest {

    private final String LABEL_REGEX = "Unit\\{unit=.+}";

    private UnitVertex v;

    @Before
    public void setUp() {
        Unit unit = mock(Unit.class);
        this.v = new UnitVertex(unit);
    }

    @Test
    public void testConstructor() {
        assertEquals("Type should be 'unit'.", Type.UNIT, this.v.getType());
        assertTrue("Wrong label returned.", this.v.getLabel().matches(LABEL_REGEX));
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new UnitVertex(null);
    }

    @Test
    public void testGetUnit() {
        assertNotNull("Unit is null.", this.v.getUnit());
    }

    @Test
    public void testGetAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", "unit", attributes.get("type").getValue());
        assertTrue("Wrong label attribute returned.", attributes.get("label").getValue().matches(LABEL_REGEX));
        assertEquals("Wrong color attribute returned.", "yellow", attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", "box", attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        String TO_STRING_REGEX = "Unit\\{label='Unit\\{unit=.+}', visit=false, localVisit=false, unit=.+}";
        assertTrue("Wrong string value returned.", this.v.toString().matches(TO_STRING_REGEX));
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(UnitVertex.class).withRedefinedSuperclass().withIgnoredFields("visit", "localVisit")
                .suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
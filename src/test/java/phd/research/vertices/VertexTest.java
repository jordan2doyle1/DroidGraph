package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Jordan Doyle
 */

public class VertexTest {

    private final String LABEL = "Vertex{type=UNKNOWN}";

    private Vertex v;

    @Before
    public void setUp() {
        this.v = new Vertex(Type.UNKNOWN, LABEL);
    }

    @Test
    public void testSimpleConstructor() {
        Vertex s = new Vertex(Type.UNKNOWN);
        assertEquals("Wrong type returned.", Type.UNKNOWN, s.getType());
        assertEquals("Wrong label returned.", LABEL, s.getLabel());
    }

    @Test
    public void testBaseConstructor() {
        assertEquals("Wrong type returned.", Type.UNKNOWN, this.v.getType());
        assertEquals("Wrong label returned.", LABEL, this.v.getLabel());
    }

    @Test(expected = NullPointerException.class)
    public void testTypeNullException() {
        new Vertex(null, LABEL);
    }

    @Test(expected = NullPointerException.class)
    public void testLabelNullException() {
        new Vertex(Type.UNKNOWN, null);
    }

    @Test
    public void testSetLabel() {
        this.v.setLabel("Vertex{}");
        assertEquals("Wrong label returned.", "Vertex{}", this.v.getLabel());
    }

    @Test
    public void visitReset() {
        this.v.visit();
        assertTrue("Visit should be true.", this.v.hasVisit());
        this.v.visitReset();
        assertFalse("Visit should be false.", this.v.hasVisit());
    }

    @Test
    public void localVisitReset() {
        this.v.localVisit();
        assertTrue("Local visit should be true.", this.v.hasLocalVisit());
        this.v.localVisitReset();
        assertFalse("Local visit should be false.", this.v.hasLocalVisit());
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();
        assertEquals("Should be exactly 2 attributes.", 2, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.UNKNOWN.name(), attributes.get("type").getValue());
        assertEquals("Wrong label attribute returned.", LABEL, attributes.get("label").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                String.format("Vertex{type=UNKNOWN, label='%s', visit=false, localVisit=false}", LABEL),
                this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Vertex.class).withIgnoredFields("visit", "localVisit")
                .withRedefinedSubclass(ControlVertex.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;

import java.util.Map;

import static org.junit.Assert.*;

public class VertexTest {

    Vertex v;

    @Before
    public void setUp() {
        v = new Vertex(Type.other, "Test");
    }

    @Test
    public void testConstructor() {
        assertEquals("Type should be 'other'.", Type.other, this.v.getType());
        assertEquals("Label should be 'Test'.", "Test", this.v.getLabel());
    }

    @Test(expected = NullPointerException.class)
    public void testTypeNullException() {
        new Vertex(null, "Test");
    }

    @Test(expected = NullPointerException.class)
    public void testLabelNullException() {
        new Vertex(Type.other, null);
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
        assertEquals("Type attribute value should be 'other'.", "other", attributes.get("type").getValue());
        assertEquals("Label attribute value should be 'Test'.", "Test", attributes.get("label").getValue());

    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.", "Vertex{type=other, label='Test', visit=false, localVisit=false}",
                this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Vertex.class).withIgnoredFields("visit", "localVisit")
                .withRedefinedSubclass(ControlVertex.class).verify();
    }
}
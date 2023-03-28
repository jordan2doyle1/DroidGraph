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

public class DefaultVertexTest {

    private DefaultVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        this.vertex = new DefaultVertex(Type.METHOD);
    }

    @Test
    public void testSimpleConstructor() {
        assertEquals("Wrong ID returned.", 0, this.vertex.getId());
        assertEquals("Wrong type returned.", Type.METHOD, this.vertex.getType());
    }

    @Test
    public void testBaseConstructor() {
        DefaultVertex defaultVertex = new DefaultVertex(30, Type.METHOD);

        assertEquals("Wrong ID returned.", 30, defaultVertex.getId());
        assertEquals("Wrong type returned.", Type.METHOD, defaultVertex.getType());
    }

    @Test(expected = NullPointerException.class)
    public void testTypeNullException() {
        new DefaultVertex(null);
    }

    @Test
    public void visitReset() {
        this.vertex.visit();
        assertTrue("Visit should be true.", this.vertex.hasVisit());
        this.vertex.visitReset();
        assertFalse("Visit should be false.", this.vertex.hasVisit());
    }

    @Test
    public void localVisitReset() {
        this.vertex.localVisit();
        assertTrue("Local visit should be true.", this.vertex.hasLocalVisit());
        this.vertex.localVisitReset();
        assertFalse("Local visit should be false.", this.vertex.hasLocalVisit());
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.vertex.getAttributes();
        assertEquals("Wrong number of attributes returned.", 4, attributes.size());
        assertEquals("Wrong type attribute returned.", Type.METHOD.name(), attributes.get("type").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.", "DefaultVertex{id=0, type=METHOD, visit=false, localVisit=false}",
                this.vertex.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(DefaultVertex.class).withIgnoredFields("visit", "localVisit")
                .withRedefinedSubclass(ControlVertex.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Color;
import phd.research.enums.Shape;
import phd.research.enums.Style;
import phd.research.enums.Type;
import phd.research.graph.Control;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author Jordan Doyle
 */

public class ControlVertexTest {

    private final int RESOURCE_ID = 123456789;
    private final String CONTROL_NAME = "btn_click_A";
    private final String LAYOUT_NAME = "activity_a.xml";
    private final String ACTIVITY = "com.example.lifecycle.ActivityA";

    private ControlVertex vertex;

    @Before
    public void setUp() {
        DefaultVertex.resetIdSequence();
        Control control =
                new Control(this.RESOURCE_ID, this.CONTROL_NAME, this.RESOURCE_ID, this.LAYOUT_NAME, this.ACTIVITY,
                        Collections.emptyList()
                );
        this.vertex = new ControlVertex(control);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong type returned.", Type.CONTROL, this.vertex.getType());
        assertEquals("Wrong id returned.", 0, this.vertex.getId());
        assertEquals("Wrong control returned.", this.CONTROL_NAME, this.vertex.getControl().getControlName());
    }

    @Test
    public void testBaseConstructor() {
        Control control =
                new Control(this.RESOURCE_ID, this.CONTROL_NAME, this.RESOURCE_ID, this.LAYOUT_NAME, this.ACTIVITY,
                        Collections.emptyList()
                );
        ControlVertex controlVertex = new ControlVertex(45, control);

        assertEquals("Wrong type returned.", Type.CONTROL, controlVertex.getType());
        assertEquals("Wrong id returned.", 45, controlVertex.getId());
        assertEquals("Wrong control returned.", this.CONTROL_NAME, controlVertex.getControl().getControlName());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new ControlVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attr = this.vertex.getAttributes();

        assertEquals("Wrong number of attributes returned.", 10, attr.size());
        assertEquals("Wrong control ID returned.", String.valueOf(this.RESOURCE_ID), attr.get("controlId").getValue());
        assertEquals("Wrong control name returned.", this.CONTROL_NAME, attr.get("control").getValue());
        assertEquals("Wrong layout ID returned.", String.valueOf(this.RESOURCE_ID), attr.get("layoutId").getValue());
        assertEquals("Wrong layout name returned.", this.LAYOUT_NAME, attr.get("layout").getValue());
        assertEquals("Wrong activity returned.", this.ACTIVITY, attr.get("activity").getValue());
        assertEquals("Wrong color attribute returned.", Color.RED.name(), attr.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", Shape.CIRCLE.name(), attr.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", Style.FILLED.name(), attr.get("style").getValue());
        assertEquals("Wrong listener list returned.", Collections.emptyList().toString(),
                attr.get("listeners").getValue()
                    );
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "ControlVertex{id=0, type=CONTROL, control='Control{controlId=" + this.RESOURCE_ID + ", layoutId=" +
                        this.RESOURCE_ID + ", controlName='" + this.CONTROL_NAME + "', layoutName='" +
                        this.LAYOUT_NAME + "', activity='" + this.ACTIVITY + "', listeners=" + Collections.emptyList() +
                        "}', visit=false, localVisit=false}", this.vertex.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(ControlVertex.class).withRedefinedSuperclass().withIgnoredFields("visit", "localVisit")
                .verify();
    }
}
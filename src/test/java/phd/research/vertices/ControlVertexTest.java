package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import phd.research.graph.Control;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControlVertexTest {

    private final String RESOURCE_NAME = "btn_click_A";
    private final int RESOURCE_ID = 123456789;
    private final String SUMMARY = RESOURCE_NAME + "(" + RESOURCE_ID + ")";
    private final String LABEL = "Control{control=" + SUMMARY + "}";

    ControlVertex v;

    @Before
    public void setUp() {
        ARSCFileParser.AbstractResource controlResource = mock(ARSCFileParser.AbstractResource.class);
        when(controlResource.getResourceName()).thenReturn(RESOURCE_NAME);
        when(controlResource.getResourceID()).thenReturn(RESOURCE_ID);

        Control control = mock(Control.class);
        when(control.getControlResource()).thenReturn(controlResource);

        v = new ControlVertex(control);
    }

    @Test
    public void testConstructor() {
        assertEquals("Type should be 'control'.", Type.control, this.v.getType());
        assertEquals("Wrong label returned.", LABEL, this.v.getLabel());
        assertEquals("Wrong resource returned.", RESOURCE_NAME,
                this.v.getControl().getControlResource().getResourceName()
                    );
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new ControlVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", "control", attributes.get("type").getValue());
        assertEquals("Wrong label attribute returned.", LABEL, attributes.get("label").getValue());
        assertEquals("Wrong color attribute returned.", "red", attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", "circle", attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "Control{label='" + LABEL + "', visit=false, localVisit=false, control=" + SUMMARY + "}",
                this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(ControlVertex.class).withRedefinedSuperclass()
                .withPrefabValues(SootClass.class, mock(SootClass.class), mock(SootClass.class))
                .withPrefabValues(SootMethod.class, mock(SootMethod.class), mock(SootMethod.class))
                .withIgnoredFields("visit", "localVisit").suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
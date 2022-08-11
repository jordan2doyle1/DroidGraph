package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import phd.research.helper.Control;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControlVertexTest {

    ControlVertex v;

    @Before
    public void setUp() {
        ARSCFileParser.AbstractResource controlResource = mock(ARSCFileParser.AbstractResource.class);
        when(controlResource.getResourceName()).thenReturn("btn_click_A");
        when(controlResource.getResourceID()).thenReturn(123456789);
        Control control = mock(Control.class);
        when(control.getControlResource()).thenReturn(controlResource);
        //when(control.toString()).thenReturn("Control: (btn_click_A, activity_A, ActivityA, null)");
        v = new ControlVertex(control);
    }

    @Test
    public void testConstructor() {
        assertEquals("Resource name should be 'btnStartA'.", "btn_click_A",
                this.v.getControl().getControlResource().getResourceName()
                    );
        assertEquals("Type should be 'control'.", Type.control, this.v.getType());
        assertEquals("Label should be 'btn_click_A (123456789)'.", "btn_click_A (123456789)", this.v.getLabel());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new ControlVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();

        assertEquals("Should be exactly 2 attributes.", 5, attributes.size());
        assertEquals("Type attribute value should be 'control'.", "control", attributes.get("type").getValue());
        assertEquals("Label attribute value should be 'btn_click_A (123456789)'.", "btn_click_A (123456789)",
                attributes.get("label").getValue()
                    );
        assertEquals("Color attribute value should be 'red'.", "red", attributes.get("color").getValue());
        assertEquals("Shape attribute value should be 'circle'.", "circle", attributes.get("shape").getValue());
        assertEquals("Style attribute value should be 'filled'.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "ControlVertex{type=control, label='btn_click_A (123456789)', visit=false, localVisit=false}",
                this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Vertex.class).withRedefinedSuperclass().verify();
    }

}
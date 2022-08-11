package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import soot.Unit;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnitVertexTest {

    UnitVertex v;

    @Before
    public void setUp() {
        ARSCFileParser.AbstractResource controlResource = mock(ARSCFileParser.AbstractResource.class);
        when(controlResource.getResourceName()).thenReturn("btn_click_A");
        when(controlResource.getResourceID()).thenReturn(123456789);
        Unit unit = mock(Unit.class);
        //when(control.getControlResource()).thenReturn(controlResource);
        //when(control.toString()).thenReturn("Control: (btn_click_A, activity_A, ActivityA, null)");
        v = new UnitVertex(unit);
    }

    @Test
    public void testConstructor() {
        assertEquals("Unit should be ''.", "", this.v.getUnit());
        assertEquals("Type should be 'unit'.", Type.unit, this.v.getType());
        assertEquals("Label should be ''.", "", this.v.getLabel());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new UnitVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();

        assertEquals("Should be exactly 2 attributes.", 5, attributes.size());
        assertEquals("Type attribute value should be 'unit'.", "unit", attributes.get("type").getValue());
        assertEquals("Label attribute value should be ''.", "", attributes.get("label").getValue());
        assertEquals("Color attribute value should be 'yellow'.", "yellow", attributes.get("color").getValue());
        assertEquals("Shape attribute value should be 'box'.", "box", attributes.get("shape").getValue());
        assertEquals("Style attribute value should be 'filled'.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.", "UnitVertex{type=unit, label='', visit=false, localVisit=false}",
                this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(Vertex.class).withRedefinedSuperclass().verify();
    }

}
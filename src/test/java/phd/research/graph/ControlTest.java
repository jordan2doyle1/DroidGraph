package phd.research.graph;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jordan Doyle
 */

public class ControlTest {

    private final String CONTROL_NAME = "btn_click_A";
    private final String LAYOUT_NAME = "activity_A.xml";
    private final String ACTIVITY_NAME = ".ActivityA";
    private final String METHOD_NAME = "onClick()";

    private Control c;

    @Before
    public void setUp() {
        ARSCFileParser.AbstractResource control = mock(ARSCFileParser.AbstractResource.class);
        when(control.getResourceName()).thenReturn(CONTROL_NAME);
        ARSCFileParser.AbstractResource layout = mock(ARSCFileParser.AbstractResource.class);
        when(layout.getResourceName()).thenReturn(LAYOUT_NAME);
        SootClass activity = mock(SootClass.class);
        when(activity.getShortName()).thenReturn(ACTIVITY_NAME);

        this.c = new Control(control, layout, activity, null);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong control name.", CONTROL_NAME, this.c.getControlResource().getResourceName());
        assertEquals("Wrong layout name.", LAYOUT_NAME, this.c.getLayoutResource().getResourceName());
        assertEquals("Wrong activity name.", ACTIVITY_NAME, this.c.getControlActivity().getShortName());
        assertNull("Expected click listener to be null.", this.c.getClickListener());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new Control(null, mock(ARSCFileParser.AbstractResource.class), mock(SootClass.class), null);
    }

    @Test(expected = NullPointerException.class)
    public void testLayoutNullException() {
        new Control(mock(ARSCFileParser.AbstractResource.class), null, mock(SootClass.class), null);
    }

    @Test(expected = NullPointerException.class)
    public void testActivityNullException() {
        ARSCFileParser.AbstractResource resource = mock(ARSCFileParser.AbstractResource.class);
        new Control(resource, resource, null, null);
    }

    @Test
    public void testSetClickListener() {
        assertNull("Expected click listener to be null.", this.c.getClickListener());

        SootMethod method = mock(SootMethod.class);
        when(method.getName()).thenReturn(METHOD_NAME);

        this.c.setClickListener(method);
        assertEquals("Wrong method name returned.", METHOD_NAME, this.c.getClickListener().getName());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned with null.",
                String.format("Control{control=%s, layout=%s, activity=%s, clickListener=null}", CONTROL_NAME,
                        LAYOUT_NAME, ACTIVITY_NAME
                             ), this.c.toString()
                    );

        SootMethod method = mock(SootMethod.class);
        when(method.getName()).thenReturn(METHOD_NAME);

        this.c.setClickListener(method);
        assertEquals("Wrong string value returned without null.",
                String.format("Control{control=%s, layout=%s, activity=%s, clickListener=%s}", CONTROL_NAME,
                        LAYOUT_NAME, ACTIVITY_NAME, METHOD_NAME
                             ), this.c.toString()
                    );
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Control.class)
                .withPrefabValues(SootClass.class, mock(SootClass.class), mock(SootClass.class))
                .withPrefabValues(SootMethod.class, mock(SootMethod.class), mock(SootMethod.class))
                .withIgnoredFields("clickListener").verify();
    }
}
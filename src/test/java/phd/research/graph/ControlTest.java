package phd.research.graph;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jordan Doyle
 */

public class ControlTest {

    private final String CONTROL_NAME = "btn_click_A";
    private final String LAYOUT_NAME = "activity_A.xml";
    private final String ACTIVITY_NAME = ".ActivityA";

    private Control c;

    @Before
    public void setUp() {
        ARSCFileParser.AbstractResource control = mock(ARSCFileParser.AbstractResource.class);
        when(control.getResourceName()).thenReturn(CONTROL_NAME);
        ARSCFileParser.AbstractResource layout = mock(ARSCFileParser.AbstractResource.class);
        when(layout.getResourceName()).thenReturn(LAYOUT_NAME);
        SootClass activity = mock(SootClass.class);
        when(activity.getShortName()).thenReturn(ACTIVITY_NAME);

        this.c = new Control(control, layout, activity, new ArrayList<>());
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong control name.", CONTROL_NAME, this.c.getControlResource().getResourceName());
        assertEquals("Wrong layout name.", LAYOUT_NAME, this.c.getLayoutResource().getResourceName());
        assertEquals("Wrong activity name.", ACTIVITY_NAME, this.c.getControlActivity().getShortName());
        assertTrue("Expected listeners to be empty list.", this.c.getClickListeners().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new Control(null, mock(ARSCFileParser.AbstractResource.class), mock(SootClass.class), new ArrayList<>());
    }

    @Test(expected = NullPointerException.class)
    public void testLayoutNullException() {
        new Control(mock(ARSCFileParser.AbstractResource.class), null, mock(SootClass.class), new ArrayList<>());
    }

    @Test(expected = NullPointerException.class)
    public void testActivityNullException() {
        ARSCFileParser.AbstractResource resource = mock(ARSCFileParser.AbstractResource.class);
        new Control(resource, resource, null, new ArrayList<>());
    }

    @Test(expected = NullPointerException.class)
    public void testListenersNullException() {
        new Control(mock(ARSCFileParser.AbstractResource.class), mock(ARSCFileParser.AbstractResource.class),
                mock(SootClass.class), null
        );
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned with no listeners.",
                String.format("Control{control=%s, layout=%s, activity=%s, clickListener=[]}", CONTROL_NAME,
                        LAYOUT_NAME, ACTIVITY_NAME
                             ), this.c.toString()
                    );

        SootMethod method = mock(SootMethod.class);
        when(method.getName()).thenReturn("onClick()");
        this.c.setClickListeners(new ArrayList<>(Collections.singletonList(method)));

        assertEquals("Wrong string value returned with listeners.",
                String.format("Control{control=%s, layout=%s, activity=%s, clickListener=[onClick()]}", CONTROL_NAME,
                        LAYOUT_NAME, ACTIVITY_NAME
                             ), this.c.toString()
                    );
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Control.class)
                .withPrefabValues(SootClass.class, mock(SootClass.class), mock(SootClass.class))
                .withPrefabValues(SootMethod.class, mock(SootMethod.class), mock(SootMethod.class))
                .suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
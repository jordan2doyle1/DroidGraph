package phd.research.graph;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jordan Doyle
 */

public class ControlTest {

    private final String CONTROL_NAME = "btn_click_A";
    private final String LAYOUT_NAME = "activity_a.xml";
    private final int RESOURCE_ID = 123456789;
    private final String ACTIVITY = "com.example.lifecycle.ActivityA";

    private Control control;

    @Before
    public void setUp() {
        this.control =
                new Control(RESOURCE_ID, CONTROL_NAME, RESOURCE_ID, LAYOUT_NAME, ACTIVITY, Collections.emptyList());
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong control ID.", RESOURCE_ID, this.control.getControlId());
        assertEquals("Wrong control name.", CONTROL_NAME, this.control.getControlName());
        assertEquals("Wrong layout ID.", RESOURCE_ID, this.control.getLayoutId());
        assertEquals("Wrong layout name.", LAYOUT_NAME, this.control.getLayoutName());
        assertEquals("Wrong activity name.", ACTIVITY, this.control.getActivity());
        assertTrue("Expected listeners to be empty list.", this.control.getListeners().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNameNullException() {
        new Control(RESOURCE_ID, null, RESOURCE_ID, LAYOUT_NAME, ACTIVITY, Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void testLayoutNameNullException() {
        new Control(RESOURCE_ID, CONTROL_NAME, RESOURCE_ID, null, ACTIVITY, Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void testActivityNullException() {
        new Control(RESOURCE_ID, CONTROL_NAME, RESOURCE_ID, LAYOUT_NAME, null, Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void testListenersNullException() {
        new Control(RESOURCE_ID, CONTROL_NAME, RESOURCE_ID, LAYOUT_NAME, ACTIVITY, null);
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned with no listeners.",
                "Control{controlId=" + RESOURCE_ID + ", layoutId=" + RESOURCE_ID + ", controlName='" + CONTROL_NAME +
                        "', layoutName='" + LAYOUT_NAME + "', activity='" + ACTIVITY + "', listeners=" +
                        Collections.emptyList() + "}", this.control.toString()
                    );

        List<String> listeners =
                new ArrayList<>(Collections.singletonList("com.example.lifecycle.ActivityA: void onClick()"));
        this.control.setListeners(listeners);

        assertEquals("Wrong string value returned with listeners.",
                "Control{controlId=" + RESOURCE_ID + ", layoutId=" + RESOURCE_ID + ", controlName='" + CONTROL_NAME +
                        "', layoutName='" + LAYOUT_NAME + "', activity='" + ACTIVITY + "', listeners=" + listeners +
                        "}", this.control.toString()
                    );
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Control.class).suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
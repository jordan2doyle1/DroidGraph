package phd.research.helper;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControlTest {

    @Test
    public void testConstructor() {
        ARSCFileParser.AbstractResource control = mock(ARSCFileParser.AbstractResource.class);
        when(control.getResourceName()).thenReturn("btn_click_A");
        ARSCFileParser.AbstractResource layout = mock(ARSCFileParser.AbstractResource.class);
        when(layout.getResourceName()).thenReturn("activity_A.xml");
        SootClass activity = mock(SootClass.class);
        when(activity.getShortName()).thenReturn(".ActivityA");

        Control c = new Control(control, layout, activity, null);

        assertEquals("Wrong Control Resource Name", "btn_click_A", c.getControlResource().getResourceName());
        assertEquals("Wrong Layout Resource Name", "activity_A.xml", c.getLayoutResource().getResourceName());
        assertEquals("Wrong Activity Name", ".ActivityA", c.getControlActivity().getShortName());
        assertNull("Expected Null", c.getClickListener());
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
        new Control(mock(ARSCFileParser.AbstractResource.class), mock(ARSCFileParser.AbstractResource.class), null,
                null
        );
    }

    @Test
    public void testSetClickListener() {
        Control c = new Control(mock(ARSCFileParser.AbstractResource.class), mock(ARSCFileParser.AbstractResource.class),
                        mock(SootClass.class), null);
        assertNull("Expected Null", c.getClickListener());

        SootMethod method = mock(SootMethod.class);
        when(method.getName()).thenReturn("onClick()");

        c.setClickListener(method);

        assertEquals("Wrong Method Name", "onClick()", c.getClickListener().getName());
    }

    @Test
    public void testToString() {
        ARSCFileParser.AbstractResource control = mock(ARSCFileParser.AbstractResource.class);
        when(control.getResourceName()).thenReturn("btn_click_A");
        ARSCFileParser.AbstractResource layout = mock(ARSCFileParser.AbstractResource.class);
        when(layout.getResourceName()).thenReturn("activity_A.xml");
        SootClass activity = mock(SootClass.class);
        when(activity.getName()).thenReturn(".ActivityA");
        SootMethod method = mock(SootMethod.class);
        when(method.getName()).thenReturn("onClick");

        Control c = new Control(control, layout, activity, method);
        assertEquals("toString Failed: Wrong Output", "Control: (btn_click_A, activity_A.xml, .ActivityA, onClick)",
                c.toString());

        c = new Control(control, layout, activity, null);
        assertEquals("toString Failed: Wrong Output", "Control: (btn_click_A, activity_A.xml, .ActivityA, null)",
                c.toString());
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Control.class)
                .withPrefabValues(SootClass.class, mock(SootClass.class), mock(SootClass.class))
                .withPrefabValues(SootMethod.class, mock(SootMethod.class), mock(SootMethod.class))
                .withIgnoredFields("clickListener").verify();
    }

//    @Test
//    public void testSootClassEquality() {
//        EqualsVerifier.forClass(SootClass.class)
//                .withPrefabValues(RefType.class, mock(RefType.class), mock(RefType.class))
//                .withPrefabValues(SootModuleInfo.class, mock(SootModuleInfo.class), mock(SootModuleInfo.class))
//                .withPrefabValues(SootClass.class, mock(SootClass.class), mock(SootClass.class))
//                .withPrefabValues(SootMethod.class, mock(SootMethod.class), mock(SootMethod.class))
//                .suppress(Warning.INHERITED_DIRECTLY_FROM_OBJECT).usingGetClass().withIgnoredFields("name").verify();
//    }
//
//    @Test
//    public void testAbstractResourceEquality() {
//        EqualsVerifier.forClass(ARSCFileParser.AbstractResource.class).suppress(Warning.NONFINAL_FIELDS).usingGetClass()
//                .verify();
//    }
}
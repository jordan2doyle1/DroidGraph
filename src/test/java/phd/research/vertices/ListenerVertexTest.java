package phd.research.vertices;

import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import soot.SootClass;
import soot.SootMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jordan Doyle
 */

public class ListenerVertexTest {

    private final String CLASS_NAME = "com.example.android.lifecycle.ActivityA";
    private final String RETURN_TYPE = "void";
    private final String METHOD_NAME = "onClick()";
    private final String PARAM_TYPE = "android.view.View";
    private final String LABEL = String.format("Listener{method=<ActivityA: %s %s(View)>", RETURN_TYPE, METHOD_NAME);
    private final String SIGNATURE = String.format("<%s: %s %s(%s)>", CLASS_NAME, RETURN_TYPE, METHOD_NAME, PARAM_TYPE);

    private ListenerVertex v;

    @Before
    public void setUp() {
        SootClass clazz = mock(SootClass.class);
        when(clazz.getName()).thenReturn(CLASS_NAME);

        soot.Type returnType = mock(soot.Type.class);
        when(returnType.toString()).thenReturn(RETURN_TYPE);
        soot.Type paramType = mock(soot.Type.class);
        when(paramType.toString()).thenReturn(PARAM_TYPE);

        SootMethod method = mock(SootMethod.class);
        when(method.getSignature()).thenReturn(SIGNATURE);
        when(method.getName()).thenReturn(METHOD_NAME);
        when(method.getDeclaringClass()).thenReturn(clazz);
        when(method.getParameterCount()).thenReturn(1);
        when(method.getReturnType()).thenReturn(returnType);

        List<soot.Type> parameterList = Collections.singletonList(paramType);
        when(method.getParameterTypes()).thenReturn(parameterList);

        this.v = new ListenerVertex(method);
    }

    @Test
    public void testConstructor() {
        assertEquals("Type should be 'listener'.", Type.listener, this.v.getType());
        assertEquals("Wrong label returned.", LABEL, this.v.getLabel());
        assertEquals("Wrong method returned.", METHOD_NAME, this.v.getMethod().getName());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new ListenerVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();
        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", "listener", attributes.get("type").getValue());
        assertEquals("Wrong label attribute returned.", LABEL, attributes.get("label").getValue());
        assertEquals("Wrong color attribute returned.", "orange", attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", "octagon", attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                String.format("Listener{label='%s', visit=false, localVisit=false, method=%s}", LABEL, SIGNATURE),
                this.v.toString()
                    );
    }
}
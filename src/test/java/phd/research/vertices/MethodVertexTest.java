package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.jgrapht.nio.Attribute;
import org.junit.Before;
import org.junit.Test;
import phd.research.enums.Type;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodVertexTest {

    private final String CLASS_NAME = "com_example_android_lifecycle_DummyMainMethod";
    private final String RETURN_TYPE = "void";
    private final String METHOD = "dummyMainMethod";
    private final String PARAM_TYPE = "android.view.View";
    private final String SIGNATURE =
            "<" + CLASS_NAME + ": " + RETURN_TYPE + " " + METHOD + "(" + PARAM_TYPE + "," + PARAM_TYPE + ")>";
    private final String LABEL = "Method{method=<DummyMainMethod: " + RETURN_TYPE + " " + METHOD + "(View,View)>";

    MethodVertex v;

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
        when(method.getName()).thenReturn(METHOD);
        when(method.getDeclaringClass()).thenReturn(clazz);
        when(method.getParameterCount()).thenReturn(2);
        when(method.getReturnType()).thenReturn(returnType);

        List<soot.Type> parameterList = new ArrayList<>();
        parameterList.add(paramType);
        parameterList.add(paramType);
        when(method.getParameterTypes()).thenReturn(parameterList);

        v = new MethodVertex(method);
    }

    @Test
    public void testConstructor() {
        assertEquals("Type should be 'method'.", Type.method, this.v.getType());
        assertEquals("Wrong label returned.", LABEL, this.v.getLabel());
        assertEquals("Wrong method returned.", METHOD, this.v.getMethod().getName());
    }

    @Test(expected = NullPointerException.class)
    public void testControlNullException() {
        new MethodVertex(null);
    }

    @Test
    public void getAttributes() {
        Map<String, Attribute> attributes = this.v.getAttributes();

        assertEquals("Should be exactly 5 attributes.", 5, attributes.size());
        assertEquals("Wrong type attribute returned.", "method", attributes.get("type").getValue());
        assertEquals("Wrong label attribute returned.", LABEL, attributes.get("label").getValue());
        assertEquals("Wrong color attribute returned.", "green", attributes.get("color").getValue());
        assertEquals("Wrong shape attribute returned.", "ellipse", attributes.get("shape").getValue());
        assertEquals("Wrong style attribute returned.", "filled", attributes.get("style").getValue());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "Method{label='" + LABEL + "', visit=false, localVisit=false, method=" + SIGNATURE + "}",
                this.v.toString()
                    );
    }

    @Test
    public void testEquals() {
        //TODO: Test Failing - Redefined superclass: Method{} should not equal superclass instance Vertex{} but it does.
        EqualsVerifier.forClass(MethodVertex.class).withRedefinedSuperclass()
                .withPrefabValues(SootMethod.class, mock(SootMethod.class), mock(SootMethod.class))
                .withIgnoredFields("visit", "localVisit").suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
package phd.research.helper;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TupleTest {

    @Test
    public void testConstructor() {
        Tuple<String, Integer, String> t = new Tuple<>("One", 1, "one");
        assertEquals("Constructor Failed: Left Object Set Wrong.", "One", t.getLeft());
        assertEquals("Constructor Failed: Middle Object Set Wrong.", (Integer) 1, t.getMiddle());
        assertEquals("Constructor Failed: Right Object Set Wrong.", "one", t.getRight());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorLeftNull() {
        new Tuple<>(null, 2, "two");
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorMiddleNull() {
        new Tuple<>("Three", null, "three");
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRightNull() {
        new Tuple<>("Four", 4, null);
    }

    @Test
    public void testToString() {
        Tuple<String, Integer, String> t = new Tuple<>("Five", 5, "five");
        assertEquals("toString Failed: Wrong Output", "Tuple: (Five, 5, five)", t.toString());
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Tuple.class).verify();
    }
}
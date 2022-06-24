package phd.research.helper;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PairTest {

    @Test
    public void testConstructor() {
        Pair<String, Integer> p = new Pair<>("One", 1);
        assertEquals("Constructor Failed: Left Object Set Wrong.", "One", p.getLeft());
        assertEquals("Constructor Failed: Right Object Set Wrong.", (Integer) 1, p.getRight());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorLeftNull() {
        new Pair<>(null, 2);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorRightNull() {
        new Pair<>("Three", null);
    }

    @Test
    public void testToString() {
        Pair<String, Integer> p = new Pair<>("Four", 4);
        assertEquals("toString Failed: Wrong Output", "Pair: (Four, 4)", p.toString());
    }

    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(Pair.class).verify();
    }
}
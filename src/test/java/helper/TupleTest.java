package helper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import phd.research.helper.Tuple;

import static junit.framework.TestCase.*;

public class TupleTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void constructor() {
        Tuple<Integer, Integer, Integer> testTuple = new Tuple<>(1, 2, 3);
        assertEquals("Error", (Integer) 1, testTuple.getLeft());
        assertEquals("Error", (Integer) 2, testTuple.getMiddle());
        assertEquals("Error", (Integer) 3, testTuple.getRight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullConstructor() {
        new Tuple<Integer, Integer, Integer>(null, null, null);
    }

    @Test
    public void getLeft() {
    }

    @Test
    public void setLeft() {
    }

    @Test
    public void getMiddle() {
    }

    @Test
    public void setMiddle() {
    }

    @Test
    public void getRight() {
    }

    @Test
    public void setRight() {
    }

    @Test
    public void testToString() {
        Tuple<Integer, Integer, Integer> testTuple = new Tuple<>(1, 2, 3);
        assertEquals("Error", "(1, 2, 3)", testTuple.toString());
    }

    @Test
    public void testEquals() {
        Tuple<Integer, Integer, Integer> tuple1 = new Tuple<>(1, 2, 3);
        Tuple<Integer, Integer, Integer> tuple2 = new Tuple<>(1, 2, 3);
        Tuple<Integer, Integer, Integer> tuple3 = new Tuple<>(3, 2, 1);

        assertTrue("Error", tuple1.equals(tuple1));
        assertFalse("Error", tuple1.equals("(1, 2, 3)"));
        assertTrue("Error", tuple1.equals(tuple2));
        assertFalse("Error", tuple1.equals(tuple3));
    }

    @Test
    public void testClone() {
    }
}
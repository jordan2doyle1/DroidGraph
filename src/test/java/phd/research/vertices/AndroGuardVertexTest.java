package phd.research.vertices;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Jordan Doyle
 */

public class AndroGuardVertexTest {

    @SuppressWarnings("SpellCheckingInspection")
    private final String BYTECODE = "Lcom/example/android/lifecycle/ActivityA$1;-><init>" +
            "(Lcom/example/android/lifecycle/ActivityA;)V [access_flags=constructor] @ 0x1958";

    private AndroGuardVertex vertex;

    @Before
    public void setUp() {
        this.vertex = new AndroGuardVertex(0, this.BYTECODE, false, false);
    }

    @Test
    public void testConstructor() {
        assertEquals("Wrong ID returned.", 0, this.vertex.getId());
        assertFalse("Wrong external status returned.", this.vertex.isExternal());
        assertEquals("Wrong bytecode signature returned.", this.BYTECODE, this.vertex.getBytecodeSignature());
        assertFalse("Wrong entry point status returned.", this.vertex.isEntryPoint());
        assertEquals("Wrong Jimple signature returned.",
                "<com.example.android.lifecycle.ActivityA$1: void <init>(com.example.android.lifecycle.ActivityA)>",
                this.vertex.getJimpleSignature()
                    );
    }

    @Test
    public void testJimpleRuntimeException() {
        AndroGuardVertex androGuardVertex = new AndroGuardVertex(1, "", false, false);
        assertNull("Jimple signature should be null.", androGuardVertex.getJimpleSignature());
    }

    @Test
    public void testToString() {
        assertEquals("Wrong string value returned.",
                "AndroGuardVertex{id=0, external=false, entryPoint=false, label='" + this.BYTECODE + "'}",
                this.vertex.toString()
                    );
    }

    @Test
    public void testEquals() {
        EqualsVerifier.forClass(AndroGuardVertex.class).withIgnoredFields("jimpleSignature").verify();
    }
}
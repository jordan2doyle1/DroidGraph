package phd.research.vertices;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.utility.Bytecode;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class AndroGuardVertex implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AndroGuardVertex.class);

    private final int id;

    private final boolean external;
    private final boolean entryPoint;

    @NotNull
    private final String bytecodeSignature;

    private String jimpleSignature;

    public AndroGuardVertex(int id, String bytecodeSignature, boolean external, boolean entryPoint) {
        this.id = id;
        this.bytecodeSignature = Objects.requireNonNull(bytecodeSignature);
        this.external = external;
        this.entryPoint = entryPoint;

        try {
            this.jimpleSignature = Bytecode.signatureToJimple(this.bytecodeSignature);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to convert bytecode signature: " + bytecodeSignature);
            this.jimpleSignature = null;
        }
    }

    public int getId() {
        return this.id;
    }

    public boolean isExternal() {
        return this.external;
    }

    public boolean isEntryPoint() {
        return this.entryPoint;
    }

    @NotNull
    public String getBytecodeSignature() {
        return this.bytecodeSignature;
    }

    public String getJimpleSignature() {
        return this.jimpleSignature;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", external=" + external + ", entryPoint=" + entryPoint +
                ", label='" + bytecodeSignature + "'}";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AndroGuardVertex)) {
            return false;
        }

        AndroGuardVertex that = (AndroGuardVertex) o;
        return this.id == that.id && this.external == that.external && this.entryPoint == that.entryPoint &&
                this.bytecodeSignature.equals(that.bytecodeSignature);
    }

    @Override
    public final int hashCode() {
        int result = this.id;
        result = 31 * result + (this.external ? 1 : 0);
        result = 31 * result + (this.entryPoint ? 1 : 0);
        result = 31 * result + this.bytecodeSignature.hashCode();
        return result;
    }
}

package phd.research.helper;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class Tuple<L, M, R> {

    @Nonnull
    private final L left;
    @Nonnull
    private final M middle;
    @Nonnull
    private final R right;

    public Tuple(L left, M middle, R right) {
        this.left = Objects.requireNonNull(left);
        this.middle = Objects.requireNonNull(middle);
        this.right = Objects.requireNonNull(right);
    }

    @Nonnull
    public L getLeft() {
        return this.left;
    }

    @Nonnull
    public M getMiddle() {
        return this.middle;
    }

    @Nonnull
    public R getRight() {
        return this.right;
    }

    @Override
    public String toString() {
        return String.format("%s: (%s, %s, %s)", getClass().getSimpleName(), this.left, this.middle, this.right);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Tuple<?, ?, ?>) {
            return Objects.equals(((Tuple<?, ?, ?>) obj).left, this.left) &&
                    Objects.equals(((Tuple<?, ?, ?>) obj).middle, this.middle) &&
                    Objects.equals(((Tuple<?, ?, ?>) obj).right, this.right);
        }

        return false;
    }

    @Override
    public final int hashCode() {
        return this.left.hashCode() + this.middle.hashCode() + this.right.hashCode();
    }
}

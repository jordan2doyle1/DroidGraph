package phd.research.helper;

/**
 * @author Jordan Doyle
 */
@SuppressWarnings("unused")
public class Pair<L, R> {

    final L left;
    final R right;

    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public L getLeft() {
        return this.left;
    }

    public R getRight() {
        return this.right;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", this.left, this.right);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Pair<?, ?>) {
            return ((Pair<?, ?>) obj).left.equals(this.left) && ((Pair<?, ?>) obj).right.equals(this.right);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<L, R> clone() throws CloneNotSupportedException {
        return (Pair<L, R>) super.clone();
    }
}

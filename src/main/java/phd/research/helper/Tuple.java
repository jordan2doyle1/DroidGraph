package phd.research.helper;

/**
 * @author Jordan Doyle
 */
@SuppressWarnings("unused")
public class Tuple<L, M, R> implements Cloneable {

    L left;
    M middle;
    R right;

    public Tuple(L left, M middle, R right) {
        this.left = left;
        this.middle = middle;
        this.right = right;
    }

    public L getLeft() {
        return this.left;
    }

    public void setLeft(L left) {
        this.left = left;
    }

    public M getMiddle() {
        return this.middle;
    }

    public void setMiddle(M middle) {
        this.middle = middle;
    }

    public R getRight() {
        return this.right;
    }

    public void setRight(R right) {
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s)", this.left, this.middle, this.right);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Tuple<?, ?, ?>) {
            return ((Tuple<?, ?, ?>) obj).left.equals(this.left) && ((Tuple<?, ?, ?>) obj).middle.equals(this.middle) &&
                    ((Tuple<?, ?, ?>) obj).right.equals(this.right);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Tuple<L, M, R> clone() throws CloneNotSupportedException {
        return (Tuple<L, M, R>) super.clone();
    }
}

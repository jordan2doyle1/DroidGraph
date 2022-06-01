package phd.research.jGraph;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Edge {

    private final int id;
    private final Vertex src;
    private final Vertex tgt;

    public Edge(Vertex src, Vertex tgt) {
        this.id = src.hashCode() + tgt.hashCode();
        this.src = src;
        this.tgt = tgt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Edge)) {
            return false;
        }

        Edge edge = (Edge) o;

        return this.id == edge.id && Objects.equals(this.src, edge.src) && Objects.equals(this.tgt, edge.tgt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.src, this.tgt);
    }
}

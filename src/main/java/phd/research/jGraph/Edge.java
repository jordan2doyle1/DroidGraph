package phd.research.jGraph;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */
public class Edge {

    private final int id;
    private final Vertex source;
    private final Vertex target;

    public Edge(Vertex source, Vertex target) {
        this.id = source.hashCode() + target.hashCode();
        this.source = source;
        this.target = target;
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

        return this.id == edge.id && Objects.equals(this.source, edge.source) &&
                Objects.equals(this.target, edge.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.source, this.target);
    }
}

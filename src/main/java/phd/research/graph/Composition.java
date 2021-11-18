package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.jGraph.Vertex;

import java.util.Objects;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class Composition {

    private int edge, vertex, callback, listener, lifecycle, method, dummy, control;

    public Composition(Graph<Vertex, DefaultEdge> graph) {
        this.callback = this.listener = this.lifecycle = this.method = this.dummy = this.control = 0;
        readComposition(graph);
    }

    public void readComposition(Graph<Vertex, DefaultEdge> graph) {
        this.edge = graph.edgeSet().size();

        Set<Vertex> vertices = graph.vertexSet();
        this.vertex = vertices.size();

        for (Vertex vertex : vertices) {
            switch (vertex.getType()) {
                case method:
                    this.method++;
                    break;
                case listener:
                    this.listener++;
                    break;
                case callback:
                    this.callback++;
                    break;
                case lifecycle:
                    this.lifecycle++;
                    break;
                case dummyMethod:
                    this.dummy++;
                    break;
                case control:
                    this.control++;
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return "(Vertex:" + this.vertex + ", Edge:" + this.edge + ", System:" + this.callback + ", Listener:"
                + this.listener + ", Lifecycle:" + this.lifecycle + ", Method:" + this.method + ", Dummy:" + this.dummy
                + ", Interface:" + this.control + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Composition)) {
            return false;
        }

        Composition composition = (Composition) o;
        return this.edge == composition.edge && this.vertex == composition.vertex
                && this.callback == composition.callback && this.listener == composition.listener
                && this.lifecycle == composition.lifecycle && this.method == composition.method
                && this.dummy == composition.dummy && this.control == composition.control;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.edge, this.vertex, this.callback, this.listener, this.lifecycle, this.method,
                this.dummy, this.control);
    }
}

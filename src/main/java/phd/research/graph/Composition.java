package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.helper.StringTable;
import phd.research.vertices.Vertex;

import java.util.Set;

/**
 * @author Jordan Doyle
 */

public class Composition {

    private int edge, vertex, other, listener, lifecycle, method, dummy, control;

    public Composition(Graph<Vertex, DefaultEdge> graph) {
        this.other = this.listener = this.lifecycle = this.method = this.dummy = this.control = 0;
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
                case other:
                    this.other++;
                    break;
                case lifecycle:
                    this.lifecycle++;
                    break;
                case dummy:
                    this.dummy++;
                    break;
                case control:
                    this.control++;
                    break;
            }
        }
    }

    public String toTableString() {
        String[][] data = new String[][]{{"TYPE", "COUNT"}, {"Vertex", String.valueOf(this.vertex)},
                {"Edge", String.valueOf(this.edge)}, {"Lifecycle", String.valueOf(this.lifecycle)},
                {"Listener", String.valueOf(this.listener)}, {"Other", String.valueOf(this.other)},
                {"Dummy", String.valueOf(this.dummy)}, {"Method", String.valueOf(this.method)},
                {"Interface", String.valueOf(this.control)}};

        return StringTable.tableWithLines(data, true);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{edge=%s, vertex=%s, other=%s, listener=%s, lifecycle=%s, method=%s, dummy=%s, control=%s}",
                getClass().getSimpleName(), this.edge, this.vertex, this.other, this.listener, this.lifecycle,
                this.method, this.dummy, this.control
                            );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Composition)) {
            return false;
        }

        Composition that = (Composition) o;

        if (edge != that.edge) {
            return false;
        }
        if (vertex != that.vertex) {
            return false;
        }
        if (other != that.other) {
            return false;
        }
        if (listener != that.listener) {
            return false;
        }
        if (lifecycle != that.lifecycle) {
            return false;
        }
        if (method != that.method) {
            return false;
        }
        if (dummy != that.dummy) {
            return false;
        }
        return control == that.control;
    }

    @Override
    public int hashCode() {
        int result = edge;
        result = 31 * result + vertex;
        result = 31 * result + other;
        result = 31 * result + listener;
        result = 31 * result + lifecycle;
        result = 31 * result + method;
        result = 31 * result + dummy;
        result = 31 * result + control;
        return result;
    }
}

package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.StringTable;
import phd.research.vertices.Vertex;

import java.util.Set;

/**
 * @author Jordan Doyle
 */

public class Composition {

    private int vertex, edge, control, method, dummy, lifecycle, listener, callback, unit;

    public Composition(Graph<Vertex, DefaultEdge> graph) {
        this.control = this.method = this.dummy = this.lifecycle = this.listener = this.callback = this.unit = 0;
        readComposition(graph);
    }

    public void readComposition(Graph<Vertex, DefaultEdge> graph) {
        Set<Vertex> vertices = graph.vertexSet();
        this.vertex = vertices.size();
        this.edge = graph.edgeSet().size();

        for (Vertex vertex : vertices) {
            switch (vertex.getType()) {
                case CONTROL:
                    this.control++;
                    break;
                case METHOD:
                    this.method++;
                    break;
                case DUMMY:
                    this.dummy++;
                    break;
                case LIFECYCLE:
                    this.lifecycle++;
                    break;
                case LISTENER:
                    this.listener++;
                    break;
                case CALLBACK:
                    this.callback++;
                    break;
                case UNIT:
                    this.unit++;
                    break;
            }
        }
    }

    public String toTableString() {
        String[][] data = new String[][]{{"TYPE", "COUNT"}, {"Vertex", String.valueOf(this.vertex)},
                {"Edge", String.valueOf(this.edge)}, {"Interface", String.valueOf(this.control)},
                {"Method", String.valueOf(this.method)}, {"Dummy", String.valueOf(this.dummy)},
                {"Lifecycle", String.valueOf(this.lifecycle)}, {"Listener", String.valueOf(this.listener)},
                {"Callback", String.valueOf(this.callback)}, {"Unit", String.valueOf(this.unit)}};

        return StringTable.tableWithLines(data, true);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{vertex=%s, edge=%s, control=%s, method=%s, dummy=%s, lifecycle=%s, listener=%s, callback=%s, " +
                        "unit=%s}", getClass().getSimpleName(), this.vertex, this.edge, this.control, this.method,
                this.dummy, this.lifecycle, this.listener, this.callback, this.unit);
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

        if (vertex != that.vertex) {
            return false;
        }
        if (edge != that.edge) {
            return false;
        }
        if (control != that.control) {
            return false;
        }
        if (method != that.method) {
            return false;
        }
        if (dummy != that.dummy) {
            return false;
        }
        if (lifecycle != that.lifecycle) {
            return false;
        }
        if (listener != that.listener) {
            return false;
        }
        if (callback != that.callback) {
            return false;
        }
        return unit == that.unit;
    }

    @Override
    public int hashCode() {
        int result = vertex;
        result = 31 * result + edge;
        result = 31 * result + control;
        result = 31 * result + method;
        result = 31 * result + dummy;
        result = 31 * result + lifecycle;
        result = 31 * result + listener;
        result = 31 * result + callback;
        result = 31 * result + unit;
        return result;
    }
}

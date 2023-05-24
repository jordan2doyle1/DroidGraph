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

    public int getDummy() {
        return this.dummy;
    }

    public int getUnit() {
        return this.unit;
    }

    public int getControl() {
        return this.control;
    }

    public int getVertex() {
        return this.vertex;
    }

    public int getMethod() {
        return this.method;
    }

    public int getLifecycle() {
        return this.lifecycle;
    }

    public int getListener() {
        return this.listener;
    }

    public int getCallback() {
        return this.callback;
    }


    @SuppressWarnings("unused")     // used in DroidCoverage.
    public int getEdge() {
        return this.edge;
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
                case UNIT:
                    this.unit++;
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
                case METHOD:
                    this.method++;
                    break;
            }
        }
    }

    public String toTableString() {
        String[][] data = new String[][]{{"TYPE", "COUNT"}, {"Vertex", String.valueOf(this.vertex)},
                {"Edge", String.valueOf(this.edge)}, {"Controls", String.valueOf(this.control)},
                {"Dummy", String.valueOf(this.dummy)}, {"Lifecycle", String.valueOf(this.lifecycle)},
                {"Listener", String.valueOf(this.listener)}, {"Callback", String.valueOf(this.callback)},
                {"Method", String.valueOf(this.method)}, {"Unit", String.valueOf(this.unit)}};

        return StringTable.tableWithLines(data, true);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{vertex=" + this.vertex + ", edge=" + this.edge + ", control=" +
                this.control + ", method=" + this.method + ", dummy=" + this.dummy + ", lifecycle=" + this.lifecycle +
                ", listener=" + this.listener + ", callback=" + this.callback + ", unit=" + this.unit + "}";
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Composition)) {
            return false;
        }

        Composition that = (Composition) o;

        return this.vertex == that.vertex && this.edge == that.edge && this.control == that.control &&
                this.method == that.method && this.dummy == that.dummy && this.lifecycle == that.lifecycle &&
                this.listener == that.listener && this.callback == that.callback && this.unit == that.unit;
    }

    @Override
    public final int hashCode() {
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

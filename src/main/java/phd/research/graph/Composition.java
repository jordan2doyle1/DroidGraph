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
                case dummyMethod:
                    this.dummy++;
                    break;
                case control:
                    this.control++;
                    break;
            }
        }
    }

    public String toTableString(String tableTitle) {
        String separator = "--------------------------------------------------------------------------------";
        String stringFormat = "\t%-40s\t%-40s\n";
        boolean even = true;

        int numDashes = (80 - (tableTitle.length() + 2));
        if ((numDashes % 2) != 0) {
            numDashes = numDashes - 1;
            even = false;
        }

        StringBuilder dashStringBuilder = new StringBuilder();
        for (int i = 0; i < (numDashes / 2); i++) {
            dashStringBuilder.append("-");
        }

        String before = even ? dashStringBuilder + " " : dashStringBuilder + "- ";
        String after = " " + dashStringBuilder;

        return before + tableTitle + after + String.format("\n" + stringFormat, "Vertex", this.vertex)
                + String.format(stringFormat, "Edge", this.edge)
                + String.format(stringFormat, "Lifecycle", this.lifecycle)
                + String.format(stringFormat, "Listener", this.listener)
                + String.format(stringFormat, "Other", this.other)
                + String.format(stringFormat, "Method", this.method)
                + String.format(stringFormat, "Dummy", this.dummy)
                + String.format(stringFormat, "Interface", this.control) + separator;
    }

    @Override
    public String toString() {
        return "(Vertex:" + this.vertex + ", Edge:" + this.edge + ", Other:" + this.other + ", Listener:"
                + this.listener + ", Lifecycle:" + this.lifecycle + ", Method:" + this.method + ", Dummy:" + this.dummy
                + ", Interface:" + this.control + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;

        if (!(o instanceof Composition)) return false;

        Composition composition = (Composition) o;
        return this.edge == composition.edge && this.vertex == composition.vertex && this.other == composition.other
                && this.listener == composition.listener && this.lifecycle == composition.lifecycle
                && this.method == composition.method && this.dummy == composition.dummy
                && this.control == composition.control;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.edge, this.vertex, this.other, this.listener, this.lifecycle, this.method, this.dummy, this.control
        );
    }
}

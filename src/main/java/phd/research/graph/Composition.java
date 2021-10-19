package phd.research.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.jGraph.JGraph;
import phd.research.jGraph.Vertex;

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

    public Composition(JGraph graph) {
        this.callback = this.listener = this.lifecycle = this.method = this.dummy = this.control = 0;
        readComposition(graph);
    }

    public int getEdgeCount() {
        return this.edge;
    }

    public int getVertexCount() {
        return this.vertex;
    }

    public int getCallbackCount() {
        return this.callback;
    }

    public int getListenerCount() {
        return this.listener;
    }

    public int getLifecycleCount() {
        return lifecycle;
    }

    public int getMethodCount() {
        return method;
    }

    public int getDummyCount() {
        return dummy;
    }

    public int getControlCount() {
        return control;
    }

    @SuppressWarnings("unchecked")
    public void readComposition(Object graph) {
        Set<Vertex> vertices = null;

        if (graph instanceof Graph) {
            Graph<Vertex, DefaultEdge> jGraphT = (Graph<Vertex, DefaultEdge>) graph;
            this.edge = jGraphT.edgeSet().size();
            vertices = jGraphT.vertexSet();
        } else if (graph instanceof JGraph) {
            JGraph jGraph = (JGraph) graph;
            this.edge = jGraph.edgeSet().size();
            vertices = jGraph.vertexSet();
        }

        if (vertices != null) {
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
    }

    @Override
    public String toString() {
        return "(Vertex:" + this.vertex + " Edge:" + this.edge + " System:" + this.callback + " Callback:"
                + this.listener + " Lifecycle:" + this.lifecycle + " Method:" + this.method + " Dummy:" + this.dummy
                + " Interface" + this.control + ")";
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
}

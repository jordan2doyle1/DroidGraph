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

    private int edgeCount, vertexCount, callbackCount, listenerCount, lifecycleCount, methodCount, dummyCount,
            controlCount;

    public Composition(Graph<Vertex, DefaultEdge> graph) {
        this.callbackCount = this.listenerCount = this.lifecycleCount = this.methodCount = this.dummyCount =
                this.controlCount = 0;
        readComposition(graph);
    }

    public Composition(JGraph graph) {
        this.callbackCount = this.listenerCount = this.lifecycleCount = this.methodCount = this.dummyCount =
                this.controlCount = 0;
        readComposition(graph);
    }

    public int getEdgeCount() {
        return this.edgeCount;
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public int getCallbackCount() {
        return this.callbackCount;
    }

    public int getListenerCount() {
        return this.listenerCount;
    }

    public int getLifecycleCount() {
        return lifecycleCount;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public int getDummyCount() {
        return dummyCount;
    }

    public int getControlCount() {
        return controlCount;
    }

    @SuppressWarnings("unchecked")
    public void readComposition(Object graph) {
        Set<Vertex> vertices = null;

        if (graph instanceof Graph) {
            Graph<Vertex, DefaultEdge> jGraphT = (Graph<Vertex, DefaultEdge>) graph;
            this.edgeCount = jGraphT.edgeSet().size();
            vertices = jGraphT.vertexSet();
        } else if (graph instanceof JGraph) {
            JGraph jGraph = (JGraph) graph;
            this.edgeCount = jGraph.edgeSet().size();
            vertices = jGraph.vertexSet();
        }

        if (vertices != null) {
            this.vertexCount = vertices.size();

            for (Vertex vertex : vertices) {
                switch (vertex.getType()) {
                    case method:
                        this.methodCount++;
                        break;
                    case listener:
                        this.listenerCount++;
                        break;
                    case callback:
                        this.callbackCount++;
                        break;
                    case lifecycle:
                        this.lifecycleCount++;
                        break;
                    case dummyMethod:
                        this.dummyCount++;
                        break;
                    case control:
                        this.controlCount++;
                        break;
                }
            }
        }
    }
}

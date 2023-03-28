package phd.research.vertices;

import org.jgrapht.nio.Attribute;
import phd.research.enums.Type;
import phd.research.graph.Classifier;
import phd.research.graph.Control;
import soot.SootMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Jordan Doyle
 */

public class VertexFactory {

    public VertexFactory() {

    }

    private static List<String> convertStringToList(String listenerString) {
        if (!listenerString.equals("[]")) {
            return Arrays.asList(listenerString.substring(1, listenerString.length() - 1).split(","));
        }
        return Collections.emptyList();
    }

    public Vertex createVertex(SootMethod method) {
        String methodSignature = method.getSignature();
        Classifier classifier = new Classifier();

        switch (classifier.getMethodType(method)) {
            case DUMMY:
                return new DummyVertex(methodSignature);
            case LIFECYCLE:
                return new LifecycleVertex(methodSignature);
            case LISTENER:
                return new ListenerVertex(methodSignature);
            case CALLBACK:
                return new CallbackVertex(methodSignature);
            case METHOD:
                return new MethodVertex(methodSignature);
            default:
                throw new RuntimeException("Method " + method + " has unknown type.");
        }
    }

    public Vertex createVertex(int id, Map<String, Attribute> attributes) {
        Type type = Type.valueOf(attributes.get("type").getValue());
        switch (type) {
            case DUMMY:
                return new DummyVertex(id, attributes.get("method").getValue());
            case LIFECYCLE:
                return new LifecycleVertex(id, attributes.get("method").getValue());
            case LISTENER:
                return new ListenerVertex(id, attributes.get("method").getValue());
            case CALLBACK:
                return new CallbackVertex(id, attributes.get("method").getValue());
            case METHOD:
                return new MethodVertex(id, attributes.get("method").getValue());
            case CONTROL:
                return new ControlVertex(id, new Control(Integer.parseInt(attributes.get("controlId").getValue()),
                        attributes.get("control").getValue(), Integer.parseInt(attributes.get("layoutId").getValue()),
                        attributes.get("layout").getValue(), attributes.get("activity").getValue(),
                        VertexFactory.convertStringToList(attributes.get("listeners").getValue())
                ));
            case UNIT:
                return new UnitVertex(id, attributes.get("method").getValue(), attributes.get("unit").getValue());
            default:
                throw new RuntimeException("Unrecognised vertex type: " + type);
        }
    }

    public Vertex createVertex(int id, SootMethod method) {
        String methodSignature = method.getSignature();
        Classifier classifier = new Classifier();

        switch (classifier.getMethodType(method)) {
            case DUMMY:
                return new DummyVertex(id, methodSignature);
            case LIFECYCLE:
                return new LifecycleVertex(id, methodSignature);
            case LISTENER:
                return new ListenerVertex(id, methodSignature);
            case CALLBACK:
                return new CallbackVertex(id, methodSignature);
            case METHOD:
                return new MethodVertex(id, methodSignature);
            default:
                throw new RuntimeException("Method " + method + " has unknown type.");
        }
    }
}

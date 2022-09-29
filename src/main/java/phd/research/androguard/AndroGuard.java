package phd.research.androguard;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.gml.GmlImporter;
import org.jgrapht.util.SupplierUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.DroidGraph;
import phd.research.graph.Filter;
import phd.research.vertices.Vertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroGuard {

    private static final Logger logger = LoggerFactory.getLogger(AndroGuard.class);

    private final File androGuardGraphFile;
    private Graph<Vertex, DefaultEdge> graph;

    public AndroGuard(File file) {
        this.androGuardGraphFile = file;
        this.graph = importGMLGraph(this.androGuardGraphFile);
    }

    public Graph<Vertex, DefaultEdge> getCallGraph() {
        if (this.graph == null) {
            this.graph = importGMLGraph(this.androGuardGraphFile);
        }

        return this.graph;
    }

    public void outputGMLGraph(File outputDirectory) {

    }

    private Graph<Vertex, DefaultEdge> importGMLGraph(File gmlFile) {
        Graph<String, DefaultEdge> tempGraph =
                new DefaultDirectedGraph<>(SupplierUtil.createStringSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER,
                        false
                );

        GmlImporter<String, DefaultEdge> importer = new GmlImporter<>();
        Map<String, Map<String, Attribute>> attrs = new HashMap<>();
        importer.addVertexAttributeConsumer((p, a) -> {
            Map<String, Attribute> map = attrs.computeIfAbsent(p.getFirst(), k -> new HashMap<>());
            map.put(p.getSecond(), a);
        });
        importer.importGraph(tempGraph, gmlFile);

        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        tempGraph.vertexSet().forEach(v -> {
            try {
                graph.addVertex(createVertexForLabel(attrs.get(v).get("label").getValue()));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        tempGraph.edgeSet().forEach(
                e -> {
                    try {
                        graph.addEdge(createVertexForLabel(attrs.get(tempGraph.getEdgeSource(e)).get("label").getValue()),
                                createVertexForLabel(attrs.get(tempGraph.getEdgeTarget(e)).get("label").getValue())
                                          );
                    } catch (FileNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                });

        if (tempGraph.vertexSet().size() != graph.vertexSet().size() ||
                tempGraph.edgeSet().size() != graph.edgeSet().size()) {
            throw new RuntimeException("Graph import failed - missing vertices or edges.");
        }
        return graph;
    }

    private Vertex createVertexForLabel(String label) throws FileNotFoundException {
        Pattern pattern = Pattern.compile("L(.+);->(.+)\\((.*)\\)(.+;|\\[?\\[?.) ?(\\[.+])?");
        Matcher matcher = pattern.matcher(label);

        if (matcher.find()) {
            String className = matcher.group(1).trim().replace("/", ".");
            String methodName = matcher.group(2).trim();
            String parameters = matcher.group(3).trim();
            List<String> paramList = new ArrayList<>(
                    parameters.equals("") ? Collections.emptyList() : Arrays.asList(parameters.split(" ")));
            paramList.replaceAll(this::convertBytecodeTypeToJimple);
            String methodReturn = convertBytecodeTypeToJimple(matcher.group(4).trim());

            String methodSignature = buildMethodSignature(className, methodName, methodReturn, paramList);
            SootMethod method = Scene.v().grabMethod(methodSignature);

            if (method == null) {
                logger.error("Failed to find method: " + methodSignature);
                return null;
            }

            SootClass clazz = Scene.v().getSootClassUnsafe(Scene.signatureToClass(methodSignature));
            if (clazz == null) {
                logger.error("");
                return null;
            }

            if (Filter.isValidClass(clazz)) {
                // TODO: Pass in collected callbacks file somehow?
                DroidGraph.createMethodVertex(new File("collectedCallbacks"), method);
            }
        }

        return null;
    }

    private String buildMethodSignature(String clazz, String method, String returnType, Collection<String> parameters) {
        StringBuilder builder = new StringBuilder("<");
        builder.append(clazz).append(": ").append(returnType).append(" ").append(method).append("(");

        parameters.forEach(parameter-> builder.append(parameter).append(","));
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.append(")>").toString();
    }

    private String convertBytecodeTypeToJimple(String type) {
        if (type.startsWith("[")) {
            return convertBytecodeTypeToJimple(type.substring(1)) + "[]";
        } else if (type.startsWith("L")) {
            return type.substring(1, type.length() - 1).replace("/", ".");
        } else {
            return convertBytecodePrimitiveToJimple(type);
        }
    }

    private String convertBytecodePrimitiveToJimple(String primitiveType) {
        switch (primitiveType) {
            case "V":
                return "void";
            case "Z":
                return "boolean";
            case "B":
                return "byte";
            case "C":
                return "char";
            case "S":
                return "short";
            case "I":
                return "int";
            case "J":
                return "long";
            case "F":
                return "float";
            case "D":
                return "double";
            default:
                throw new RuntimeException("Primitive type provided not recognised: " + primitiveType);
        }
    }
}

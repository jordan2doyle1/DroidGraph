package phd.research.core;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import phd.research.enums.Format;
import phd.research.enums.Type;
import phd.research.graph.Filter;
import phd.research.graph.UnitGraph;
import phd.research.graph.Writer;
import phd.research.helper.Control;
import phd.research.jGraph.Vertex;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.util.Chain;

import java.io.File;
import java.util.*;

/**
 * @author Jordan Doyle
 */
public class DroidGraph {

    private Graph<Vertex, DefaultEdge> callGraph;
    private Graph<Vertex, DefaultEdge> controlFlowGraph;
    private Set<Control> controls;

    public DroidGraph() {

    }

    // TODO: Find somewhere to put the below testing methods for future reference.

    public void getValueFromGetIdMethod() {
        // For Testing Purposes Only.
        SootClass sc = Scene.v().getSootClass("com.example.android.lifecycle.ActivityA$1");
        for (SootMethod method : sc.getMethods() ) {
            if (method.getName().contains("onClick")) {
                if (method.hasActiveBody()) {
                    PatchingChain<Unit> units = method.getActiveBody().getUnits();
                    for (Iterator<Unit> iterator = units.snapshotIterator(); iterator.hasNext(); ) {
                        Unit unit = iterator.next();
                        unit.apply(new AbstractStmtSwitch<Stmt>() {
                            @Override
                            public void caseAssignStmt(AssignStmt stmt) {
                                super.caseAssignStmt(stmt);

                                // Get the left and right operand, if the right operand is a virtual invoke with name.
                                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                                if (invokeExpr.getMethod().getName().equals("getId")) {
                                    System.out.println(unit.getUseBoxes());
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private void printMethodUnitsToConsole(String className, String methodName) {
        // For Testing Purposes Only. E.g. className: com.example.android.lifecycle.ActivityA, methodName: onCreate
        System.out.println("**** Printing method units: " + className + " " + methodName + " ****");
        SootClass sc = Scene.v().getSootClass(className);
        for (SootMethod method : sc.getMethods() ) {
            if (method.getName().contains(methodName)) {
                if (method.hasActiveBody()) {
                    for (Unit unit : method.getActiveBody().getUnits()) {
                        System.out.println(unit.toString());
                    }
                }
            }
        }
        System.out.println("**** END ****");
    }

    // TODO: Find somewhere to put the below method.

    protected static void outputMethods(Format format) throws Exception {
        for (SootClass sootClass : Scene.v().getClasses()) {
            if (Filter.isValidClass(null, null, sootClass)) {
                for (SootMethod method : sootClass.getMethods()) {
                    if (method.hasActiveBody()) {
                        Body body = method.getActiveBody();
                        UnitGraph unitGraph = new UnitGraph(body);

                        String name = sootClass.getName().substring(sootClass.getName().lastIndexOf(".") + 1)
                                + "_" + method.getName();

                        Writer.writeGraph(format, FrameworkMain.getOutputDirectory(), name, unitGraph.getGraph());
                    }
                }
            }
        }
    }


    // TODO: The below methods should stay in this class but the class needs to be renamed.

    public void generateGraphs() {
        this.callGraph = generateGraph(Scene.v().getCallGraph());
        this.controlFlowGraph = generateGraph(this.callGraph);
    }

    public Graph<Vertex, DefaultEdge> getCallGraph() {
        if (this.callGraph == null)
            this.callGraph = generateGraph(Scene.v().getCallGraph());

        return this.callGraph;
    }

    public Graph<Vertex, DefaultEdge> getControlFlowGraph() {
        if (this.controlFlowGraph == null)
            this.controlFlowGraph = generateGraph(this.getCallGraph());

        return this.controlFlowGraph;
    }

    private static String removePackageName(String name) {
        int index = name.lastIndexOf(".");

        if (name.contains("dummyMainMethod"))
            index = name.lastIndexOf("_");

        if(index != -1) {
            name = name.replace(name.substring(0, index + 1), "");
        }

        return name;
    }

    private static String getLabel(SootMethod method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<").append(DroidGraph.removePackageName(method.getDeclaringClass().getName()))
                .append(": ").append(DroidGraph.removePackageName(method.getReturnType().toString()))
                .append(" ").append(DroidGraph.removePackageName(method.getName())).append("(");

        List<soot.Type> parameters = method.getParameterTypes();
        for (int i = 0; i < method.getParameterCount(); i++) {
            stringBuilder.append(DroidGraph.removePackageName(parameters.get(i).toString()));
            if(i != (method.getParameterCount() - 1) )
                stringBuilder.append(",");
        }

        stringBuilder.append(")>");
        return stringBuilder.toString();
    }

    /**
     * For some reason JGraphT does not have a method to retrieve individual vertices that already exist in the graph.
     * Instead, this method searched a list of all the vertices contained within the graph for the required
     * {@link Vertex} object and returns it.
     *
     * @param id  the ID of the {@link Vertex} object being searched for.
     * @param set the set of all vertices to search.
     * @return The {@link Vertex} object from the vertex set with the given ID.
     */
    private static Vertex getVertex(int id, Set<Vertex> set) {
        for (Vertex vertex : set) {
            if (vertex.getID() == id)
                return vertex;
        }

        return null;
    }

    private static Type getMethodType(SootMethod method) {
        if (method.getDeclaringClass().getName().equals("dummyMainClass"))
            return Type.dummyMethod;
        else if (Filter.isListenerMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
            return Type.listener;
        else if (Filter.isLifecycleMethod(method))
            return Type.lifecycle;
        else if (Filter.isOtherCallbackMethod(new File(FrameworkMain.getOutputDirectory() + "CollectedCallbacks"), method))
            return Type.other;
        else
            return Type.method;
    }

    private Control getControl(SootMethod callback) {
        if (this.controls == null)
            this.controls = getUIControls();

        for (Control control : this.controls) {
            if (control.getClickListener() != null)
                if (control.getClickListener().equals(callback))
                    return control;
        }

        return null;
    }

    private Control getControl(String resourceName) {
        if (this.controls == null)
            this.controls = getUIControls();

        for (Control control : this.controls) {
            if (control.getControlResource() != null)
                if (control.getControlResource().getResourceName().equals(resourceName))
                    return control;
        }

        return null;
    }

    private Vertex getInterfaceControl(Vertex vertex) {
        Control control = this.getControl(vertex.getSootMethod());
        if (control != null)
            return new Vertex(control.hashCode(), String.valueOf(control.getControlResource().getResourceID()),
                    Type.control, vertex.getSootMethod());
        else
            System.err.println("No control for " + vertex.getLabel());

        return null;
    }

    private Set<SootMethod> checkGraph(Graph<Vertex, DefaultEdge> graph) {
        // TODO: Verify graph is complete and correct (all methods present?, all vertices have input edges?, etc.)
        // TODO: Print to the console if a problem or anomaly is found.
        Chain<SootClass> classes = Scene.v().getClasses();
        Set<SootMethod> notInGraph = new HashSet<>();

        if (graph == null)
            graph = this.getControlFlowGraph();

        for (SootClass sootClass : classes) {
            if (Filter.isValidClass(null, null, sootClass)) {
                List<SootMethod> methods = sootClass.getMethods();
                for (SootMethod method : methods) {
                    Type methodType = DroidGraph.getMethodType(method);
                    Vertex vertex = new Vertex(method.hashCode(), getLabel(method), methodType, method);
                    if (!graph.containsVertex(vertex))
                        notInGraph.add(method);
                }
            }
        }

        if (notInGraph.isEmpty()) {
            System.out.println("All methods in the graph.");
        } else {
            System.err.println(notInGraph.size() + " methods are not in the graph. ");

            for (SootMethod method : notInGraph) {
                System.out.println(method.toString());
            }
        }

        return notInGraph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(CallGraph sootCallGraph) {
        // TODO: Confirm Call Graph Generation is correct?
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

        Iterator<MethodOrMethodContext> sourceItr = sootCallGraph.sourceMethods();
        while (sourceItr.hasNext()) {
            SootMethod srcMethod = sourceItr.next().method();

            if (Filter.isValidMethod(null, null, srcMethod) ||
                    srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                Type methodType = DroidGraph.getMethodType(srcMethod);
                Vertex srcVertex = new Vertex(srcMethod.hashCode(), getLabel(srcMethod), methodType, srcMethod);
                graph.addVertex(srcVertex);

                Iterator<Edge> edgeItr = sootCallGraph.edgesOutOf(srcMethod);
                while (edgeItr.hasNext()) {
                    SootMethod tgtMethod = edgeItr.next().tgt();

                    if (Filter.isValidMethod(null, null, tgtMethod) ||
                            srcMethod.getDeclaringClass().getName().equals("dummyMainClass")) {
                        methodType = DroidGraph.getMethodType(tgtMethod);
                        Vertex tgtVertex = new Vertex(tgtMethod.hashCode(), getLabel(tgtMethod), methodType,
                                tgtMethod);
                        graph.addVertex(tgtVertex);

                        graph.addEdge(srcVertex, tgtVertex);
                    }
                }
            }
        }

        // checkGraph(graph);
        return graph;
    }

    private Graph<Vertex, DefaultEdge> generateGraph(Graph<Vertex, DefaultEdge> callGraph) {
        // TODO: Confirm Control Flow Graph Generation is correct?
        Graph<Vertex, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        Graphs.addGraph(graph, callGraph);
        JimpleBasedInterproceduralCFG jimpleCFG = new JimpleBasedInterproceduralCFG();

        Set<Vertex> vertexSet = new HashSet<>(graph.vertexSet());
        for (Vertex vertex : vertexSet) {
            if (vertex.getType() == Type.listener) {
                Vertex interfaceVertex = getInterfaceControl(vertex);
                if (interfaceVertex != null) {
                    graph.addVertex(interfaceVertex);
                    graph.addEdge(interfaceVertex, vertex);
                } else
                    System.err.println("Failed to find interface control for vertex: \"" + vertex.getLabel() + "\". ");
            }

            if (vertex.getType() == Type.listener || vertex.getType() == Type.lifecycle ||
                    vertex.getType() == Type.method) {
                if (vertex.getSootMethod().hasActiveBody()) {
                    UnitGraph unitGraph = new UnitGraph(vertex.getSootMethod().getActiveBody());
                    Graph<Vertex, DefaultEdge> methodSubGraph = unitGraph.getGraph();
                    Graphs.addGraph(graph, methodSubGraph);

                    Set<Vertex> roots = unitGraph.getRoots();
                    for (Vertex root : roots) {
                        graph.addEdge(vertex, root);
                    }

                    //TODO: Don't forget to link the return statement back to the calling statement.
                    Set<Unit> callStatements = jimpleCFG.getCallsFromWithin(vertex.getSootMethod());
                    for (Unit callStatement : callStatements) {
                        Collection<SootMethod> calledMethods = jimpleCFG.getCalleesOfCallAt(callStatement);
                        for (SootMethod calledMethod : calledMethods) {
                            Vertex callVertex = getVertex(callStatement.hashCode(), graph.vertexSet());
                            Vertex calledVertex = getVertex(calledMethod.hashCode(), graph.vertexSet());
                            if (callVertex != null && calledVertex != null)
                                graph.addEdge(callVertex, calledVertex);
                        }
                    }
                }
            } else if (vertex.getType() != Type.statement && vertex.getType() != Type.control
                    && vertex.getType() != Type.dummyMethod)
                System.err.println("Found unknown vertex type \"" + vertex.getType() + "\": " + vertex.getLabel());
        }

        // checkGraph(graph);
        return graph;
    }
}
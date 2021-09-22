package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.graph.UnitGraph;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class MethodManager {

    private static final Logger logger = LoggerFactory.getLogger(MethodManager.class);
    private static final MethodManager instance = new MethodManager();

    private Set<SootMethod> all;
    private Set<SootMethod> filtered;

    private MethodManager() { }

    public static MethodManager getInstance() {
        return instance;
    }

    public void start() {
        this.all = retrieveAllMethods();
        this.filtered = filterMethods();
    }

    public int methodCount() {
        return this.all.size();
    }

    public boolean isFiltered(SootMethod method) {
        return this.filtered.contains(method);
    }

    public int filteredCount() {
        return this.filtered.size();
    }

    public void outputFilteredMethods(String format) {
        int count = 0;
        for (SootMethod method : this.filtered) {
            outputMethod(method, format);
            count++;
        }
        logger.info(count + " methods output in " + format + " format");
    }

    public void outputMethod(SootMethod method, String format) {
        if (method.hasActiveBody()) {
            Body body = method.getActiveBody();
            UnitGraph unitGraph = new UnitGraph(body);
            unitGraph.outputGraph(format);
            logger.info("Unit graph for method " + method.getName() + " output in " + format + " format");
        }
    }

    @SuppressWarnings("CommentedOutCode")
    private Set<SootMethod> retrieveAllMethods() {
        logger.info("Retrieving all methods...");
        Set<SootClass> classes = ClassManager.getInstance().getAllClasses();
        Set<SootMethod> allMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            List<SootMethod> methods = sootClass.getMethods();
            allMethods.addAll(methods);

//            for (SootMethod method : methods) {
//                logger.debug("Added method \"" + method.getName() + "\".");
//            }
        }

        logger.info("Found " + allMethods.size() + " methods.");
        return allMethods;
    }

    private Set<SootMethod> filterMethods() {
        logger.info("Filtering methods...");
        Set<SootClass> classes = ClassManager.getInstance().getFiltered();
        Set<SootMethod> acceptedMethods = new HashSet<>();

        for (SootClass sootClass : classes) {
            List<SootMethod> methods = sootClass.getMethods();
            acceptedMethods.addAll(methods);

            for (SootMethod method : methods) {
                logger.debug("\"" + method.getName() + "\" passed filter.");
            }
        }

        logger.info("Found " + acceptedMethods.size() + " filtered methods.");
        return acceptedMethods;
    }
}
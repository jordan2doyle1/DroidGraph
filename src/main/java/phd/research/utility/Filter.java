package phd.research.utility;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.singletons.GraphSettings;
import phd.research.vertices.AndroGuardVertex;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.util.SystemClassHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Jordan Doyle
 */

public class Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Filter.class);

    private static final List<String> PACKAGE_BLACKLIST = Filter.loadBlacklist("package_blacklist");
    private static final List<String> CLASS_BLACKLIST = Filter.loadBlacklist("class_blacklist");
    private static final List<String> LAYOUT_BLACKLIST = Filter.loadBlacklist("layout_blacklist");

    private static final Graph<AndroGuardVertex, DefaultEdge> ANDRO_GUARD_GRAPH =
            Importer.importAndroGuardGraph(GraphSettings.v().getCallGraphFile());
    private static final Map<String, Boolean> METHOD_EXTERNAL_STATUS = Filter.getExternalStatusMap();

    public static boolean isValidMethod(SootMethod method) {
        if (Filter.isValidClass(method.getDeclaringClass())) {
            if (method.getName().startsWith("access$")) {
                return false;
            }

            String methodSignature = method.getSignature().replace("'", "");
            if (Filter.METHOD_EXTERNAL_STATUS.containsKey(methodSignature)) {
                return !Filter.METHOD_EXTERNAL_STATUS.get(methodSignature);
            }
        }
        return false;
    }

    public static boolean isValidClass(SootClass clazz) {
        if (SystemClassHandler.v().isClassInSystemPackage(clazz) || clazz.isJavaLibraryClass() ||
                clazz.isLibraryClass() || clazz.isPhantomClass() || Scene.v().isExcluded(clazz) ||
                !Filter.isValidPackage(clazz.getPackageName())) {
            return false;
        }

        if (clazz.getShortName().equals("R")) {
            return false;
        }

        return Filter.CLASS_BLACKLIST.stream()
                .noneMatch(blacklistedClass -> clazz.getShortName().contains(blacklistedClass));
    }

    public static boolean isValidLayout(String layout) {
        return !Filter.LAYOUT_BLACKLIST.contains(layout);
    }

    public static Graph<AndroGuardVertex, DefaultEdge> getAndroGuardCallGraph() {
        return Filter.ANDRO_GUARD_GRAPH;
    }

    private static boolean isValidPackage(String packageName) {
        return Filter.PACKAGE_BLACKLIST.stream().noneMatch(
                blacklistedPackage -> blacklistedPackage.startsWith(".") ? packageName.contains(blacklistedPackage) :
                        packageName.startsWith(blacklistedPackage));
    }

    private static List<String> loadBlacklist(String fileName) {
        LOGGER.info("Loading blacklist from resource file '" + fileName + "'");
        InputStream resourceStream = Filter.class.getClassLoader().getResourceAsStream(fileName);
        return resourceStream != null ?
                new BufferedReader(new InputStreamReader(resourceStream)).lines().collect(Collectors.toList()) :
                new ArrayList<>();
    }

    private static Map<String, Boolean> getExternalStatusMap() {
        LOGGER.info("Loading AndroGuard external status map.");
        Map<String, Boolean> statusMap = new HashMap<>();
        Filter.ANDRO_GUARD_GRAPH.vertexSet()
                .forEach(vertex -> statusMap.put(vertex.getJimpleSignature(), vertex.isExternal()));
        return statusMap;
    }
}

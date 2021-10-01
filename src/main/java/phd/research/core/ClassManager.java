package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class ClassManager {

    private static final Logger logger = LoggerFactory.getLogger(ClassManager.class);
    private static final ClassManager instance = new ClassManager();

    private Set<String> blacklist;
    private Set<SootClass> all;
    private Set<SootClass> filtered;
    private Set<SootClass> entryPoints;
    private Set<SootClass> launchActivities;

    private ClassManager() { }

    public static ClassManager getInstance() {
        return instance;
    }

    public void start() {
        this.blacklist = readBlacklist();
        this.all = retrieveAllClasses();
        this.filtered = filterClasses();
        this.entryPoints = retrieveEntryPoints();
        this.launchActivities = retrieveLaunchActivities();
    }

    public Set<SootClass> getAllClasses() {
        return this.all;
    }

    public int classCount() {
        return this.all.size();
    }

    public Set<SootClass> getFiltered() {
        return this.filtered;
    }

    public int filteredCount() {
        return this.filtered.size();
    }

    public Set<SootClass> getEntryPoints() {
        return this.entryPoints;
    }

    public SootClass getEntryPoint(String className) {
        return getClass(this.filtered, className);
    }

    public int entryPointCount() {
        return this.entryPoints.size();
    }

    public Set<SootClass> getLaunchActivities() {
        return this.launchActivities;
    }

    public int launchActivityCount() {
        return this.launchActivities.size();
    }

    private Set<String> readBlacklist() {
        logger.info("Reading class blacklist...");
        Set<String> classes = new HashSet<>();
        String file = System.getProperty("user.dir") + "/config/class_blacklist";

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String currentClass;

            while ((currentClass = bufferedReader.readLine()) != null) {
                classes.add(currentClass);
                logger.debug("\"" + currentClass + "\" found in blacklist.");
            }
        } catch (IOException e) {
            logger.error("Failure reading class blacklist: " + e.getMessage());
            return classes;
        }

        logger.info("Finished reading class blacklist.");
        return classes;
    }

    private boolean blacklistContains(String className) {
        for (String blacklistClass : this.blacklist) {
            if (className.contains(blacklistClass))
                return true;
        }
        return false;
    }

    @SuppressWarnings("CommentedOutCode")
    private Set<SootClass> retrieveAllClasses() {
        logger.info("Retrieving all classes...");
        Set<SootClass> classes = new HashSet<>(Scene.v().getClasses());

//        for (SootClass sootClass : classes) {
//            logger.debug("Added class \"" + sootClass.getShortName() + "\".");
//        }

        logger.info("Found " + classes.size() + " classes.");
        return classes;
    }

    private Set<SootClass> filterClasses() {
        logger.info("Filtering classes...");
        Set<SootClass> classes = new HashSet<>();

        for (SootClass sootClass : this.all) {
            if (PackageManager.getInstance().isFiltered(sootClass.getPackageName())) {
                if (!blacklistContains(sootClass.getShortName())) {
                    classes.add(sootClass);
                    logger.debug("\"" + sootClass.getShortName() + "\" passed filter.");
                }
            }
        }

        logger.info("Filtered " + classes.size() + " classes.");
        return classes;
    }

    private SootClass getClass(Set<SootClass> set, String search) {
        for (SootClass sootClass : set) {
            if (search.equals(sootClass.getName())) {
                return sootClass;
            }
        }
        return null;
    }

    private Set<SootClass> retrieveEntryPoints() {
        logger.info("Retrieving entry points...");
        Set<SootClass> entryPoints = new HashSet<>();

        ProcessManifest manifest;
        try {
            manifest = new ProcessManifest(FrameworkMain.getAPK());
            for (String entryPoint : manifest.getEntryPointClasses()) {
                SootClass entryPointClass = getClass(this.filtered, entryPoint);
                if (entryPointClass != null) {
                    entryPoints.add(entryPointClass);
                    logger.debug("Found entry point \"" + entryPointClass.getShortName() + "\".");
                } else
                    logger.error("Failed to find entry point " + entryPoint + ".");
            }
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return entryPoints;
        }

        logger.info("Found " + entryPoints.size() + " entry points.");
        return entryPoints;
    }

    private Set<SootClass> retrieveLaunchActivities() {
        logger.info("Retrieving launch activities...");
        Set<SootClass> launchActivities = new HashSet<>();

        ProcessManifest manifest;
        try {
            manifest = new ProcessManifest(FrameworkMain.getAPK());
            for (AXmlNode activity : manifest.getLaunchableActivityNodes()) {
                if (activity.hasAttribute("name")) {
                    // Could be excluding valid activities if the app developer has not provided the name attribute.
                    String activityName = activity.getAttribute("name").getValue().toString();
                    SootClass launchActivity = getClass(this.filtered, activityName);
                    if (launchActivity != null) {
                        launchActivities.add(launchActivity);
                        logger.debug("Found launch activity \"" + launchActivity.getShortName() + "\".");
                    } else {
                        logger.error("Failed to find launch activity " + activityName + ".");
                    }
                }
            }
        } catch (IOException | XmlPullParserException e) {
            logger.error("Failure processing manifest: " + e.getMessage());
            return launchActivities;
        }

        logger.info("Found " + launchActivities.size() + " launch activities.");
        return launchActivities;
    }
}
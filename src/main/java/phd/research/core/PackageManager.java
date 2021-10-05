package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.util.Chain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jordan Doyle
 */
public class PackageManager {

    private static final Logger logger = LoggerFactory.getLogger(PackageManager.class);
    private static final PackageManager instance = new PackageManager();

    private String basename;
    private Set<String> blacklist;
    private Set<String> all;
    private Set<String> filtered;

    private PackageManager() { }

    public static PackageManager getInstance() {
        return instance;
    }

    public void start() {
        this.basename = retrieveBasename();
        this.blacklist = readBlacklist();
        this.all = retrieveAllPackages();
        this.filtered = filterPackages();
    }

    public String getBasename() {
        return this.basename;
    }

    public Set<String> getAllPackages() {
        return this.all;
    }

    public boolean isFiltered(String packageName) {
        return this.filtered.contains(packageName);
    }

    public int packageCount() {
        return this.all.size();
    }

    public int filteredCount() {
        return this.filtered.size();
    }

    private String retrieveBasename() {
        logger.info("Retrieving basename from manifest...");

        ProcessManifest manifest;
        try {
            manifest = new ProcessManifest(FrameworkMain.getApk());
        } catch (IOException | XmlPullParserException e) {
            logger.error("Error processing manifest: " + e.getMessage());
            return null;
        }

        String basename = manifest.getPackageName();
        logger.info("Package basename is \"" + basename + "\"");
        return basename;
    }

    private Set<String> readBlacklist() {
        logger.info("Reading package blacklist...");
        Set<String> packages = new HashSet<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(FrameworkMain.getPackageBlacklist()));
            String currentPackage;

            while ((currentPackage = bufferedReader.readLine()) != null) {
                packages.add(currentPackage);
                logger.debug("\"" + currentPackage + "\" found in blacklist.");
            }
        } catch (IOException e) {
            logger.error("Error reading package blacklist: " + e.getMessage());
            return packages;
        }

        logger.info("Finished reading package blacklist.");
        return packages;
    }

    private boolean blacklistContains(String packageName) {
        for (String blacklistPackage : this.blacklist) {
            if (blacklistPackage.startsWith(".")) {
                if (packageName.contains(blacklistPackage))
                    return true;
            } else {
                if (packageName.startsWith(blacklistPackage))
                    return true;
            }
        }
        return false;
    }

    private Set<String> retrieveAllPackages() {
        logger.info("Retrieving all packages...");
        Set<String> packages = new HashSet<>();
        Chain<SootClass> classes = Scene.v().getClasses();

        for (SootClass sootClass : classes) {
            String packageName = sootClass.getPackageName();
            if (!packageName.equals("")) {
                packages.add(packageName);
                logger.debug("Adding package \"" + packageName + "\".");
            }
        }

        logger.info("Found " + packages.size() + " packages.");
        return packages;
    }

    private Set<String> filterPackages() {
        logger.info("Filtering packages...");
        Set<String> packages = new HashSet<>();

        if (!this.all.isEmpty()) {
            for (String currentPackage : this.all) {
                if (!blacklistContains(currentPackage)) {
                    packages.add(currentPackage);
                    logger.debug("\"" + currentPackage + "\" passed filter.");
                }
            }
        }

        logger.info("Found " + packages.size() + " filtered packages.");
        return packages;
    }
}

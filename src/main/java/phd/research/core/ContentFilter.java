package phd.research.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.SootClass;
import soot.SootMethod;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ContentFilter {

    //TODO: Create Class to confirm analysis content. this will be all the creating lists code.
    private static final Logger logger = LoggerFactory.getLogger(ContentFilter.class);

    private final Set<String> packageBlacklist;
    private final Set<String> classBlacklist;

    public ContentFilter() {
        this.packageBlacklist = readBlacklist(FrameworkMain.getPackageBlacklist());
        this.classBlacklist = readBlacklist(FrameworkMain.getClassBlacklist());
    }

    public boolean isValidPackage(String packageName) {
        for (String blacklistPackage : this.packageBlacklist) {
            if (blacklistPackage.startsWith(".")) {
                if (packageName.contains(blacklistPackage))
                    return false;
            } else {
                if (packageName.startsWith(blacklistPackage))
                    return false;
            }
        }
        return true;
    }

    public boolean isValidClass(SootClass sootClass) {
        if (!isValidPackage(sootClass.getPackageName())) {
            for (String blacklistClass : this.classBlacklist) {
                if (sootClass.getShortName().contains(blacklistClass))
                    return false;
            }
            return false;
        }
        return true;
    }

    public boolean isValidMethod(SootMethod method) {
        return isValidClass(method.getDeclaringClass());
    }

    private Set<String> readBlacklist(String file) {
        Set<String> items = new HashSet<>();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            String currentItem;

            while ((currentItem = bufferedReader.readLine()) != null) {
                items.add(currentItem);
            }
        } catch (IOException e) {
            logger.error("Error reading blacklist: " + e.getMessage());
            return items;
        }

        return items;
    }
}

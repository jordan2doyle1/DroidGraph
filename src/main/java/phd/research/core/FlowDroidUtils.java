package phd.research.core;

import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class FlowDroidUtils {

    public FlowDroidUtils() {

    }

    public static ProcessManifest getManifest(String apk) {
        ProcessManifest manifest;

        try {
            manifest = new ProcessManifest(apk);
        } catch (IOException | XmlPullParserException e) {
            System.err.println("Failure processing manifest: " + e.getMessage());
            return null;
        }

        return manifest;
    }

    public static Set<SootClass> getEntryPointClasses(String apk) {
        Set<SootClass> entryPoints = new HashSet<>();
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            for (String entryPoint : manifest.getEntryPointClasses()) {
                SootClass entryPointClass = Scene.v().getSootClass(entryPoint);
                if (entryPointClass != null) entryPoints.add(entryPointClass);
            }
        }

        return entryPoints;
    }

    public static ARSCFileParser getResources(String apk) {
        ARSCFileParser resources = new ARSCFileParser();

        try {
            resources.parse(apk);
        } catch (IOException e) {
            System.err.println("Error getting resources: " + e.getMessage());
        }

        return resources;
    }

    public static LayoutFileParser getLayoutFileParser(String apk) {
        LayoutFileParser layoutParser;
        ProcessManifest manifest = getManifest(apk);

        if (manifest != null) {
            layoutParser = new LayoutFileParser(manifest.getPackageName(), getResources(apk));
            layoutParser.parseLayoutFileDirect(FrameworkMain.getApk());
            return layoutParser;
        }

        return null;
    }

    public static CollectedCallbacks readCollectedCallbacks(File callbackFile) {
        CollectedCallbacks callbacks = null;

        try {
            callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        } catch (FileNotFoundException e) {
            System.err.println("Error: Collected Callbacks File Not Found:" + e.getMessage());
        }

        return callbacks;
    }
}

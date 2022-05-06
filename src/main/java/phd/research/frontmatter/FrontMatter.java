package phd.research.frontmatter;

import org.json.JSONArray;
import org.json.JSONObject;
import soot.Scene;
import soot.SootMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FrontMatter {

    //TODO: Add command line arguments to tell the program where FrontMatter is stored and where to store output files.
    //TODO: Set ANDROID_HOME system variable.

    private JSONObject output;

    public FrontMatter() throws IOException {
        this.output = readJSONOutput();
    }

    public boolean containsControl(String className, int id) throws IOException {
        if (this.output == null) {
            this.output = readJSONOutput();
        }

        JSONArray activities = this.output.getJSONArray("activities");
        for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            if (activity.getString("name").equals(className)) {
                JSONArray layouts = activity.getJSONArray("layouts");
                for (int j = 0; j < layouts.length(); j++) {
                    JSONObject view = layouts.getJSONObject(j);
                    JSONObject viewWithID = searchForID(view, id);
                    if (viewWithID != null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private JSONObject searchForID(JSONObject view, int id) {
        if (view.getInt("id") == id) {
            return view;
        } else {
            if (view.has("children")) {
                JSONArray children = view.getJSONArray("children");
                for (int i = 0; i < children.length(); i++) {
                    JSONObject child = children.getJSONObject(i);
                    JSONObject viewWithID = searchForID(child, id);
                    if(viewWithID != null) {
                        return viewWithID;
                    }
                }
            }
        }

        return null;
    }

    public SootMethod getClickListener(int id) throws IOException {
        if (this.output == null) {
            this.output = readJSONOutput();
        }

        JSONArray listenerMethods = null;
        JSONArray activities = this.output.getJSONArray("activities");
        for (int i = 0; i < activities.length(); i++) {
            JSONObject activity = activities.getJSONObject(i);
            JSONArray layouts = activity.getJSONArray("layouts");
            for (int j = 0; j < layouts.length(); j++) {
                JSONObject view = layouts.getJSONObject(j);
                JSONObject viewWithID = searchForID(view, id);
                if (viewWithID != null) {
                    if (viewWithID.has("listeners")) {
                        listenerMethods = viewWithID.getJSONArray("listeners");
                    }
                }
            }
        }

        if (listenerMethods != null)
            return Scene.v().getMethod(listenerMethods.getString(0));

        return null;
    }

    private JSONObject readJSONOutput() throws IOException {
        File outputFile = new File(System.getProperty("user.dir") + "/FrontMatterOutput.json");
        if (!outputFile.exists())
            throw new IOException("Error: Front Matter output file does not exist!");

        String outputContent = new String(Files.readAllBytes(Paths.get(outputFile.toURI())));
        return new JSONObject(outputContent);
    }
}

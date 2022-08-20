package phd.research.helper;

import soot.jimple.infoflow.android.resources.ARSCFileParser;

/**
 * @author Jordan Doyle
 */

public class CustomResource extends ARSCFileParser.AbstractResource {
    String overrideResourceName;
    int overrideResourceID;

    public CustomResource(String resourceName, int resourceId) {
        this.overrideResourceName = resourceName;
        this.overrideResourceID = resourceId;
    }

    public CustomResource(String resourceName) {
        this.overrideResourceName = resourceName;
    }

    @Override
    public String getResourceName() {
        return this.overrideResourceName;
    }

    @Override
    public int getResourceID() {
        return this.overrideResourceID;
    }
}

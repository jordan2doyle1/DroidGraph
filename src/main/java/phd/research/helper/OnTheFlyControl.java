package phd.research.helper;

import soot.jimple.infoflow.android.resources.ARSCFileParser;

/**
 * @author Jordan Doyle
 */

public class OnTheFlyControl extends ARSCFileParser.AbstractResource{
    String overrideResourceName;
    int overrideResourceID;

    public OnTheFlyControl(String resourceName, int resourceId) {
        this.overrideResourceName = resourceName;
        this.overrideResourceID = resourceId;
    }

    public OnTheFlyControl(String resourceName) {
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

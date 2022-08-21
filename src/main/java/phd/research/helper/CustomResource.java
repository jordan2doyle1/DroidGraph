package phd.research.helper;

import org.jetbrains.annotations.NotNull;
import soot.jimple.infoflow.android.resources.ARSCFileParser;

import java.util.Objects;

/**
 * @author Jordan Doyle
 */

public class CustomResource extends ARSCFileParser.AbstractResource {

    @NotNull
    String overrideResourceName;
    int overrideResourceID;

    @API
    public CustomResource(String resourceName, int resourceId) {
        this.overrideResourceName = Objects.requireNonNull(resourceName);
        this.overrideResourceID = resourceId;
    }

    @API
    public CustomResource(String resourceName) {
        this.overrideResourceName = Objects.requireNonNull(resourceName);
        this.overrideResourceID = -1;
    }

    @API
    @Override
    public String getResourceName() {
        return this.overrideResourceName;
    }

    @API
    @Override
    public int getResourceID() {
        return this.overrideResourceID;
    }
}

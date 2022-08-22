package phd.research.helper;

import soot.jimple.infoflow.android.resources.controls.LayoutControlFactory;

/**
 * @author Jordan Doyle
 */

public class DroidControlFactory extends LayoutControlFactory {

    @Override
    protected boolean isAndroidNamespace(String namespace) {
        if (namespace == null) {
            return false;
        }

        namespace = namespace.trim();

        if (namespace.startsWith("*")) {
            namespace = namespace.substring(1);
        }

        return namespace.equals("http://schemas.android.com/apk/res/android") ||
                namespace.equals("http://schemas.android.com/apk/res-auto");
    }
}

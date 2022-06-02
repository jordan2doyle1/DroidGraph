package phd.research.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.core.FlowDroidUtils;
import phd.research.core.FrameworkMain;
import phd.research.frontmatter.FrontMatter;
import phd.research.helper.Control;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.android.resources.LayoutFileParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.util.MultiMap;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class UiFrontMatterSearch extends UiSearch {

    private static final Logger logger = LoggerFactory.getLogger(UiFrontMatterSearch.class);

    public UiFrontMatterSearch(String apk) {
        super(apk);
    }

    public Set<Control> getControlListenerMethods() {
        LayoutFileParser layoutParser = FlowDroidUtils.getLayoutFileParser(super.apk);

        Set<Control> uiControls = new HashSet<>();
        MultiMap<String, AndroidLayoutControl> userControls;
        if (layoutParser != null) {
            userControls = layoutParser.getUserControls();
        } else {
            logger.error("Problem getting Layout File Parser. Can't get UI Controls!");
            return uiControls;
        }

        FrontMatter frontMatter;
        try {
            frontMatter = new FrontMatter(new File(FrameworkMain.getFrontMatterOutputFile()));
        } catch (IOException e) {
            logger.error("Problem reading Front Matter output file!" + e);
            return uiControls;
        }

        for (String layoutFile : userControls.keySet()) {
            ARSCFileParser.AbstractResource layoutResource =
                    resources.findResourceByName("layout", getResourceName(layoutFile));

            for (AndroidLayoutControl control : userControls.get(layoutFile)) {
                if (control.getID() == -1) {
                    continue;
                }

                ARSCFileParser.AbstractResource controlResource = super.getResourceById(control.getID());
                if (controlResource == null) {
                    logger.error("No resource found with ID " + control.getID() + ".");
                    continue;
                }

                SootClass callbackClass = super.findLayoutClass(layoutResource.getResourceID());
                if (callbackClass == null) {
                    logger.error("No class found for layout resource: " + layoutResource.getResourceID());
                    continue;
                }

                SootMethod clickListener = null;
                try {
                    if (frontMatter.containsControl(callbackClass.getName(), control.getID())) {
                        clickListener = frontMatter.getClickListener(control.getID());
                    } else {
                        logger.warn("FrontMatter output did not contain control ID " + control.getID());
                    }
                } catch (IOException e) {
                    logger.error("Problem searching FrontMatter output." + e.getMessage());
                    return uiControls;
                }


                if (clickListener == null) {
                    logger.error("Couldn't find click listener method with ID: " + control.getID());
                }

                uiControls.add(new Control(control.hashCode(), controlResource, layoutResource, clickListener));
            }
        }

        return uiControls;
    }
}

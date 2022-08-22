package phd.research.helper;

import soot.Scene;
import soot.SootClass;
import soot.jimple.infoflow.android.axml.AXmlHandler;
import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.axml.parsers.AXML20Parser;
import soot.jimple.infoflow.android.resources.AbstractResourceParser;
import soot.jimple.infoflow.android.resources.controls.AndroidLayoutControl;
import soot.jimple.infoflow.android.resources.controls.LayoutControlFactory;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

/**
 * Parser for analyzing the menu XML files inside an android application. Code copied and adapted from LayoutFileParser
 * in FlowDroid written by Steven Arzt.
 *
 * @author Jordan Doyle
 */

public class MenuFileParser extends AbstractResourceParser {

    private final MultiMap<String, AndroidLayoutControl> userControls = new HashMultiMap<>();

    private LayoutControlFactory controlFactory = new DroidControlFactory();

    public MenuFileParser() {
    }

    /**
     * Gets the user controls found in the layout XML file. The result is a mapping from the file name in which the
     * control was found to the respective layout control.
     *
     * @return The layout controls found in the XML file.
     */
    public MultiMap<String, AndroidLayoutControl> getUserControls() {
        return this.userControls;
    }

    /**
     * Sets the layout control factory to use for creating new layout controls
     *
     * @param controlFactory The layout control factory
     */
    public void setControlFactory(LayoutControlFactory controlFactory) {
        this.controlFactory = controlFactory;
        this.controlFactory.setLoadAdditionalAttributes(true);
    }

    /**
     * Parses all layout XML files in the given APK file and loads the IDs of the user controls in it. This method
     * directly executes the analyses without registering any Soot phases.
     *
     * @param apkFileName The APK file in which to look for user controls
     */
    public void parseLayoutFileDirect(String packageName, final String apkFileName) {
        handleAndroidResourceFiles(apkFileName, null, (fileName, fileNameFilter, stream) -> {
            if (!fileName.startsWith("res/menu") || !fileName.endsWith(".xml")) {
                return;
            }

            String entryClass = fileName.substring(0, fileName.lastIndexOf("."));
            if (!packageName.isEmpty()) {
                entryClass = packageName + "." + entryClass;
            }

            if (fileNameFilter != null) {
                boolean found = false;
                for (String s : fileNameFilter) {
                    if (s.equalsIgnoreCase(entryClass)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }

            try {
                AXmlHandler handler = new AXmlHandler(stream, new AXML20Parser());
                parseLayoutNode(fileName, handler.getDocument().getRootNode());
            } catch (Exception ex) {
                logger.error("Could not read binary XML fileName: " + ex.getMessage(), ex);
            }
        });
    }

    /**
     * Parses the layout file with the given root node
     *
     * @param layoutFile The full path and file name of the file being parsed
     * @param rootNode   The root node from where to start parsing
     */
    private void parseLayoutNode(String layoutFile, AXmlNode rootNode) {
        if (rootNode.getTag() == null || rootNode.getTag().isEmpty()) {
            logger.warn(String.format("Found a null or empty node name in file %s, skipping node...", layoutFile));
            return;
        }

        if (rootNode.getTag().trim().equals("item")) {
            parseLayoutAttributes(layoutFile, Scene.v().getSootClass("android.view.View"), rootNode);
        }

        for (AXmlNode childNode : rootNode.getChildren()) {
            parseLayoutNode(layoutFile, childNode);
        }
    }

    /**
     * Parses the layout attributes in the given AXml node
     *
     * @param layoutFile  The full path and file name of the file being parsed
     * @param layoutClass The class for the attributes are parsed
     * @param rootNode    The AXml node containing the attributes
     */
    private void parseLayoutAttributes(String layoutFile, SootClass layoutClass, AXmlNode rootNode) {
        AndroidLayoutControl layoutControl = controlFactory.createLayoutControl(layoutFile, layoutClass, rootNode);
        this.userControls.put(layoutFile, layoutControl);
    }
}
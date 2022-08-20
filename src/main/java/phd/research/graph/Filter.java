package phd.research.graph;

import org.xmlpull.v1.XmlPullParserException;
import phd.research.core.FlowDroidUtils;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.infoflow.android.callbacks.AndroidCallbackDefinition;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacks;
import soot.jimple.infoflow.android.callbacks.xml.CollectedCallbacksSerializer;
import soot.jimple.infoflow.android.entryPointCreators.AndroidEntryPointUtils;
import soot.jimple.infoflow.util.SystemClassHandler;
import soot.util.MultiMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Jordan Doyle
 */

public class Filter {

    public static boolean isEntryPointClass(File apk, SootClass clazz) throws XmlPullParserException, IOException {
        return FlowDroidUtils.getEntryPointClasses(apk).stream().anyMatch(e -> e.equals(clazz));
    }

    public static boolean isLifecycleMethod(SootMethod method) {
        AndroidEntryPointUtils entryPointUtils = new AndroidEntryPointUtils();
        return entryPointUtils.isEntryPointMethod(method);
    }

    public static boolean isListenerMethod(File callbackFile, SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        return Filter.isListenerMethod(callbacks.getCallbackMethods(), method);
    }

    public static boolean isListenerMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks,
            SootMethod method) {
        SootClass parentClass = getParentClass(method);
        for (AndroidCallbackDefinition callbackDefinition : callbacks.get(parentClass)) {
            if (callbackDefinition.getTargetMethod().equals(method) &&
                    callbackDefinition.getCallbackType() == AndroidCallbackDefinition.CallbackType.Widget) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOtherCallbackMethod(File callbackFile, SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        return Filter.isOtherCallbackMethod(callbacks.getCallbackMethods(), method);
    }

    public static boolean isOtherCallbackMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks,
            SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(callbacks, method)) {
            SootClass methodClass = Filter.getParentClass(method);
            for (AndroidCallbackDefinition callbackDefinition : callbacks.get(methodClass)) {
                if (callbackDefinition.getTargetMethod().equals(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPossibleListenerMethod(File callbackFile, SootMethod method) throws FileNotFoundException {
        CollectedCallbacks callbacks = CollectedCallbacksSerializer.deserialize(callbackFile);
        return Filter.isPossibleListenerMethod(callbacks.getCallbackMethods(), method);
    }

    public static boolean isPossibleListenerMethod(MultiMap<SootClass, AndroidCallbackDefinition> callbacks,
            SootMethod method) {
        if (!Filter.isLifecycleMethod(method) && !Filter.isListenerMethod(callbacks, method) &&
                !Filter.isOtherCallbackMethod(callbacks, method)) {
            if ((!method.toString().startsWith("<androidx"))) {
                for (Type paramType : method.getParameterTypes()) {
                    if (paramType.toString().equals("android.view.View")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isBlackListedLayout(String layout) {
        List<String> blackListedLayouts = new ArrayList<>(Arrays.asList("abc_alert_dialog_title_material",
                "abc_alert_dialog_button_bar_material", "notification_template_big_media",
                "notification_action_tombstone", "abc_screen_content_include", "abc_popup_menu_item_layout",
                "notification_action", "abc_expanded_menu_layout", "notification_template_big_media_narrow_custom",
                "notification_template_part_chronometer", "abc_alert_dialog_button_bar_material",
                "abc_dialog_title_material", "abc_alert_dialog_title_material", "notification_template_media_custom",
                "abc_action_menu_item_layout", "select_dialog_singlechoice_material", "select_dialog_item_material",
                "select_dialog_multichoice_material", "abc_screen_toolbar", "abc_alert_dialog_material",
                "abc_action_mode_bar", "abc_action_mode_close_item_material", "notification_media_action",
                "abc_search_dropdown_item_icons_2line", "abc_screen_toolbar", "abc_list_menu_item_layout",
                "notification_template_big_media_custom", "abc_screen_simple_overlay_action_mode",
                "notification_template_custom_big", "abc_list_menu_item_checkbox", "abc_popup_menu_header_item_layout",
                "abc_list_menu_item_radio", "abc_activity_chooser_view", "abc_screen_simple",
                "abc_action_bar_up_container", "notification_template_icon_group", "abc_search_view", "abc_tooltip",
                "abc_cascading_menu_item_layout", "abc_activity_chooser_view_list_item", "custom_dialog",
                "notification_template_media", "notification_template_lines_media", "abc_action_menu_layout",
                "notification_template_big_media_narrow", "abc_action_bar_title_item",
                "notification_media_cancel_action", "support_simple_spinner_dropdown_item", "abc_list_menu_item_icon"));

        return blackListedLayouts.contains(layout);
    }

    public static boolean isValidClass(SootClass clazz) {
        return Filter.isValidClass(null, null, clazz);
    }

    public static boolean isValidClass(Collection<String> packageBlacklist, Collection<String> classBlacklist,
            SootClass clazz) {
        if (SystemClassHandler.v().isClassInSystemPackage(clazz) || clazz.isJavaLibraryClass() ||
                clazz.isLibraryClass() || clazz.isPhantomClass() || Scene.v().isExcluded(clazz)) {
            return false;
        }

        if (packageBlacklist != null) {
            if (!isValidPackage(packageBlacklist, clazz.getPackageName())) {
                return false;
            }
        }

        if (classBlacklist != null) {
            for (String blacklistClass : classBlacklist) {
                if (clazz.getShortName().contains(blacklistClass)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isValidMethod(SootMethod method) {
        return Filter.isValidMethod(null, null, method);
    }

    public static boolean isValidMethod(Collection<String> packageBlacklist, Collection<String> classBlacklist,
            SootMethod method) {
        return Filter.isValidClass(packageBlacklist, classBlacklist, method.getDeclaringClass());
    }

    private static boolean isValidPackage(Collection<String> blacklist, String packageName) {
        if (blacklist == null) {
            return true;
        }

        for (String blacklistPackage : blacklist) {
            if (blacklistPackage.startsWith(".")) {
                if (packageName.contains(blacklistPackage)) {
                    return false;
                }
            } else {
                if (packageName.startsWith(blacklistPackage)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static SootClass getParentClass(SootMethod method) {
        SootClass clazz = method.getDeclaringClass();
        return (clazz.hasOuterClass() ? clazz.getOuterClass() : clazz);
    }
}

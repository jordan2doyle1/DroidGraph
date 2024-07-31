package phd.research.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phd.research.helper.Tuple;
import phd.research.singletons.FlowDroidAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jordan Doyle
 */

public class LogHandler {

    @SuppressWarnings("unused")
    public static final String I_TAG = "<INTERACTION>";
    public static final String M_TAG = "<METHOD>";
    public static final String C_TAG = "<CONTROL>";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogHandler.class);

    public static Tuple<Integer, SootClass, SootMethod> regexLogMessage(String logMessage) {
        Matcher matcher;
        Pattern pattern;

        if (logMessage.contains(C_TAG)) {
            pattern = Pattern.compile("Method:\\s<(.+):\\s(.+)>\\sView:\\s(-?\\d+)");
        } else if (logMessage.contains(M_TAG)) {
            pattern = Pattern.compile("Method:\\s<(.+):\\s(.+)>");
        } else {
            LOGGER.error("No recognisable tag in log message: {}", logMessage);
            return null;
        }

        if (logMessage.contains("'")) {
            logMessage = logMessage.replaceAll("'", "");
            LOGGER.warn("Removed single quotation from log message.");
        }

        if (!FlowDroidAnalysis.v().isSootInitialised()) {
            FlowDroidAnalysis.v().initializeSoot();
        }

        matcher = pattern.matcher(logMessage);
        if (matcher.find()) {
            SootClass sootClass = Scene.v().getSootClassUnsafe(matcher.group(1));
            if (sootClass != null) {
                SootMethod method = sootClass.getMethodUnsafe(matcher.group(2));
                if (sootClass.hasOuterClass()) {
                    sootClass = sootClass.getOuterClass();
                    if (method == null) {
                        method = sootClass.getMethodUnsafe(matcher.group(2));
                    }
                }

                if (method != null) {
                    if (logMessage.contains(C_TAG)) {
                        Integer id = Integer.parseInt(matcher.group(3));
                        return new Tuple<>(id, sootClass, method);
                    } else if (logMessage.contains(M_TAG)) {
                        return new Tuple<>(-1, sootClass, method);
                    }
                } else {
                    LOGGER.error(
                            String.format("Failed to retrieve class %s found in Logcat message.", matcher.group(1)));
                }
            } else {
                LOGGER.error(String.format("Failed to retrieve method %s found in Logcat message.", matcher.group(2)));
            }
        }

        LOGGER.error("Failure while reading Logcat message: {}", logMessage);
        return null;
    }
}

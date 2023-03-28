package phd.research.utility;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jordan Doyle
 */

public class Bytecode {

    public static String signatureToJimple(String signature) throws RuntimeException {
        Pattern pattern = Pattern.compile("(\\[.|L.+;)->(.+)\\((.*)\\)(.+;|\\[?\\[?.) ?(\\[.+])?");
        Matcher matcher = pattern.matcher(signature);

        if (matcher.find()) {
            String className = Bytecode.typeToJimple(matcher.group(1).trim());
            String methodName = matcher.group(2).trim();
            String parameters = matcher.group(3).trim();
            String returnType = Bytecode.typeToJimple(matcher.group(4).trim());

            List<String> parameterList = new ArrayList<>(
                    parameters.equals("") ? Collections.emptyList() : Arrays.asList(parameters.split(" ")));
            parameterList.replaceAll(Bytecode::typeToJimple);

            return Bytecode.buildJimpleSignature(className, methodName, returnType, parameterList);
        }

        throw new RuntimeException("Bytecode method signature \"" + signature + "\" not recognised.");
    }

    private static String typeToJimple(String type) throws RuntimeException {
        if (type.startsWith("[")) {
            return Bytecode.typeToJimple(type.substring(1)) + "[]";
        } else if (type.startsWith("L")) {
            return type.substring(1, type.length() - 1).replace("/", ".");
        } else {
            return Bytecode.primitiveToJimple(type);
        }
    }

    private static String primitiveToJimple(String primitiveType) throws RuntimeException {
        switch (primitiveType) {
            case "V":
                return "void";
            case "Z":
                return "boolean";
            case "B":
                return "byte";
            case "C":
                return "char";
            case "S":
                return "short";
            case "I":
                return "int";
            case "J":
                return "long";
            case "F":
                return "float";
            case "D":
                return "double";
            default:
                throw new RuntimeException("Primitive type \"" + primitiveType + "\" not recognised.");
        }
    }

    private static String buildJimpleSignature(String className, String methodName, String returnType,
            Collection<String> parameters) {
        StringBuilder builder = new StringBuilder("<");
        builder.append(className).append(": ").append(returnType).append(" ").append(methodName).append("(");

        parameters.forEach(parameter -> builder.append(parameter).append(","));
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.append(")>").toString();
    }
}

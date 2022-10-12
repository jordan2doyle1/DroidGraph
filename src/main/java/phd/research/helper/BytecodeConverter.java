package phd.research.helper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jordan Doyle
 */

public class BytecodeConverter {

    public static String signatureToJimple(String signature) throws RuntimeException {
        Pattern pattern = Pattern.compile("L(.+);->(.+)\\((.*)\\)(.+;|\\[?\\[?.) ?(\\[.+])?");
        Matcher matcher = pattern.matcher(signature);

        if (matcher.find()) {
            String clazz = matcher.group(1).trim().replace("/", ".");
            String method = matcher.group(2).trim();
            String parameters = matcher.group(3).trim();
            String returnType = BytecodeConverter.typeToJimple(matcher.group(4).trim());

            List<String> parameterList = new ArrayList<>(
                    parameters.equals("") ? Collections.emptyList() : Arrays.asList(parameters.split(" ")));
            parameterList.replaceAll(BytecodeConverter::typeToJimple);

            return BytecodeConverter.buildMethodSignature(clazz, method, returnType, parameterList);
        }

        throw new RuntimeException(String.format("Bytecode method signature %s not recognised.", signature));
    }

    private static String typeToJimple(String type) throws RuntimeException {
        if (type.startsWith("[")) {
            return BytecodeConverter.typeToJimple(type.substring(1)) + "[]";
        } else if (type.startsWith("L")) {
            return type.substring(1, type.length() - 1).replace("/", ".");
        } else {
            return BytecodeConverter.primitiveToJimple(type);
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
                throw new RuntimeException(String.format("Primitive type %s not recognised.", primitiveType));
        }
    }

    private static String buildMethodSignature(String clazz, String method, String returnType,
            Collection<String> parameters) {
        StringBuilder builder = new StringBuilder("<");
        builder.append(clazz).append(": ").append(returnType).append(" ").append(method).append("(");

        parameters.forEach(parameter -> builder.append(parameter).append(","));
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.append(")>").toString();
    }
}

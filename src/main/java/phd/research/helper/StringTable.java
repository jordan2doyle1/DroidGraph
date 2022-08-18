package phd.research.helper;

import java.util.*;
import java.util.stream.Stream;

public class StringTable {

    @SuppressWarnings("unused")
    public static String simpleTable(String[][] data, boolean leftJustifiedRows) {
        Map<Integer, Integer> columnLengths = getColumnLengths(data);
        String formatString = getFormatString(columnLengths, leftJustifiedRows);

        StringBuilder stringBuilder = new StringBuilder();
        Stream.iterate(0, i -> ++i).limit(data.length)
                .forEach(a -> stringBuilder.append(String.format(formatString, (Object[]) data[a])));
        return stringBuilder.toString();
    }

    public static String tableWithLines(String[][] data, boolean leftJustifiedRows) {
        Map<Integer, Integer> columnLengths = getColumnLengths(data);
        String formatString = getFormatString(columnLengths, leftJustifiedRows);
        String line = getHeaderLine(columnLengths);

        StringBuilder stringBuilder = new StringBuilder(line);
        Arrays.stream(data).limit(1).forEach(a -> stringBuilder.append(String.format(formatString, (Object[]) a)));
        stringBuilder.append(line);

        Stream.iterate(1, i -> ++i).limit(data.length - 1)
                .forEach(a -> stringBuilder.append(String.format(formatString, (Object[]) data[a])));
        stringBuilder.append(line);
        return stringBuilder.toString();
    }

    @SuppressWarnings("unused")
    public static String tableWithLinesAndMaxWidth(String[][] data, boolean leftJustifiedRows, int maxWidth) {
        List<String[]> dataList = new ArrayList<>(Arrays.asList(data));
        List<String[]> finalDataList = new ArrayList<>();
        for (String[] row : dataList) {
            boolean needExtraRow;
            int splitRow = 0;
            do {
                needExtraRow = false;
                String[] newRow = new String[row.length];
                for (int i = 0; i < row.length; i++) {
                    if (row[i].length() < maxWidth) {
                        newRow[i] = splitRow == 0 ? row[i] : "";
                    } else if ((row[i].length() > (splitRow * maxWidth))) {
                        int end = Math.min(row[i].length(), ((splitRow * maxWidth) + maxWidth));
                        newRow[i] = row[i].substring((splitRow * maxWidth), end);
                        needExtraRow = true;
                    } else {
                        newRow[i] = "";
                    }
                }
                finalDataList.add(newRow);
                if (needExtraRow) {
                    splitRow++;
                }
            } while (needExtraRow);
        }
        String[][] finalData = new String[finalDataList.size()][finalDataList.get(0).length];
        for (int i = 0; i < finalData.length; i++) {
            finalData[i] = finalDataList.get(i);
        }

        Map<Integer, Integer> columnLengths = getColumnLengths(finalData);
        String formatString = getFormatString(columnLengths, leftJustifiedRows);
        String line = getHeaderLine(columnLengths);

        StringBuilder stringBuilder = new StringBuilder(line);
        Arrays.stream(finalData).limit(1).forEach(a -> stringBuilder.append(String.format(formatString, (Object[]) a)));
        stringBuilder.append(line);

        Stream.iterate(1, i -> ++i).limit(finalData.length - 1)
                .forEach(a -> stringBuilder.append(String.format(formatString, (Object[]) finalData[a])));
        stringBuilder.append(line);
        return stringBuilder.toString();
    }

    private static String getHeaderLine(Map<Integer, Integer> columnLengths) {
        String header = columnLengths.entrySet().stream().reduce("", (line, b) -> {
            String tempLine = "+-";
            tempLine = tempLine + Stream.iterate(0, i -> ++i).limit(b.getValue())
                    .reduce("", (ln1, b1) -> ln1 + "-", (a1, b1) -> a1 + b1);
            tempLine = tempLine + "-";
            return line + tempLine;
        }, (a, b) -> a + b);
        header = header + "+\n";
        return header;
    }

    private static Map<Integer, Integer> getColumnLengths(String[][] data) {
        Map<Integer, Integer> columnLengths = new HashMap<>();
        Arrays.stream(data).forEach(a -> Stream.iterate(0, i -> ++i).limit(a.length).forEach(i -> {
            columnLengths.putIfAbsent(i, 0);
            if (columnLengths.get(i) < a[i].length()) {
                columnLengths.put(i, a[i].length());
            }
        }));
        return columnLengths;
    }

    private static String getFormatString(Map<Integer, Integer> columnLengths, boolean leftJustifiedRows) {
        StringBuilder formatString = new StringBuilder();
        String flag = leftJustifiedRows ? "-" : "";
        columnLengths.forEach((key, value) -> formatString.append("| %").append(flag).append(value).append("s "));
        formatString.append("|\n");
        return formatString.toString();
    }
}

package by.strimka.input;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvMalformedLineException;
import com.opencsv.exceptions.CsvValidationException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Разбор CSV в CsvRow. На неверных данных не бросает исключение: все ошибки собираются
 * в CsvReadResult, чтобы пользователь увидел их разом. IOException — только при сбое самого Reader.
 */
public final class CsvReader {

    // Разделитель колонок — точка с запятой, как в формате входа.
    private static final char SEPARATOR = ';';

    // Допустимая форма дробного числа: необязательный минус, целая часть, необязательная дробная часть через точку.
    // Регулярное выражение нужно, потому что Double.parseDouble принимает 200d, 1f, 0x1p3, 1e3, +5, NaN.
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");

    private CsvReader() {
    }

    /**
     * Читает заголовок, затем строки данных.
     * Пустые строки пропускаются. Строка с неверным числом полей — ошибка.
     */
    public static CsvReadResult read(Reader source) throws IOException {
        List<CsvRow> rows = new ArrayList<>();
        List<CsvReadError> errors = new ArrayList<>();

        try (CSVReader csv = new CSVReaderBuilder(source)
                .withCSVParser(new CSVParserBuilder().withSeparator(SEPARATOR).build())
                .build()) {

            // Без заголовка нельзя сопоставить значения с колонками, поэтому дальше идти нет смысла.
            String[] header = nextRecord(csv, errors);
            if (header == null) {
                // Если чтение остановилось из-за ошибки формата, она уже записана в errors.
                if (errors.isEmpty()) {
                    errors.add(new CsvReadError(0, null, "файл пуст"));
                }
                return new CsvReadResult(rows, errors);
            }

            // Если заголовок содержит ошибки, строки данных не разбираем: их значения некуда сопоставить.
            Map<String, Integer> index = indexHeader(header, errors);
            if (!errors.isEmpty()) {
                return new CsvReadResult(rows, errors);
            }

            // Номер строки берётся через getLinesRead(), а не через счётчик записей:
            // значение в кавычках может занимать несколько физических строк.
            String[] values;
            while ((values = nextRecord(csv, errors)) != null) {
                long line = csv.getLinesRead();
                // Пустая строка (например, в конце файла) — не ошибка, просто пропускаем.
                if (values.length == 1 && values[0].isBlank()) {
                    continue;
                }
                if (values.length != header.length) {
                    errors.add(new CsvReadError(line, null,
                            "ожидалось полей " + header.length + ", получено " + values.length));
                    continue;
                }
                CsvRow row = toRow(line, values, index, errors);
                if (row != null) {
                    rows.add(row);
                }
            }
        }
        return new CsvReadResult(rows, errors);
    }

    /**
     * Читает следующую запись. Ошибка значения уровня записи (CsvValidationException) записывается
     * в список ошибок, и чтение продолжается со следующей записи.
     * Незакрытая кавычка (CsvMalformedLineException) записывается как ошибка, после чего возвращается null:
     * OpenCSV не даёт продолжить разбор после неё, поэтому остаток файла не читается.
     */
    private static String[] nextRecord(CSVReader csv, List<CsvReadError> errors) throws IOException {
        while (true) {
            try {
                return csv.readNext();
            } catch (CsvValidationException exception) {
                errors.add(new CsvReadError(csv.getLinesRead(), null, exception.getMessage()));
            } catch (CsvMalformedLineException exception) {
                errors.add(new CsvReadError(csv.getLinesRead(), null, "незакрытые кавычки"));
                return null;
            }
        }
    }

    /**
     * Строит соответствие «имя колонки → позиция». Порядок колонок во входе может отличаться
     * от CsvColumns.NAMES, поэтому значения ищутся по имени, а не по позиции.
     */
    private static Map<String, Integer> indexHeader(String[] header, List<CsvReadError> errors) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            String name = header[i].trim();
            if (!CsvColumns.NAMES.contains(name)) {
                errors.add(new CsvReadError(0, null, "неизвестная колонка «" + name + "»"));
            } else if (index.put(name, i) != null) {
                errors.add(new CsvReadError(0, null, "колонка «" + name + "» повторяется"));
            }
        }
        // Проверяем, что все обязательные колонки присутствуют.
        for (String column : CsvColumns.NAMES) {
            if (!index.containsKey(column)) {
                errors.add(new CsvReadError(0, null, "отсутствует обязательная колонка «" + column + "»"));
            }
        }
        return index;
    }

    /**
     * Превращает значения строки в CsvRow. Возвращает null, если в строке есть ошибки.
     * Сами ошибки уже добавлены в errors.
     */
    private static CsvRow toRow(long line, String[] values, Map<String, Integer> index,
                                List<CsvReadError> errors) {
        // Ошибки строки собираем отдельно: если их нет, строка попадает в результат, иначе null.
        List<CsvReadError> rowErrors = new ArrayList<>();
        CsvRow row = new CsvRow();
        row.setLineNumber(line);
        row.setTeamName(text(values, index, "teamName"));
        row.setPurpose(text(values, index, "purpose"));
        row.setCriticality(text(values, index, "criticality"));
        row.setMonth(integer(line, values, index, "month", rowErrors));
        row.setYear(integer(line, values, index, "year", rowErrors));
        row.setProdBugs(integer(line, values, index, "prodBugs", rowErrors));
        row.setClosedTasks(integer(line, values, index, "closedTasks", rowErrors));
        row.setTestBugs(integer(line, values, index, "testBugs", rowErrors));
        row.setTestingTime(decimal(line, values, index, "testingTime", rowErrors));
        row.setLeadTime(decimal(line, values, index, "leadTime", rowErrors));
        row.setChangesLeadTime(decimal(line, values, index, "changesLeadTime", rowErrors));
        row.setReleases(integer(line, values, index, "releases", rowErrors));
        row.setFailures(integer(line, values, index, "failures", rowErrors));
        row.setMttr(decimal(line, values, index, "mttr", rowErrors));
        errors.addAll(rowErrors);
        return rowErrors.isEmpty() ? row : null;
    }

    /**
     * Значение колонки без пробелов по краям. Пустая строка становится null:
     * отсутствующее значение обрабатывает валидатор.
     */
    private static String text(String[] values, Map<String, Integer> index, String column) {
        String raw = values[index.get(column)].trim();
        return raw.isEmpty() ? null : raw;
    }

    /**
     * Целое число. При ошибке формата добавляет ошибку в errors и возвращает null.
     */
    private static Integer integer(long line, String[] values, Map<String, Integer> index,
                                   String column, List<CsvReadError> errors) {
        String raw = text(values, index, column);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.valueOf(raw);
        } catch (NumberFormatException exception) {
            errors.add(new CsvReadError(line, column,
                    "ожидалось целое число, получено «" + raw + "»"));
            return null;
        }
    }

    /**
     * Дробное число. При ошибке добавляет ошибку в errors и возвращает null.
     * Запятая отвергается отдельным сообщением: Double.parseDouble её не принимает,
     * а общее сообщение «ожидалось число» было бы менее понятным.
     */
    private static Double decimal(long line, String[] values, Map<String, Integer> index,
                                  String column, List<CsvReadError> errors) {
        String raw = text(values, index, column);
        if (raw == null) {
            return null;
        }
        if (raw.contains(",")) {
            errors.add(new CsvReadError(line, column,
                    "используйте точку, получено «" + raw + "»"));
            return null;
        }
        if (!DECIMAL.matcher(raw).matches()) {
            errors.add(new CsvReadError(line, column, "ожидалось число, получено «" + raw + "»"));
            return null;
        }
        // Форма уже проверена регулярным выражением, поэтому parseDouble не бросит исключение.
        double value = Double.parseDouble(raw);
        // Очень длинная запись даёт бесконечность, для метрик она недопустима.
        if (!Double.isFinite(value)) {
            errors.add(new CsvReadError(line, column,
                    "ожидалось конечное число, получено «" + raw + "»"));
            return null;
        }
        return value;
    }
}

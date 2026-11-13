package by.strimka.validation;

import by.strimka.domain.Criticality;
import by.strimka.domain.PeriodRecord;
import by.strimka.input.CsvColumns;
import by.strimka.input.CsvReadError;
import by.strimka.input.CsvReadResult;
import by.strimka.input.CsvRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Проверка записей после разбора: обязательные поля, допустимые значения, диапазоны,
 * связи между полями и уникальность команды за период. Все ошибки собираются,
 * проверка не останавливается на первой.
 */
public final class InputValidator {

    // Диапазоны отсекают явные опечатки (например, год 0025 или месяц 13).
    private static final int MIN_MONTH = 1;
    private static final int MAX_MONTH = 12;
    private static final int MIN_YEAR = 2020;
    private static final int MAX_YEAR = 2100;

    private InputValidator() {
    }

    /**
     * Возвращает записи только при отсутствии ошибок: частично валидные данные в расчёт не попадают.
     */
    public static ValidationResult validate(CsvReadResult read) {
        List<CsvReadError> errors = new ArrayList<>(read.errors());

        for (CsvRow row : read.rows()) {
            checkRow(row, errors);
        }
        // Межстрочные проверки (дубликаты, согласованность команды) требуют видеть все строки сразу.
        checkTeams(read.rows(), errors);

        // Сортировка по номеру строки, затем по колонке: ошибки выводятся в порядке файла.
        errors.sort(Comparator
                .comparingLong(CsvReadError::line)
                .thenComparingInt(error -> columnRank(error.column())));

        if (!errors.isEmpty()) {
            return new ValidationResult(List.of(), errors);
        }
        // Все проверки пройдены, поэтому обязательные значения не null и распаковка в toRecord безопасна.
        List<PeriodRecord> records = new ArrayList<>();
        for (CsvRow row : read.rows()) {
            records.add(toRecord(row));
        }
        return new ValidationResult(records, errors);
    }

    /**
     * Проверки одной строки: наличие значений, допустимые значения, диапазоны и знак.
     * Проверки связей между полями — в checkLogic.
     */
    private static void checkRow(CsvRow row, List<CsvReadError> errors) {
        long line = row.getLineNumber();

        // Сначала проверяем наличие значений. Остальные проверки пропускают null,
        // чтобы не выводить вторичные ошибки по пустому полю.
        requireValue(row.getMonth(), line, "month", errors);
        requireValue(row.getYear(), line, "year", errors);
        requireValue(row.getTeamName(), line, "teamName", errors);
        requireValue(row.getPurpose(), line, "purpose", errors);
        requireValue(row.getCriticality(), line, "criticality", errors);
        requireValue(row.getProdBugs(), line, "prodBugs", errors);
        requireValue(row.getClosedTasks(), line, "closedTasks", errors);
        requireValue(row.getTestBugs(), line, "testBugs", errors);
        requireValue(row.getTestingTime(), line, "testingTime", errors);
        requireValue(row.getLeadTime(), line, "leadTime", errors);
        requireValue(row.getChangesLeadTime(), line, "changesLeadTime", errors);
        requireValue(row.getReleases(), line, "releases", errors);
        requireValue(row.getFailures(), line, "failures", errors);
        requireValue(row.getMttr(), line, "mttr", errors);

        // Список допустимых значений в тексте сообщения должен совпадать с enum Criticality.
        if (row.getCriticality() != null && Criticality.fromCsv(row.getCriticality()).isEmpty()) {
            errors.add(new CsvReadError(line, "criticality",
                    "допустимо BestEfforts, BusinessCritical, MissionCritical; получено «"
                            + row.getCriticality() + "»"));
        }
        if (row.getMonth() != null && (row.getMonth() < MIN_MONTH || row.getMonth() > MAX_MONTH)) {
            errors.add(new CsvReadError(line, "month",
                    "ожидалось от " + MIN_MONTH + " до " + MAX_MONTH + ", получено " + row.getMonth()));
        }
        if (row.getYear() != null && (row.getYear() < MIN_YEAR || row.getYear() > MAX_YEAR)) {
            errors.add(new CsvReadError(line, "year",
                    "ожидалось от " + MIN_YEAR + " до " + MAX_YEAR + ", получено " + row.getYear()));
        }

        checkNonNegative(row.getProdBugs(), line, "prodBugs", errors);
        checkNonNegative(row.getClosedTasks(), line, "closedTasks", errors);
        checkNonNegative(row.getTestBugs(), line, "testBugs", errors);
        checkNonNegative(row.getTestingTime(), line, "testingTime", errors);
        checkNonNegative(row.getLeadTime(), line, "leadTime", errors);
        checkNonNegative(row.getChangesLeadTime(), line, "changesLeadTime", errors);
        checkNonNegative(row.getReleases(), line, "releases", errors);
        checkNonNegative(row.getFailures(), line, "failures", errors);
        checkNonNegative(row.getMttr(), line, "mttr", errors);

        checkLogic(row, line, errors);
    }

    /**
     * Связи между полями. Нули в знаменателях проверяются здесь, поэтому расчёт метрик
     * не получит деления на ноль. Логика «больше» проверяет, что доли не превышают 100 %.
     */
    private static void checkLogic(CsvRow row, long line, List<CsvReadError> errors) {
        Integer closedTasks = row.getClosedTasks();

        // closedTasks, leadTime, releases и сумма багов — знаменатели метрик. Ноль в них недопустим.
        if (closedTasks != null && closedTasks == 0) {
            errors.add(new CsvReadError(line, null,
                    "closedTasks = 0, доли багов не рассчитываются"));
        }
        if (row.getLeadTime() != null && row.getLeadTime() == 0.0) {
            errors.add(new CsvReadError(line, null,
                    "leadTime = 0, Testing Cycle Time не рассчитывается"));
        }
        Integer releases = row.getReleases();
        if (releases != null && releases == 0) {
            errors.add(new CsvReadError(line, null,
                    "releases = 0, Change Failure Rate не рассчитывается"));
        }
        Integer prodBugs = row.getProdBugs();
        Integer testBugs = row.getTestBugs();
        if (prodBugs != null && testBugs != null && prodBugs + testBugs == 0) {
            errors.add(new CsvReadError(line, null,
                    "prodBugs и testBugs равны 0, протекание не рассчитывается"));
        }
        Integer failures = row.getFailures();
        if (failures != null && releases != null && failures > releases) {
            errors.add(new CsvReadError(line, null, "failures больше releases"));
        }
        if (prodBugs != null && closedTasks != null && prodBugs > closedTasks) {
            errors.add(new CsvReadError(line, null, "prodBugs больше closedTasks"));
        }
        if (testBugs != null && closedTasks != null && testBugs > closedTasks) {
            errors.add(new CsvReadError(line, null, "testBugs больше closedTasks"));
        }
    }

    /**
     * Проверки между строками: одна команда не может иметь два набора данных за один месяц,
     * а назначение и критичность команды должны совпадать во всех строках.
     */
    private static void checkTeams(List<CsvRow> rows, List<CsvReadError> errors) {
        // byPeriod ищет дубликаты по ключу «команда|год|месяц».
        // firstByTeam хранит первую строку команды для сверки назначения и критичности.
        Map<String, CsvRow> byPeriod = new HashMap<>();
        Map<String, CsvRow> firstByTeam = new HashMap<>();

        for (CsvRow row : rows) {
            // Строки без ключевых полей пропускаем: пустые значения уже отмечены ошибкой.
            if (row.getTeamName() == null || row.getMonth() == null || row.getYear() == null) {
                continue;
            }
            // Дубликат ищем только по корректному ключу. Строка с месяцем 13 или годом вне диапазона
            // уже отмечена ошибкой диапазона, и её пара не должна давать ложный дубликат.
            if (hasValidPeriod(row)) {
                String periodKey = row.getTeamName() + "|" + row.getYear() + "|" + row.getMonth();
                CsvRow earlier = byPeriod.putIfAbsent(periodKey, row);
                if (earlier != null) {
                    errors.add(new CsvReadError(row.getLineNumber(), null,
                            "дубликат команды «" + row.getTeamName() + "» за " + row.getMonth() + "/"
                                    + row.getYear() + " (впервые в строке " + earlier.getLineNumber() + ")"));
                    continue;
                }
            }

            CsvRow first = firstByTeam.putIfAbsent(row.getTeamName(), row);
            if (first == null) {
                continue;
            }
            if (row.getPurpose() != null && first.getPurpose() != null
                    && !row.getPurpose().equals(first.getPurpose())) {
                errors.add(new CsvReadError(row.getLineNumber(), "purpose",
                        "у команды «" + row.getTeamName() + "» назначение отличается от строки "
                                + first.getLineNumber()));
            }
            if (row.getCriticality() != null && first.getCriticality() != null
                    && !row.getCriticality().equals(first.getCriticality())) {
                errors.add(new CsvReadError(row.getLineNumber(), "criticality",
                        "у команды «" + row.getTeamName() + "» критичность отличается от строки "
                                + first.getLineNumber()));
            }
        }
    }

    /**
     * Месяц и год в допустимых диапазонах. Вызывается только после проверки на null.
     */
    private static boolean hasValidPeriod(CsvRow row) {
        return row.getMonth() >= MIN_MONTH && row.getMonth() <= MAX_MONTH
                && row.getYear() >= MIN_YEAR && row.getYear() <= MAX_YEAR;
    }

    /**
     * Перевод строки в доменную запись. orElseThrow безопасен: критичность уже проверена
     * тем же набором допустимых значений.
     */
    private static PeriodRecord toRecord(CsvRow row) {
        return new PeriodRecord(
                row.getYear(),
                row.getMonth(),
                row.getTeamName(),
                row.getPurpose(),
                Criticality.fromCsv(row.getCriticality()).orElseThrow(),
                row.getProdBugs(),
                row.getClosedTasks(),
                row.getTestBugs(),
                row.getTestingTime(),
                row.getLeadTime(),
                row.getChangesLeadTime(),
                row.getReleases(),
                row.getFailures(),
                row.getMttr());
    }

    /**
     * Фиксирует пустое значение. Параметр Object позволяет использовать метод для любого типа поля.
     */
    private static void requireValue(Object value, long line, String column, List<CsvReadError> errors) {
        if (value == null) {
            errors.add(new CsvReadError(line, column, "значение пустое"));
        }
    }

    /**
     * Счётчики и время не могут быть отрицательными.
     */
    private static void checkNonNegative(Number value, long line, String column, List<CsvReadError> errors) {
        if (value != null && value.doubleValue() < 0) {
            errors.add(new CsvReadError(line, column, "значение не может быть отрицательным"));
        }
    }

    /**
     * Позиция колонки в CsvColumns. Ошибки уровня строки (column == null) идут первыми,
     * неизвестные колонки — последними.
     */
    private static int columnRank(String column) {
        if (column == null) {
            return -1;
        }
        int rank = CsvColumns.NAMES.indexOf(column);
        return rank == -1 ? CsvColumns.NAMES.size() : rank;
    }
}

package by.strimka.input;

import java.util.List;

/**
 * Состав и порядок колонок входного CSV. Единственный источник:
 * его используют чтение (проверка заголовка) и валидатор (порядок ошибок).
 */
public final class CsvColumns {

    public static final List<String> NAMES = List.of(
            "month", "year", "teamName", "purpose", "criticality",
            "prodBugs", "closedTasks", "testBugs", "testingTime", "leadTime",
            "changesLeadTime", "releases", "failures", "mttr");

    // Утилитный класс: содержит только константу, экземпляры не нужны.
    private CsvColumns() {
    }
}

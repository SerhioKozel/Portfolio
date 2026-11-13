package by.strimka.input;

import java.util.List;

/**
 * Результат разбора файла: прочитанные строки и все найденные ошибки.
 */
public record CsvReadResult(List<CsvRow> rows, List<CsvReadError> errors) {

    // Копии списков защищают результат от изменения извне.

    /**
     * Копирует списки строк и ошибок, чтобы результат не менялся извне.
     */
    public CsvReadResult {
        rows = List.copyOf(rows);
        errors = List.copyOf(errors);
    }
}

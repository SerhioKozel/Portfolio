package by.strimka.validation;

import by.strimka.domain.PeriodRecord;
import by.strimka.input.CsvReadError;
import java.util.List;

/**
 * Результат валидации: записи (только при отсутствии ошибок) и список ошибок.
 */
public record ValidationResult(List<PeriodRecord> records, List<CsvReadError> errors) {

    // Compact-конструктор: копии списков защищают результат от изменения извне.

    /**
     * Копирует списки записей и ошибок, чтобы результат не менялся извне.
     */
    public ValidationResult {
        records = List.copyOf(records);
        errors = List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}

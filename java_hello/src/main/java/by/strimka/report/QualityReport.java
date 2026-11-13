package by.strimka.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Корень JSON-отчёта: список команд.
 */
@JsonPropertyOrder({"entries"})
public record QualityReport(@JsonProperty("entries") List<TeamReport> entries) {

    /**
     * Копирует список команд, чтобы отчёт не менялся извне.
     */
    public QualityReport {
        entries = List.copyOf(entries);
    }
}

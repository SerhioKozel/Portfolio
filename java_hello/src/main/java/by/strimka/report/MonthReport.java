package by.strimka.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.util.List;

/**
 * Месяц команды в JSON-отчёте.
 */
@JsonPropertyOrder({"year", "month", "percent", "components", "metrics"})
public record MonthReport(
        @JsonProperty("year") int year,
        @JsonProperty("month") int month,
        @JsonProperty("percent") BigDecimal percent,
        @JsonProperty("components") Components components,
        @JsonProperty("metrics") List<MetricReport> metrics) {

    /**
     * Копирует список метрик, чтобы отчёт месяца не менялся извне.
     */
    public MonthReport {
        metrics = List.copyOf(metrics);
    }

    /**
     * Компоненты формулы ИТОГ: баланс по категориям и суммарные веса.
     */
    @JsonPropertyOrder({"dora", "quality", "total"})
    public record Components(
            @JsonProperty("dora") Balance dora,
            @JsonProperty("quality") Balance quality,
            @JsonProperty("total") Total total) {
    }

    /**
     * Вес выполненных (passed) и невыполненных (failed) метрик категории.
     */
    @JsonPropertyOrder({"passed", "failed"})
    public record Balance(
            @JsonProperty("passed") int passed,
            @JsonProperty("failed") int failed) {
    }

    /**
     * Суммарный вес категории.
     */
    @JsonPropertyOrder({"dora", "quality"})
    public record Total(
            @JsonProperty("dora") int dora,
            @JsonProperty("quality") int quality) {
    }
}

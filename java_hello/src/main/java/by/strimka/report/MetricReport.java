package by.strimka.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;

/**
 * Метрика в JSON-отчёте. Только строки и числа, без внутренних типов домена,
 * чтобы формат отчёта не зависел от классов расчёта.
 * {@link JsonProperty} фиксирует имя в JSON: переименование поля в коде не меняет контракт.
 */
@JsonPropertyOrder({"name", "category", "weight", "level", "value"})
public record MetricReport(
        @JsonProperty("name") String name,
        @JsonProperty("category") String category,
        @JsonProperty("weight") int weight,
        @JsonProperty("level") String level,
        @JsonProperty("value") BigDecimal value) {
}

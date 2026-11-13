package by.strimka.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * Команда в JSON-отчёте: сведения о команде и результат по месяцам.
 */
@JsonPropertyOrder({"team", "result"})
public record TeamReport(
        @JsonProperty("team") Team team,
        @JsonProperty("result") Result result) {

    /**
     * Сведения о команде в отчёте.
     */
    @JsonPropertyOrder({"name", "purpose", "criticality"})
    public record Team(
            @JsonProperty("name") String name,
            @JsonProperty("purpose") String purpose,
            @JsonProperty("criticality") String criticality) {
    }

    /**
     * Итог команды: месяцы, число месяцев ниже порога и вердикт.
     */
    @JsonPropertyOrder({"months", "monthsBelowFifty", "verdict"})
    public record Result(
            @JsonProperty("months") List<MonthReport> months,
            @JsonProperty("monthsBelowFifty") long monthsBelowFifty,
            @JsonProperty("verdict") String verdict) {

        /**
         * Копирует список месяцев, чтобы итог не менялся извне.
         */
        public Result {
            months = List.copyOf(months);
        }
    }
}

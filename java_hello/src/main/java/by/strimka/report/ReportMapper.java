package by.strimka.report;

import by.strimka.evaluation.PeriodScore;
import by.strimka.evaluation.QualityEntry;
import by.strimka.evaluation.QualityResult;
import by.strimka.metrics.Metric;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Переводит доменные объекты (QualityEntry, PeriodScore) в DTO отчёта.
 * Расчётов не делает: только переносит данные и округляет для вывода.
 */
public final class ReportMapper {

    // Значения вердикта в отчёте.
    private static final String PASSED_VERDICT = "PASSED";
    private static final String FAILED_VERDICT = "FAILED";

    private ReportMapper() {
    }

    /**
     * Преобразует результаты оценки всех команд в отчёт.
     */
    public static QualityReport toReport(List<QualityEntry> entries) {
        List<TeamReport> teams = new ArrayList<>();
        for (QualityEntry entry : entries) {
            teams.add(toTeamReport(entry));
        }
        return new QualityReport(teams);
    }

    private static TeamReport toTeamReport(QualityEntry entry) {
        TeamReport.Team team = new TeamReport.Team(
                entry.team().name(),
                entry.team().purpose(),
                entry.team().criticality().csvValue());

        QualityResult quality = entry.result();
        List<MonthReport> months = new ArrayList<>();
        for (PeriodScore score : quality.months()) {
            months.add(toMonthReport(score));
        }

        TeamReport.Result result = new TeamReport.Result(
                months,
                quality.monthsBelowFifty(),
                quality.passed() ? PASSED_VERDICT : FAILED_VERDICT);
        return new TeamReport(team, result);
    }

    private static MonthReport toMonthReport(PeriodScore score) {
        MonthReport.Components components = new MonthReport.Components(
                new MonthReport.Balance(score.doraPassed(), score.doraFailed()),
                new MonthReport.Balance(score.qualityPassed(), score.qualityFailed()),
                new MonthReport.Total(score.doraTotal(), score.qualityTotal()));

        List<MetricReport> metrics = new ArrayList<>();
        for (Metric metric : score.metrics()) {
            metrics.add(new MetricReport(
                    metric.code().name(),
                    metric.category().name(),
                    metric.weight(),
                    metric.level().name(),
                    round(metric.value())));
        }
        return new MonthReport(score.year(), score.month(), round(score.percent()), components, metrics);
    }

    /**
     * Округление только для вывода. Сравнения в расчёте выполняются по сырым значениям.
     * Возвращается BigDecimal, чтобы JSON печатал два знака после запятой.
     */
    static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}

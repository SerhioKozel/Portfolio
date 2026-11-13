package by.strimka.output;

import by.strimka.input.CsvReadError;
import by.strimka.metrics.MetricCode;
import by.strimka.report.MetricReport;
import by.strimka.report.MonthReport;
import by.strimka.report.QualityReport;
import by.strimka.report.TeamReport;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Вывод в консоль через log4j. Текст собирается заранее и выводится одним вызовом.
 */
public final class ConsoleRenderer {

    private static final Logger log = LogManager.getLogger(ConsoleRenderer.class);

    private ConsoleRenderer() {
    }

    /**
     * Выводит отчёт по командам в лог.
     */
    public static void printReport(QualityReport report) {
        log.info(renderReport(report));
    }

    /**
     * Выводит ошибки входного файла в лог.
     */
    public static void printErrors(List<CsvReadError> errors) {
        StringBuilder text = new StringBuilder();
        for (CsvReadError error : errors) {
            text.append(formatError(error)).append('\n');
        }
        text.append("Найдено ошибок: ").append(errors.size()).append(". Отчёт не создан.");
        log.error(text.toString());
    }

    /**
     * Формат: «строка N, колонка «X»: сообщение». Ошибки уровня файла (line = 0) — с префиксом «файл».
     */
    static String formatError(CsvReadError error) {
        if (error.line() == 0) {
            return "файл: " + error.message();
        }
        StringBuilder prefix = new StringBuilder("строка ").append(error.line());
        if (error.column() != null) {
            prefix.append(", колонка «").append(error.column()).append('»');
        }
        return prefix.append(": ").append(error.message()).toString();
    }

    /**
     * Текст отчёта для консоли. Строка «ниже 50 %» соответствует правилу QualityResult.
     */
    static String renderReport(QualityReport report) {
        StringBuilder text = new StringBuilder();
        int failed = 0;
        for (TeamReport entry : report.entries()) {
            TeamReport.Team team = entry.team();
            TeamReport.Result result = entry.result();
            String verdict = "PASSED".equals(result.verdict())
                    ? "выполнила требования качества"
                    : "не выполнила требования качества";
            // Счётчик непрошедших команд для итоговой строки.
            if (!"PASSED".equals(result.verdict())) {
                failed++;
            }
            text.append("Команда «").append(team.name()).append("» (")
                    .append(team.purpose()).append(", ").append(team.criticality()).append("): ")
                    .append(verdict).append('\n');
            for (MonthReport month : result.months()) {
                // %n — системный перевод строки. %% выводит знак процента.
                text.append(String.format(Locale.ROOT, "  %04d-%02d: %.2f %%%n",
                        month.year(), month.month(), month.percent()));
                for (MetricReport metric : month.metrics()) {
                    String title = MetricCode.valueOf(metric.name()).title();
                    text.append(String.format(Locale.ROOT, "      %s: %s (%.2f)%n",
                            title, metric.level(), metric.value()));
                }
            }
            text.append("  ниже 50 %: ").append(result.monthsBelowFifty()).append('\n');
        }
        text.append("Итог: команд ").append(report.entries().size())
                .append(", не выполнили: ").append(failed);
        return text.toString();
    }
}

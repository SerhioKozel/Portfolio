package by.strimka.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import by.strimka.evaluation.TeamEvaluator;
import by.strimka.input.CsvReadResult;
import by.strimka.input.CsvReader;
import by.strimka.validation.InputValidator;
import by.strimka.validation.ValidationResult;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Компонентные тесты сборки отчёта: разбор, валидация, расчёт и отчёт вместе, без App и без файлов.
 * Коды возврата и JSON-файл проверяются сквозными тестами в AppTest.
 */
@Tag("component")
@Epic("Калькулятор качества")
@Feature("Отчёт")
class ReportPipelineTest {

    private static final String HEADER = "month;year;teamName;purpose;criticality;prodBugs;closedTasks;testBugs;"
            + "testingTime;leadTime;changesLeadTime;releases;failures;mttr";

    // Лямбды Allure.step не объявляют checked-исключений, поэтому IOException оборачиваем здесь.
    private static QualityReport buildReport(String text) {
        try {
            CsvReadResult read = CsvReader.read(new StringReader(text));
            ValidationResult validation = InputValidator.validate(read);
            assertTrue(validation.errors().isEmpty(), "входные данные должны быть валидны");
            return ReportMapper.toReport(TeamEvaluator.evaluateAll(validation.records()));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Test
    @DisplayName("Месяцы в отчёте идут по порядку, даже если во входе они перепутаны")
    void buildReport_sortsMonths_whenInputIsUnordered() {
        Allure.step("Act: строка месяца 10 идёт раньше строки месяца 9", step -> {
            String october = "10;2026;Payments;Платежи;BestEfforts;5;100;30;200;1000;50;40;1;30";
            String september = "9;2026;Payments;Платежи;BestEfforts;5;100;30;200;1000;50;40;1;30";
            QualityReport report = buildReport(HEADER + "\n" + october + "\n" + september + "\n");
            Allure.step("Assert: первый месяц — 9, второй — 10", s -> {
                List<MonthReport> months = report.entries().get(0).result().months();
                assertEquals(9, months.get(0).month());
                assertEquals(10, months.get(1).month());
            });
        });
    }

    @Test
    @DisplayName("Команды в отчёте идут в порядке первого появления во входе")
    void buildReport_keepsTeamOrderOfFirstAppearance() {
        Allure.step("Act: сначала строка команды Beta, потом Alpha", step -> {
            String beta = "9;2026;Beta;Тест;BestEfforts;5;100;30;200;1000;50;40;1;30";
            String alpha = "9;2026;Alpha;Тест;BestEfforts;5;100;30;200;1000;50;40;1;30";
            QualityReport report = buildReport(HEADER + "\n" + beta + "\n" + alpha + "\n");
            Allure.step("Assert: первая команда — Beta, вторая — Alpha", s -> {
                assertEquals("Beta", report.entries().get(0).team().name());
                assertEquals("Alpha", report.entries().get(1).team().name());
            });
        });
    }
}

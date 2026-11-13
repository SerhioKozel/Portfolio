package by.strimka.metrics.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import by.strimka.exception.InvalidInputException;
import by.strimka.metrics.MetricLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@Epic("Калькулятор качества")
@Feature("Метрики качества")
class ProdBugsMetricTest {

    @Test
    @DisplayName("5 багов прода при 100 закрытых задачах дают 5 % и уровень PASSED")
    void value_returnsPercent_forTypicalInput() {
        Allure.step("Act: 5 багов прода при 100 закрытых задачах дают 5 % и уровень PASSED", step -> {
            ProdBugsMetric metric = new ProdBugsMetric(5, 100);
            Allure.step("Assert: доля 5.0 %", s -> {
                assertEquals(5.0, metric.value(), 1e-9);
            });
            Allure.step("Assert: уровень PASSED, порог " + metric.getSla(), s -> {
                assertEquals(MetricLevel.PASSED, metric.level());
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Доля багов прода: порог 10 %, ниже — PASSED, от 10 % — FAILED")
    @CsvSource({"9, 100, PASSED", "10, 100, FAILED", "11, 100, FAILED"})
    void level_returnsExpected_aroundSla(double prodBugs, double closedTasks, MetricLevel expected) {
        String title = "Act: " + prodBugs + " багов прода при " + closedTasks
                + " задачах дают уровень " + expected;
        Allure.step(title, step -> {
            MetricLevel actual = new ProdBugsMetric(prodBugs, closedTasks).level();
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }

    @Test
    @DisplayName("Ноль закрытых задач вызывает InvalidInputException")
    void value_throwsInvalidInputException_whenClosedTasksIsZero() {
        Allure.step("Act: ноль закрытых задач вызывает InvalidInputException", step -> {
            ProdBugsMetric metric = new ProdBugsMetric(5, 0);
            assertThrows(InvalidInputException.class, metric::value);
        });
    }
}

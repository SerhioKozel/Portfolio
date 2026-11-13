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
class TestBugsMetricTest {

    @Test
    @DisplayName("40 багов из тестирования при 100 закрытых задачах дают 40 % и уровень PASSED")
    void value_returnsPercent_forTypicalInput() {
        Allure.step("Act: 40 багов из тестирования при 100 закрытых задачах дают 40 % и уровень PASSED", step -> {
            TestBugsMetric metric = new TestBugsMetric(40, 100);
            Allure.step("Assert: доля 40.0 %", s -> {
                assertEquals(40.0, metric.value(), 1e-9);
            });
            Allure.step("Assert: уровень PASSED, порог " + metric.getSla(), s -> {
                assertEquals(MetricLevel.PASSED, metric.level());
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Доля багов из тестирования: порог 80 %, ниже — PASSED, от 80 % — FAILED")
    @CsvSource({"79, 100, PASSED", "80, 100, FAILED", "81, 100, FAILED"})
    void level_returnsExpected_aroundSla(double testBugs, double closedTasks, MetricLevel expected) {
        String title = "Act: " + testBugs + " багов из тестирования при " + closedTasks
                + " задачах дают уровень " + expected;
        Allure.step(title, step -> {
            MetricLevel actual = new TestBugsMetric(testBugs, closedTasks).level();
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }

    @Test
    @DisplayName("Ноль закрытых задач вызывает InvalidInputException")
    void value_throwsInvalidInputException_whenClosedTasksIsZero() {
        Allure.step("Act: ноль закрытых задач вызывает InvalidInputException", step -> {
            TestBugsMetric metric = new TestBugsMetric(5, 0);
            assertThrows(InvalidInputException.class, metric::value);
        });
    }
}

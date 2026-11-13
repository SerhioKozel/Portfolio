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
class ProdBugsLeakMetricTest {

    @Test
    @DisplayName("20 багов прода и 80 багов тестирования дают протекание 20 % и уровень PASSED")
    void value_returnsLeakPercent_forTypicalInput() {
        Allure.step("Act: 20 багов прода и 80 багов тестирования дают протекание 20 % и уровень PASSED", step -> {
            ProdBugsLeakMetric metric = new ProdBugsLeakMetric(20, 80);
            Allure.step("Assert: протекание 20.0 %", s -> {
                assertEquals(20.0, metric.value(), 1e-9);
            });
            Allure.step("Assert: уровень PASSED, порог " + metric.getSla(), s -> {
                assertEquals(MetricLevel.PASSED, metric.level());
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Протекание дефектов на прод: порог 50 %, ниже — PASSED, от 50 % — FAILED")
    @CsvSource({"49, 51, PASSED", "50, 50, FAILED", "51, 49, FAILED"})
    void level_returnsExpected_aroundSla(double prodBugs, double testBugs, MetricLevel expected) {
        String title = "Act: " + prodBugs + " багов прода и " + testBugs
                + " тестирования дают уровень " + expected;
        Allure.step(title, step -> {
            MetricLevel actual = new ProdBugsLeakMetric(prodBugs, testBugs).level();
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }

    @Test
    @DisplayName("Только баги прода дают протекание 100 % и уровень FAILED")
    void level_failed_whenOnlyProdBugsExist() {
        Allure.step("Act: только баги прода дают протекание 100 % и уровень FAILED", step -> {
            ProdBugsLeakMetric metric = new ProdBugsLeakMetric(5, 0);
            Allure.step("Assert: протекание 100.0 %", s -> {
                assertEquals(100.0, metric.value(), 1e-9);
            });
            Allure.step("Assert: уровень FAILED", s -> {
                assertEquals(MetricLevel.FAILED, metric.level());
            });
        });
    }

    @Test
    @DisplayName("Отсутствие багов вызывает InvalidInputException")
    void value_throwsInvalidInputException_whenNoBugs() {
        Allure.step("Act: отсутствие багов вызывает InvalidInputException", step -> {
            ProdBugsLeakMetric metric = new ProdBugsLeakMetric(0, 0);
            assertThrows(InvalidInputException.class, metric::value);
        });
    }
}

package by.strimka.metrics.dora;

import static org.junit.jupiter.api.Assertions.assertEquals;

import by.strimka.metrics.MetricLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@Epic("Калькулятор качества")
@Feature("Метрики DORA")
class MttrCalculatorTest {

    private final MttrCalculator calculator = new MttrCalculator();

    @ParameterizedTest
    @DisplayName("MTTR: PRO до 4,383 мин включительно, OTHER до 1972,35 мин включительно, свыше — FAILED")
    @CsvSource({"4.383, PRO", "4.384, OTHER", "1972.35, OTHER", "1972.36, FAILED"})
    void checkSla_returnsLevel_aroundBudgets(double minutes, MetricLevel expected) {
        Allure.step("Act: " + minutes + " мин MTTR дают уровень " + expected, step -> {
            MetricLevel actual = calculator.checkSla(minutes);
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }
}

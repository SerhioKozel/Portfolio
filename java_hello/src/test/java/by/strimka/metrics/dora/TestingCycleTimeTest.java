package by.strimka.metrics.dora;

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
@Feature("Метрики DORA")
class TestingCycleTimeTest {

    private final TestingCycleTime calculator = new TestingCycleTime();

    @Test
    @DisplayName("Lead Time 14.5 ч и тестирование 3 ч дают около 20.69 %")
    void calculate_returnsTestingPercent_forTypicalInput() {
        Allure.step("Act: Lead Time 14.5 ч и тестирование 3 ч дают около 20.69 %", step -> {
            double percent = calculator.calculate(14.5, 3);
            Allure.step("Assert: около 20.69 %", s -> {
                assertEquals(20.69, percent, 0.01);
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Testing Cycle Time: PRO до 30 % включительно, OTHER до 50 % включительно, свыше 50 % — FAILED")
    @CsvSource({"30.0, PRO", "30.01, OTHER", "50.0, OTHER", "50.01, FAILED"})
    void checkSla_returnsLevel_aroundLimits(double percent, MetricLevel expected) {
        Allure.step("Act: " + percent + " % дают уровень " + expected, step -> {
            MetricLevel actual = calculator.checkSla(percent);
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }

    @Test
    @DisplayName("Lead Time = 0 вызывает InvalidInputException")
    void calculate_throwsInvalidInputException_whenLeadTimeIsZero() {
        Allure.step("Act: Lead Time = 0 вызывает InvalidInputException", step -> {
            assertThrows(InvalidInputException.class, () -> calculator.calculate(0, 3));
        });
    }
}

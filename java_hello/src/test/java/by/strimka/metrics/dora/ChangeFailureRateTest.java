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
class ChangeFailureRateTest {

    private final ChangeFailureRate calculator = new ChangeFailureRate();

    @Test
    @DisplayName("40 релизов и 1 сбой дают 2.5 %")
    void calculate_returnsFailurePercent_forTypicalInput() {
        Allure.step("Act: 40 релизов и 1 сбой дают 2.5 %", step -> {
            double percent = calculator.calculate(40, 1);
            Allure.step("Assert: 2.5 %", s -> {
                assertEquals(2.5, percent, 1e-9);
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Change Failure Rate: PRO до 3 % включительно, OTHER до 10 % включительно, свыше 10 % — FAILED")
    @CsvSource({"3.0, PRO", "3.01, OTHER", "10.0, OTHER", "10.01, FAILED"})
    void checkSla_returnsLevel_aroundLimits(double percent, MetricLevel expected) {
        Allure.step("Act: " + percent + " % дают уровень " + expected, step -> {
            MetricLevel actual = calculator.checkSla(percent);
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }

    @Test
    @DisplayName("Ноль релизов вызывает InvalidInputException")
    void calculate_throwsInvalidInputException_whenReleasesIsZero() {
        Allure.step("Act: ноль релизов вызывает InvalidInputException", step -> {
            assertThrows(InvalidInputException.class, () -> calculator.calculate(0, 1));
        });
    }
}

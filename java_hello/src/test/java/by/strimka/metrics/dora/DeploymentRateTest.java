package by.strimka.metrics.dora;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class DeploymentRateTest {

    private final DeploymentRate calculator = new DeploymentRate();

    @Test
    @DisplayName("25 релизов за 20 рабочих дней дают 1.25 релиза в день")
    void calculate_returnsReleasesPerDay_forTypicalInput() {
        Allure.step("Act: 25 релизов за 20 рабочих дней дают 1.25 релиза в день", step -> {
            double perDay = calculator.calculate(25);
            Allure.step("Assert: 1.25 релиза в день", s -> {
                assertEquals(1.25, perDay, 1e-9);
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Deployment Rate: PRO строго больше 1,0 в день, OTHER от 0,2 включительно, ниже 0,2 — FAILED")
    @CsvSource({"1.0, OTHER", "1.01, PRO", "0.2, OTHER", "0.19, FAILED"})
    void checkSla_returnsLevel_aroundLimits(double releasesPerDay, MetricLevel expected) {
        Allure.step("Act: " + releasesPerDay + " релиза в день дают уровень " + expected, step -> {
            MetricLevel actual = calculator.checkSla(releasesPerDay);
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }

    @Test
    @DisplayName("20 релизов за месяц дают уровень OTHER, граница PRO строгая")
    void calculate_returnsOther_whenTwentyReleasesPerMonth() {
        Allure.step("Act: 20 релизов за месяц дают уровень OTHER, граница PRO строгая", step -> {
            double perDay = calculator.calculate(20);
            Allure.step("Assert: уровень OTHER", s -> {
                assertEquals(MetricLevel.OTHER, calculator.checkSla(perDay));
            });
        });
    }
}

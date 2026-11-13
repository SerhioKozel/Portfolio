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
class ChangesLeadTimeTest {

    private final ChangesLeadTime calculator = new ChangesLeadTime();

    @ParameterizedTest
    @DisplayName("Changes Lead Time: PRO до 60 ч включительно, OTHER до 120 ч включительно, свыше 120 ч — FAILED")
    @CsvSource({"60.0, PRO", "60.01, OTHER", "120.0, OTHER", "120.01, FAILED"})
    void checkSla_returnsLevel_aroundLimits(double hours, MetricLevel expected) {
        Allure.step("Act: " + hours + " ч Changes Lead Time дают уровень " + expected, step -> {
            MetricLevel actual = calculator.checkSla(hours);
            Allure.step("Assert: уровень " + expected, s -> {
                assertEquals(expected, actual);
            });
        });
    }
}

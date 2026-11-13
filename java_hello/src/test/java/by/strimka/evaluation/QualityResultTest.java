package by.strimka.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import by.strimka.metrics.Metric;
import by.strimka.metrics.MetricCatalog;
import by.strimka.metrics.MetricCode;
import by.strimka.metrics.MetricLevel;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Epic("Калькулятор качества")
@Feature("Расчёт вердикта")
class QualityResultTest {

    private static final List<MetricCode> DORA = List.of(
            MetricCode.CHANGES_LEAD_TIME, MetricCode.CHANGE_FAILURE_RATE,
            MetricCode.DEPLOYMENT_RATE, MetricCode.MTTR);
    private static final List<MetricCode> QUALITY = List.of(
            MetricCode.PROD_BUGS_SHARE, MetricCode.TEST_BUGS_SHARE,
            MetricCode.PROD_BUGS_LEAK, MetricCode.TESTING_CYCLE_TIME);

    /**
     * Месяц: doraPassed DORA-метрик выполнены (PRO), qualityPassed quality-метрик выполнены.
     */
    private static PeriodScore month(int month, int doraPassed, int qualityPassed) {
        List<Metric> metrics = new ArrayList<>();
        for (int i = 0; i < DORA.size(); i++) {
            metrics.add(result(DORA.get(i), i < doraPassed ? MetricLevel.PRO : MetricLevel.FAILED));
        }
        for (int i = 0; i < QUALITY.size(); i++) {
            metrics.add(result(QUALITY.get(i), i < qualityPassed ? MetricLevel.PASSED : MetricLevel.FAILED));
        }
        return PeriodScore.of(2026, month, metrics);
    }

    private static Metric result(MetricCode code, MetricLevel level) {
        MetricCatalog.Definition definition = MetricCatalog.get(code);
        return new Metric(code, definition.category(), definition.weight(), level, 0.0);
    }

    private static PeriodScore good(int month) {
        return month(month, 4, 4);
    }

    /**
     * ИТОГ = 50 % ровно: (8 + 1 − 3) / 12 × 100.
     */
    private static PeriodScore exactlyFifty(int month) {
        return month(month, 4, 1);
    }

    private static PeriodScore low(int month) {
        return month(month, 0, 0);
    }

    private static QualityResult verdict(PeriodScore... months) {
        return QualityResult.of(List.of(months));
    }

    @Test
    @DisplayName("Все 8 метрик выполнены дают ИТОГ 100 %")
    void percent_isHundred_whenAllMetricsPassed() {
        Allure.step("Act: все 8 метрик выполнены дают ИТОГ 100 %", step -> {
            double percent = good(9).percent();
            Allure.step("Assert: иТОГ 100 %", s -> {
                assertEquals(100.0, percent, 1e-9);
            });
        });
    }

    @Test
    @DisplayName("Баланс, дающий ровно 50 %, дает ИТОГ 50 %")
    void percent_isFifty_exactly_whenBalanceIsHalf() {
        Allure.step("Act: баланс, дающий ровно 50 %, дает ИТОГ 50 %", step -> {
            double percent = exactlyFifty(9).percent();
            Allure.step("Assert: иТОГ 50 %", s -> {
                assertEquals(50.0, percent, 1e-9);
            });
        });
    }

    @Test
    @DisplayName("Все метрики провалены дают ИТОГ −100 % без ограничения снизу")
    void percent_isMinusHundred_whenAllMetricsFailed() {
        Allure.step("Act: все метрики провалены дают ИТОГ −100 % без ограничения снизу", step -> {
            double percent = low(9).percent();
            Allure.step("Assert: иТОГ −100 %", s -> {
                assertEquals(-100.0, percent, 1e-9);
            });
        });
    }

    @Test
    @DisplayName("Три месяца ниже 50 % из четырёх делают команду невыполнившей требования")
    void team_failed_whenThreeMonthsBelowFifty() {
        Allure.step("Act: три месяца ниже 50 % из четырёх делают команду невыполнившей требования", step -> {
            QualityResult verdict = verdict(low(9), low(10), low(11), good(12));
            Allure.step("Assert: команда не выполнила требования", s -> {
                assertFalse(verdict.passed());
            });
            Allure.step("Assert: месяцев ниже 50 %: 3", s -> {
                assertEquals(3, verdict.monthsBelowFifty());
            });
        });
    }

    @Test
    @DisplayName("Два месяца ниже 50 % еще допустимы")
    void team_passed_whenTwoMonthsBelowFifty() {
        Allure.step("Act: два месяца ниже 50 % еще допустимы", step -> {
            QualityResult verdict = verdict(low(9), low(10), good(11), good(12));
            Allure.step("Assert: команда выполнила требования", s -> {
                assertTrue(verdict.passed());
            });
            Allure.step("Assert: месяцев ниже 50 %: 2", s -> {
                assertEquals(2, verdict.monthsBelowFifty());
            });
        });
    }

    @Test
    @DisplayName("Все месяцы выше 50 % дают выполнение требований")
    void team_passed_whenNoMonthBelowFifty() {
        Allure.step("Act: все месяцы выше 50 % дают выполнение требований", step -> {
            QualityResult verdict = verdict(good(9), good(10));
            Allure.step("Assert: команда выполнила требования", s -> {
                assertTrue(verdict.passed());
            });
            Allure.step("Assert: месяцев ниже 50 %: 0", s -> {
                assertEquals(0, verdict.monthsBelowFifty());
            });
        });
    }

    @Test
    @DisplayName("Три месяца ровно на 50 % не считаются ниже 50 %")
    void month_exactlyFifty_isNotCountedAsBelow() {
        Allure.step("Act: три месяца ровно на 50 % не считаются ниже 50 %", step -> {
            QualityResult verdict = verdict(exactlyFifty(9), exactlyFifty(10), exactlyFifty(11));
            Allure.step("Assert: месяцев ниже 50 %: 0", s -> {
                assertEquals(0, verdict.monthsBelowFifty());
            });
            Allure.step("Assert: команда выполнила требования", s -> {
                assertTrue(verdict.passed());
            });
        });
    }
}

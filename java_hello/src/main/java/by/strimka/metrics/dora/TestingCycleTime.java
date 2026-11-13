package by.strimka.metrics.dora;

import by.strimka.metrics.MetricLevel;
import by.strimka.utils.Utils;

/**
 * Testing Cycle Time: доля тестирования в общем Lead Time, в процентах.
 */
public class TestingCycleTime {

    private static final double PRO_LIMIT = 30.0;
    private static final double OTHER_LIMIT = 50.0;

    /**
     * Процент = время тестирования / Lead Time × 100.
     * Нулевой Lead Time отсекает валидатор.
     */
    public double calculate(double leadTimeHours, double testingTimeHours) {
        return Utils.percent(testingTimeHours, leadTimeHours, "leadTime");
    }

    // Границы включительные: ровно 30 % — PRO, ровно 50 % — OTHER.

    /**
     * Уровень по доле времени тестирования в процентах.
     * Граница включительная: значение, равное порогу, относится к лучшему уровню.
     */
    public MetricLevel checkSla(double percent) {
        if (percent <= PRO_LIMIT) {
            return MetricLevel.PRO;
        }
        if (percent <= OTHER_LIMIT) {
            return MetricLevel.OTHER;
        }
        return MetricLevel.FAILED;
    }
}

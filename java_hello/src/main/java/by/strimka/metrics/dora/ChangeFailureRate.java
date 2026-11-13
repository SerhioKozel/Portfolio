package by.strimka.metrics.dora;

import by.strimka.metrics.MetricLevel;
import by.strimka.utils.Utils;

/**
 * Change Failure Rate: доля неудачных релизов в процентах.
 */
public class ChangeFailureRate {

    private static final double PRO_LIMIT = 3.0;
    private static final double OTHER_LIMIT = 10.0;

    /**
     * Процент = неудачные релизы / все релизы × 100.
     * Нулевой знаменатель отсекает валидатор; Utils.percent не даст рассчитать его молча.
     */
    public double calculate(double releases, double failures) {
        return Utils.percent(failures, releases, "releases");
    }

    // Границы включительные: ровно 3 % — PRO, ровно 10 % — OTHER.

    /**
     * Уровень по доле неудачных релизов в процентах.
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

package by.strimka.metrics.dora;

import by.strimka.metrics.MetricLevel;

/**
 * Changes Lead Time: время от изменения до продакшена. Пороги заданы в часах.
 */
public class ChangesLeadTime {

    private static final double PRO_LIMIT = 60.0;
    private static final double OTHER_LIMIT = 120.0;

    // Границы включительные: 60 ч — PRO, 120 ч — OTHER, больше — FAILED.

    /**
     * Уровень по времени от коммита до продакшена в часах.
     * Граница включительная: значение, равное порогу, относится к лучшему уровню.
     */
    public MetricLevel checkSla(double leadTimeHours) {
        if (leadTimeHours <= PRO_LIMIT) {
            return MetricLevel.PRO;
        }
        if (leadTimeHours <= OTHER_LIMIT) {
            return MetricLevel.OTHER;
        }
        return MetricLevel.FAILED;
    }
}

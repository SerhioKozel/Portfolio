package by.strimka.metrics.dora;

import by.strimka.metrics.MetricLevel;

/**
 * Deployment Rate: среднее число релизов в рабочий день.
 */
public class DeploymentRate {

    // Фиксированное число рабочих дней в месяце, а не фактический календарь периода.
    private static final double WORKING_DAYS = 20.0;
    private static final double PRO_LIMIT = 1.0;
    private static final double OTHER_LIMIT = 0.2;

    /**
     * Среднее число релизов в день: релизы месяца делятся на число рабочих дней.
     */
    public double calculate(double releases) {
        return releases / WORKING_DAYS;
    }

    // Границы разные: PRO — строго больше 1 релиза в день, OTHER — от 0,2 включительно.

    /**
     * Уровень по числу релизов в день.
     * PRO — строго больше порога, OTHER — от порога включительно.
     */
    public MetricLevel checkSla(double releasesPerDay) {
        if (releasesPerDay > PRO_LIMIT) {
            return MetricLevel.PRO;
        }
        if (releasesPerDay >= OTHER_LIMIT) {
            return MetricLevel.OTHER;
        }
        return MetricLevel.FAILED;
    }
}

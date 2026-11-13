package by.strimka.metrics.dora;

import by.strimka.metrics.MetricLevel;

/**
 * MTTR: среднее время восстановления в минутах.
 * Пороги выведены из допустимого простоя за месяц. 43 830 мин — среднее число минут
 * в месяце (365,25 дня / 12 × 24 ч × 60 мин).
 */
public class MttrCalculator {

    // 43 830 мин/мес × (100 − 99,99) / 100 = 4,383 мин.
    // Записано константой, а не вычисляется в double, чтобы граница не зависела от погрешности округления.
    private static final double PRO_LIMIT_MINUTES = 4.383;
    // 43 830 мин/мес × (100 − 95,5) / 100 = 1972,35 мин.
    private static final double OTHER_LIMIT_MINUTES = 1972.35;

    // PRO — простой не больше 0,01 % месяца (доступность 99,99 %), OTHER — не больше 4,5 %.

    /**
     * Уровень по среднему времени восстановления в минутах.
     * Граница включительная: значение, равное порогу, относится к лучшему уровню.
     */
    public MetricLevel checkSla(double meanRecoveryMinutes) {
        if (meanRecoveryMinutes <= PRO_LIMIT_MINUTES) {
            return MetricLevel.PRO;
        }
        if (meanRecoveryMinutes <= OTHER_LIMIT_MINUTES) {
            return MetricLevel.OTHER;
        }
        return MetricLevel.FAILED;
    }
}

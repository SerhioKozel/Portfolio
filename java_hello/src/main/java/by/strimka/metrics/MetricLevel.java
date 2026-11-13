package by.strimka.metrics;

/**
 * Уровень выполнения метрики. PRO и OTHER — уровни DORA, PASSED — уровень метрики качества,
 * FAILED — невыполнение в любой категории. Выполненным считается любой уровень, кроме FAILED (см. Metric.passed()).
 */
public enum MetricLevel {
    PRO,
    OTHER,
    PASSED,
    FAILED
}

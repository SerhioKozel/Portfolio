package by.strimka.metrics;

/**
 * Категория метрики. Они различаются правилами оценки:
 * DORA даёт уровни PRO / OTHER / FAILED, QUALITY — PASSED / FAILED.
 */
public enum Category {
    DORA,
    QUALITY
}

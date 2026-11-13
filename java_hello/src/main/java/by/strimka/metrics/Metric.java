package by.strimka.metrics;

import java.util.Objects;

/**
 * Результат одной метрики за один месяц. Конструктор проверяет инварианты,
 * чтобы некорректный объект нельзя было создать.
 */
public record Metric(MetricCode code, Category category, int weight, MetricLevel level, double value) {

    // Compact-конструктор: проверка инвариантов перед сохранением полей.

    /**
     * Проверяет инварианты: обязательные поля, положительный вес и отсутствие уровня PASSED у DORA-метрик.
     */
    public Metric {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(level, "level");
        if (weight <= 0) {
            throw new IllegalArgumentException("Вес метрики должен быть положительным: " + weight);
        }
        if (category == Category.DORA && level == MetricLevel.PASSED) {
            throw new IllegalArgumentException(
                    "DORA-метрика не может иметь уровень PASSED (метрика " + code + ")");
        }
    }

    /**
     * Метрика выполнена, если она не FAILED. Поэтому PRO, OTHER и PASSED засчитываются одинаково.
     */
    public boolean passed() {
        return level != MetricLevel.FAILED;
    }
}

package by.strimka.metrics.quality;

import by.strimka.metrics.MetricLevel;
import by.strimka.utils.Utils;

/**
 * Базовый класс метрик качества. Общий алгоритм (проверка знаменателя, процент, сравнение
 * с порогом) находится здесь. Наследники задают только числитель, знаменатель, название и порог.
 */
public abstract class QualityMetric {

    // Числитель и знаменатель в одних единицах, процент считает value().

    /**
     * Числитель. Единицы совпадают со знаменателем, процент считает {@link #value()}.
     */
    protected abstract double numerator();

    /**
     * Знаменатель. Ноль здесь недопустим: валидатор входа должен его отсечь.
     */
    protected abstract double denominator();

    /**
     * Название знаменателя для сообщения об ошибке.
     */
    protected abstract String denominatorName();

    /**
     * Порог в процентах. Метрика выполнена, если значение строго меньше порога.
     */
    protected abstract double getSla();

    /**
     * Процент = числитель / знаменатель × 100.
     * Нулевой знаменатель — внутренняя ошибка: валидатор должен был его отсечь.
     * Исключение лучше молчаливого NaN, который прошёл бы в отчёт незамеченным.
     */
    public final double value() {
        return Utils.percent(numerator(), denominator(), denominatorName());
    }

    // Сравнение строгое: значение, равное порогу, не выполняет требование.
    // Пороги DORA задаются в калькуляторах dora/*, и их границы не всегда включительные.

    /**
     * Уровень метрики: PASSED, если значение строго меньше порога, иначе FAILED.
     */
    public final MetricLevel level() {
        return value() < getSla() ? MetricLevel.PASSED : MetricLevel.FAILED;
    }
}

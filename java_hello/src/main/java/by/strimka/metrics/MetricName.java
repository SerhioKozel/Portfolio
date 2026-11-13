package by.strimka.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Человекочитаемое название метрики. Читается рефлексией во время вывода отчёта,
 * поэтому хранится до выполнения (RUNTIME).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MetricName {

    /**
     * Название метрики для отчёта.
     */
    String value();
}

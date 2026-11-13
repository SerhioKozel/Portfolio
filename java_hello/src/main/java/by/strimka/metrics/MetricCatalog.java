package by.strimka.metrics;

import java.util.List;

/**
 * Каталог метрик: код, категория и вес. Веса задаются только здесь.
 * Метрики качества весят по 1, метрики DORA — по 2. Вес участвует в формуле ИТОГ (PeriodScore).
 */
public final class MetricCatalog {

    // Список описывает все метрики. Порядок расчёта задаёт TeamEvaluator.
    private static final List<Definition> DEFINITIONS = List.of(
            new Definition(MetricCode.PROD_BUGS_SHARE, Category.QUALITY, 1),
            new Definition(MetricCode.TEST_BUGS_SHARE, Category.QUALITY, 1),
            new Definition(MetricCode.PROD_BUGS_LEAK, Category.QUALITY, 1),
            new Definition(MetricCode.TESTING_CYCLE_TIME, Category.QUALITY, 1),
            new Definition(MetricCode.CHANGES_LEAD_TIME, Category.DORA, 2),
            new Definition(MetricCode.CHANGE_FAILURE_RATE, Category.DORA, 2),
            new Definition(MetricCode.DEPLOYMENT_RATE, Category.DORA, 2),
            new Definition(MetricCode.MTTR, Category.DORA, 2));

    /**
     * Описание одной метрики.
     */
    public record Definition(MetricCode code, Category category, int weight) {
    }

    // Утилитный класс: только статические данные и методы.
    private MetricCatalog() {
    }

    /**
     * Все описания метрик в порядке каталога.
     */
    public static List<Definition> all() {
        return DEFINITIONS;
    }

    /**
     * Возвращает описание метрики.
     * Бросает исключение, если метрики нет в каталоге.
     */
    public static Definition get(MetricCode code) {
        for (Definition definition : DEFINITIONS) {
            if (definition.code() == code) {
                return definition;
            }
        }
        throw new IllegalArgumentException("Метрика не описана в каталоге: " + code);
    }

    /**
     * Суммарный вес категории. Используется как знаменатель в формуле ИТОГ.
     */
    public static int totalWeight(Category category) {
        int total = 0;
        for (Definition definition : DEFINITIONS) {
            if (definition.category() == category) {
                total += definition.weight();
            }
        }
        return total;
    }
}

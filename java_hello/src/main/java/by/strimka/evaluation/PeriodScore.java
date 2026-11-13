package by.strimka.evaluation;

import by.strimka.metrics.Category;
import by.strimka.metrics.Metric;
import by.strimka.metrics.MetricCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * Результат одного месяца команды. Компоненты А, Б, С, Д, Е, Ж хранятся явно,
 * чтобы формулу ИТОГ можно было проверить без повторного разбора входа.
 */
public record PeriodScore(
        int year,
        int month,
        ArrayList<Metric> metrics,
        int doraPassed,
        int qualityPassed,
        int doraFailed,
        int qualityFailed,
        int doraTotal,
        int qualityTotal,
        double percent) {

    // Compact-конструктор: копия списка защищает от изменения извне.

    /**
     * Копирует список метрик, чтобы результат месяца не менялся извне.
     */
    public PeriodScore {
        metrics = new ArrayList<>(metrics);
    }

    /**
     * Суммирует веса выполненных и невыполненных метрик по категориям.
     */
    public static PeriodScore of(int year, int month, List<Metric> metrics) {
        int doraPassed = 0;
        int qualityPassed = 0;
        int doraFailed = 0;
        int qualityFailed = 0;

        for (Metric metric : metrics) {
            if (metric.category() == Category.DORA) {
                if (metric.passed()) {
                    doraPassed += metric.weight();
                } else {
                    doraFailed += metric.weight();
                }
            } else {
                if (metric.passed()) {
                    qualityPassed += metric.weight();
                } else {
                    qualityFailed += metric.weight();
                }
            }
        }

        int doraTotal = MetricCatalog.totalWeight(Category.DORA);
        int qualityTotal = MetricCatalog.totalWeight(Category.QUALITY);
        // Формула ИТОГ из задания: (выполнено − не выполнено) / суммарный вес × 100.
        // Результат может быть отрицательным, если невыполнено больше половины веса.
        double percent = (doraPassed + qualityPassed - doraFailed - qualityFailed) * 100.0
                / (doraTotal + qualityTotal);

        return new PeriodScore(year, month, new ArrayList<>(metrics), doraPassed, qualityPassed,
                doraFailed, qualityFailed, doraTotal, qualityTotal, percent);
    }
}

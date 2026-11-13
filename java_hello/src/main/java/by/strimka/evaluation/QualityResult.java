package by.strimka.evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Итог по команде за все месяцы: месяцы, число месяцев ниже порога и итоговый вердикт.
 * Команда выполнила требования, если месяцев ниже порога не больше двух.
 */
public record QualityResult(ArrayList<PeriodScore> months, long monthsBelowFifty, boolean passed) {

    // Месяц ниже этого процента считается неудачным.
    private static final double THRESHOLD_PERCENT = 50.0;
    // Сколько неудачных месяцев допускается.
    private static final int ALLOWED_MONTHS_BELOW = 2;

    // Compact-конструктор: копия списка месяцев защищает от изменения извне.

    /**
     * Копирует список месяцев, чтобы результат команды не менялся извне.
     */
    public QualityResult {
        months = new ArrayList<>(months);
    }

    /**
     * Считает итог по команде: сколько месяцев ниже порога и выполнила ли команда требования.
     */
    public static QualityResult of(List<PeriodScore> months) {
        // Считаем строго ниже порога: месяц ровно на 50 % неудачным не считается.
        long below = months.stream()
                .filter(month -> month.percent() < THRESHOLD_PERCENT)
                .count();
        return new QualityResult(new ArrayList<>(months), below, below <= ALLOWED_MONTHS_BELOW);
    }
}

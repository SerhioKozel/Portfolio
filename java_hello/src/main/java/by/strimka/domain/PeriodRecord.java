package by.strimka.domain;

/**
 * Проверенная запись одного месяца одной команды.
 * Создаётся только после валидации: поля, которые обязательны во входе, имеют примитивные типы.
 */
public record PeriodRecord(
        int year,
        int month,
        String teamName,
        String purpose,
        Criticality criticality,
        int prodBugs,
        int closedTasks,
        int testBugs,
        double testingTime,
        double leadTime,
        double changesLeadTime,
        int releases,
        int failures,
        double mttr) {
}

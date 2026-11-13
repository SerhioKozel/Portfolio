package by.strimka.metrics;

/**
 * Коды метрик. Имена попадают в JSON-отчёт через name(), поэтому переименование
 * константы меняет формат отчёта. Название для вывода задаётся аннотацией @MetricName.
 */
public enum MetricCode {

    @MetricName("Доля багов Прода")
    PROD_BUGS_SHARE,

    @MetricName("Доля багов из тестирования")
    TEST_BUGS_SHARE,

    @MetricName("Протекание дефектов на прод")
    PROD_BUGS_LEAK,

    @MetricName("Testing Cycle Time")
    TESTING_CYCLE_TIME,

    @MetricName("Changes Lead Time")
    CHANGES_LEAD_TIME,

    @MetricName("Change Failure Rate")
    CHANGE_FAILURE_RATE,

    @MetricName("Deployment Rate")
    DEPLOYMENT_RATE,

    @MetricName("MTTR")
    MTTR;

    /**
     * Название для вывода. Читается рефлексией из аннотации поля константы.
     */
    public String title() {
        try {
            return MetricCode.class.getField(name()).getAnnotation(MetricName.class).value();
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Нет поля для константы " + name(), exception);
        }
    }
}

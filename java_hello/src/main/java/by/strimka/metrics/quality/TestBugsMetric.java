package by.strimka.metrics.quality;

/**
 * Доля багов из тестирования: баги из тестирования / закрытые задачи, в процентах.
 */
public class TestBugsMetric extends QualityMetric {

    // Порог в процентах: выполнено, если меньше.
    public static final double TEST_BUGS_SLA = 80.0;

    private final double testBugs;
    private final double closedTasks;

    /**
     * Создаёт метрику багов тестирования.
     * testBugs — баги, найденные в тестировании; closedTasks — закрытые задачи.
     */
    public TestBugsMetric(double testBugs, double closedTasks) {
        this.testBugs = testBugs;
        this.closedTasks = closedTasks;
    }

    @Override
    protected double numerator() {
        return testBugs;
    }

    @Override
    protected double denominator() {
        return closedTasks;
    }

    @Override
    protected String denominatorName() {
        return "количество закрытых задач";
    }

    @Override
    protected double getSla() {
        return TEST_BUGS_SLA;
    }
}

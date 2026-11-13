package by.strimka.metrics.quality;

/**
 * Доля багов Прода: баги на проде / закрытые задачи, в процентах.
 */
public class ProdBugsMetric extends QualityMetric {

    // Порог в процентах: выполнено, если меньше.
    public static final double PROD_BUGS_SLA = 10.0;

    private final double prodBugs;
    private final double closedTasks;

    /**
     * Создаёт метрику багов прода.
     * prodBugs — баги, найденные на проде; closedTasks — закрытые задачи.
     */
    public ProdBugsMetric(double prodBugs, double closedTasks) {
        this.prodBugs = prodBugs;
        this.closedTasks = closedTasks;
    }

    @Override
    protected double numerator() {
        return prodBugs;
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
        return PROD_BUGS_SLA;
    }
}

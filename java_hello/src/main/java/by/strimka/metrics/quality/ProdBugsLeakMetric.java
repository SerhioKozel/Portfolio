package by.strimka.metrics.quality;

/**
 * Протекание дефектов на прод: доля багов, найденных на проде, среди всех багов периода
 * (прод + тестирование), в процентах.
 */
public class ProdBugsLeakMetric extends QualityMetric {

    // Порог в процентах: выполнено, если меньше.
    public static final double PROD_BUGS_LEAK_SLA = 50.0;

    private final double prodBugs;
    private final double testBugs;

    /**
     * Создаёт метрику протекания дефектов.
     * prodBugs — баги, найденные на проде; testBugs — баги, найденные в тестировании.
     */
    public ProdBugsLeakMetric(double prodBugs, double testBugs) {
        this.prodBugs = prodBugs;
        this.testBugs = testBugs;
    }

    @Override
    protected double numerator() {
        return prodBugs;
    }

    @Override
    protected double denominator() {
        return prodBugs + testBugs;
    }

    @Override
    protected String denominatorName() {
        return "общее количество багов";
    }

    @Override
    protected double getSla() {
        return PROD_BUGS_LEAK_SLA;
    }
}

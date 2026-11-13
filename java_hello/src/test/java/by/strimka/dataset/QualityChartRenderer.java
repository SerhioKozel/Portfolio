package by.strimka.dataset;

import by.strimka.evaluation.PeriodScore;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Рисует график соответствия качества одной команды за год в PNG.
 *
 * <p>Ось Y — от −100 до 120 %: процент может уйти в минус (штраф за провалы), а 100 % должен быть виден.
 * Пунктирная линия показывает порог 50 %, ниже которого команда не проходит.
 * JFreeChart рисует без экрана: для сохранения PNG нужен только {@code ImageIO}, окно не открывается.
 */
public final class QualityChartRenderer {

    static final int WIDTH = 900;
    static final int HEIGHT = 500;
    static final double THRESHOLD_PERCENT = 50.0;
    // Верхняя граница больше 100: команда с 100 % иначе ляжет на край графика и будет не видна.
    static final int RANGE_MIN = -100;
    static final int RANGE_MAX = 120;
    private static final int QUALITY_SERIES = 0;
    private static final int FIRST_MONTH = 1;
    private static final int LAST_MONTH = 12;
    private static final int THRESHOLD_SERIES = 1;

    private QualityChartRenderer() {
    }

    /**
     * Сохраняет график команды в PNG.
     *
     * @param teamName название команды, попадает в заголовок
     * @param months   месячные результаты команды
     * @param file     путь к PNG; родительская папка должна существовать
     */
    public static void render(String teamName, List<PeriodScore> months, Path file) throws IOException {
        XYSeries quality = new XYSeries("Соответствие");
        for (PeriodScore score : months) {
            quality.add(score.month(), score.percent());
        }
        XYSeries threshold = new XYSeries("Порог 50 %");
        threshold.add(FIRST_MONTH, THRESHOLD_PERCENT);
        threshold.add(LAST_MONTH, THRESHOLD_PERCENT);

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(quality);
        dataset.addSeries(threshold);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Соответствие качества: " + teamName, "Месяц", "Соответствие, %", dataset);

        XYPlot plot = chart.getXYPlot();
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setRange(FIRST_MONTH, LAST_MONTH);
        domainAxis.setTickUnit(new NumberTickUnit(1));
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(RANGE_MIN, RANGE_MAX);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        // Точки на каждом месяце: плоская линия на одном уровне читается, даже если совпадает с другими.
        renderer.setSeriesShapesVisible(QUALITY_SERIES, true);
        float[] dash = {6f, 4f};
        renderer.setSeriesStroke(THRESHOLD_SERIES,
                new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, dash, 0f));
        renderer.setSeriesPaint(THRESHOLD_SERIES, Color.GRAY);
        renderer.setSeriesShapesVisible(THRESHOLD_SERIES, false);

        ChartUtils.saveChartAsPNG(file.toFile(), chart, WIDTH, HEIGHT);
    }
}

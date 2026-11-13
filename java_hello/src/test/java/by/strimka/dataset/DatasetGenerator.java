package by.strimka.dataset;

import java.util.Random;

/**
 * Генератор CSV на один год (12 месяцев) для 5–8 команд. Живёт в src/test и не попадает в продакшен.
 *
 * <p>У каждой команды свой {@link Random} с сидом {@code seed + индекс}. Так результат не зависит от порядка
 * вызовов: команду можно генерировать отдельно или параллельно, и данные не изменятся. Один общий
 * {@code Random} на все команды этого бы не гарантировал.
 *
 * <p>Все значения целые и в допустимых диапазонах, поэтому строки проходят {@code InputValidator}.
 */
public final class DatasetGenerator {

    static final String HEADER = "month;year;teamName;purpose;criticality;prodBugs;closedTasks;testBugs;"
            + "testingTime;leadTime;changesLeadTime;releases;failures;mttr";
    static final int YEAR = 2026;
    static final int MIN_TEAMS = 5;
    static final int MAX_TEAMS = 8;

    private static final String[] CRITICALITIES = {"BestEfforts", "BusinessCritical", "MissionCritical"};
    private static final double MIN_QUALITY = 0.4;
    private static final double QUALITY_SPREAD = 0.55;

    private DatasetGenerator() {
    }

    /**
     * Возвращает CSV-текст: заголовок и по 12 строк на каждую команду.
     *
     * @param seed      базовый сид; одинаковый сид даёт одинаковый текст
     * @param teamCount количество команд от 5 до 8
     */
    public static String generate(long seed, int teamCount) {
        if (teamCount < MIN_TEAMS || teamCount > MAX_TEAMS) {
            throw new IllegalArgumentException(
                    "команд должно быть от " + MIN_TEAMS + " до " + MAX_TEAMS + ", получено " + teamCount);
        }
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (int team = 0; team < teamCount; team++) {
            Random random = new Random(seed + team);
            // Доля «хороших» процессов команды: от 0.4 до 0.95. Чем ниже, тем больше багов и провалов.
            double quality = MIN_QUALITY + random.nextDouble() * QUALITY_SPREAD;
            String name = String.format("Team-%02d", team + 1);
            String criticality = CRITICALITIES[team % CRITICALITIES.length];
            for (int month = 1; month <= 12; month++) {
                appendRow(csv, month, name, criticality, quality, random);
            }
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, int month, String name, String criticality,
                                  double quality, Random random) {
        double defects = 1.0 - quality;
        int closedTasks = 100 + random.nextInt(51);
        int testBugs = (int) Math.round(closedTasks * defects * 0.5 * (0.5 + random.nextDouble()));
        int prodBugs = (int) Math.round(closedTasks * defects * 0.1 * (0.5 + random.nextDouble()));
        int testingTime = 100 + random.nextInt(201);
        int leadTime = 500 + random.nextInt(1001);
        int changesLeadTime = 20 + (int) Math.round(200 * defects * random.nextDouble());
        int releases = 10 + random.nextInt(31);
        int failures = (int) Math.round(releases * defects * 0.5 * random.nextDouble());
        int mttr = 10 + (int) Math.round(100 * defects * random.nextDouble());

        csv.append(month).append(';')
                .append(YEAR).append(';')
                .append(name).append(';')
                .append("Тест").append(';')
                .append(criticality).append(';')
                .append(prodBugs).append(';')
                .append(closedTasks).append(';')
                .append(testBugs).append(';')
                .append(testingTime).append(';')
                .append(leadTime).append(';')
                .append(changesLeadTime).append(';')
                .append(releases).append(';')
                .append(failures).append(';')
                .append(mttr).append('\n');
    }
}

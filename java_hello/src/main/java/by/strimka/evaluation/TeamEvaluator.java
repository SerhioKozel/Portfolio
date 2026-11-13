package by.strimka.evaluation;

import by.strimka.domain.PeriodRecord;
import by.strimka.domain.Team;
import by.strimka.metrics.Metric;
import by.strimka.metrics.MetricCatalog;
import by.strimka.metrics.MetricCode;
import by.strimka.metrics.MetricLevel;
import by.strimka.metrics.dora.ChangeFailureRate;
import by.strimka.metrics.dora.ChangesLeadTime;
import by.strimka.metrics.dora.DeploymentRate;
import by.strimka.metrics.dora.MttrCalculator;
import by.strimka.metrics.dora.TestingCycleTime;
import by.strimka.metrics.quality.ProdBugsLeakMetric;
import by.strimka.metrics.quality.ProdBugsMetric;
import by.strimka.metrics.quality.QualityMetric;
import by.strimka.metrics.quality.TestBugsMetric;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Оценка команд: группировка записей по команде, расчёт каждого месяца, итоговый результат.
 */
public final class TeamEvaluator {

    // Таймаут ожидания результата одной команды. Это ограничение ожидания, а не остановка работы:
    // задача не прерывается принудительно, см. evaluateAll(List).
    private static final long TEAM_TIMEOUT_SECONDS = 30;

    private TeamEvaluator() {
    }

    /**
     * Считает команды в пуле, который создаёт и завершает сам метод.
     * Пул создаётся явно, а не через try-with-resources: close() ждал бы все задачи,
     * и таймаут ожидания ничего бы не давал. Поэтому завершение делается в finally.
     */
    public static List<QualityEntry> evaluateAll(List<PeriodRecord> records) {
        List<List<PeriodRecord>> teams = groupByTeam(records);
        // Пустой вход: пул не создаём, иначе он создался бы без задач.
        if (teams.isEmpty()) {
            return new ArrayList<>();
        }
        // Задачи вычислительные (CPU-bound), поэтому размер пула равен числу ядер, но не больше числа команд.
        // Виртуальные потоки здесь не нужны: они выгодны для большого числа блокирующих задач.
        int poolSize = Math.max(1, Math.min(teams.size(), Runtime.getRuntime().availableProcessors()));
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            return evaluateTeams(teams, pool);
        } finally {
            // shutdownNow прерывает оставшиеся задачи и не ждёт их. Расчёт не проверяет флаг прерывания,
            // поэтому остановка не гарантирована, но вызов не блокирует выход из метода.
            pool.shutdownNow();
        }
    }

    /**
     * Считает команды в переданном исполнителе. Владелец исполнителя сам отвечает за его завершение:
     * метод его не закрывает и не останавливает.
     */
    public static List<QualityEntry> evaluateAll(List<PeriodRecord> records, ExecutorService executor) {
        return evaluateTeams(groupByTeam(records), executor);
    }

    // Группирует записи по команде. LinkedHashMap сохраняет порядок первого появления команды во входе,
    // поэтому отчёт идёт в том же порядке, что и CSV.
    private static List<List<PeriodRecord>> groupByTeam(List<PeriodRecord> records) {
        Map<String, List<PeriodRecord>> byTeam = new LinkedHashMap<>();
        for (PeriodRecord record : records) {
            byTeam.computeIfAbsent(record.teamName(), name -> new ArrayList<>()).add(record);
        }
        return new ArrayList<>(byTeam.values());
    }

    // Отправляет расчёт каждой команды в исполнитель и собирает результаты в порядке команд.
    // Future собираются в порядке появления команд во входе, поэтому порядок не зависит от скорости задач.
    private static List<QualityEntry> evaluateTeams(List<List<PeriodRecord>> teams, ExecutorService executor) {
        try {
            List<Future<QualityEntry>> futures = new ArrayList<>();
            for (List<PeriodRecord> teamRecords : teams) {
                futures.add(executor.submit(() -> evaluateTeam(teamRecords)));
            }
            List<QualityEntry> entries = new ArrayList<>();
            for (Future<QualityEntry> future : futures) {
                entries.add(future.get(TEAM_TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return entries;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("расчёт команд прерван", exception);
        } catch (ExecutionException exception) {
            // Ошибка внутри задачи приходит обёрнутой: отдаём исходное непроверяемое исключение, если оно есть.
            if (exception.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("расчёт команды завершился с ошибкой", exception);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("расчёт команды не уложился в таймаут", exception);
        }
    }

    /**
     * Считает итог по одной команде.
     * Записи сортируются по году и месяцу, потому что вход может идти в любом порядке.
     */
    public static QualityEntry evaluateTeam(List<PeriodRecord> teamRecords) {
        // Вход может идти в произвольном порядке, поэтому месяцы сортируются по году и месяцу.
        List<PeriodRecord> sorted = new ArrayList<>(teamRecords);
        sorted.sort(Comparator.comparingInt(PeriodRecord::year)
                .thenComparingInt(PeriodRecord::month));

        List<PeriodScore> months = new ArrayList<>();
        for (PeriodRecord record : sorted) {
            months.add(evaluatePeriod(record));
        }
        // Команда берётся из первой записи. Валидатор проверяет, что у всех месяцев
        // название, назначение и критичность совпадают.
        PeriodRecord first = sorted.get(0);
        Team team = new Team(first.teamName(), first.purpose(), first.criticality());
        return new QualityEntry(team, QualityResult.of(months));
    }

    /**
     * Считает все метрики одного месяца. Порядок добавления задаёт порядок метрик в отчёте.
     */
    public static PeriodScore evaluatePeriod(PeriodRecord record) {
        List<Metric> metrics = new ArrayList<>();

        // Метрики качества: доли считаются от закрытых задач, протекание — от всех багов периода.
        metrics.add(quality(MetricCode.PROD_BUGS_SHARE,
                new ProdBugsMetric(record.prodBugs(), record.closedTasks())));
        metrics.add(quality(MetricCode.TEST_BUGS_SHARE,
                new TestBugsMetric(record.testBugs(), record.closedTasks())));
        metrics.add(quality(MetricCode.PROD_BUGS_LEAK,
                new ProdBugsLeakMetric(record.prodBugs(), record.testBugs())));

        // Метрики DORA: сначала считаем значение, затем проверяем его по порогу.
        // Значение сохраняется в результате, чтобы попасть в отчёт.
        TestingCycleTime testingCycleTime = new TestingCycleTime();
        double testingPercent = testingCycleTime.calculate(record.leadTime(), record.testingTime());
        metrics.add(result(MetricCode.TESTING_CYCLE_TIME,
                testingCycleTime.checkSla(testingPercent), testingPercent));

        ChangesLeadTime changesLeadTime = new ChangesLeadTime();
        metrics.add(result(MetricCode.CHANGES_LEAD_TIME,
                changesLeadTime.checkSla(record.changesLeadTime()), record.changesLeadTime()));

        ChangeFailureRate changeFailureRate = new ChangeFailureRate();
        double failurePercent = changeFailureRate.calculate(record.releases(), record.failures());
        metrics.add(result(MetricCode.CHANGE_FAILURE_RATE,
                changeFailureRate.checkSla(failurePercent), failurePercent));

        DeploymentRate deploymentRate = new DeploymentRate();
        double releasesPerDay = deploymentRate.calculate(record.releases());
        metrics.add(result(MetricCode.DEPLOYMENT_RATE,
                deploymentRate.checkSla(releasesPerDay), releasesPerDay));

        MttrCalculator mttr = new MttrCalculator();
        metrics.add(result(MetricCode.MTTR, mttr.checkSla(record.mttr()), record.mttr()));

        return PeriodScore.of(record.year(), record.month(), metrics);
    }

    // Уровень метрики качества уже известен из QualityMetric.level().
    private static Metric quality(MetricCode code, QualityMetric metric) {
        return result(code, metric.level(), metric.value());
    }

    // Категория и вес берутся из каталога, поэтому калькуляторы не знают про веса.
    private static Metric result(MetricCode code, MetricLevel level, double value) {
        MetricCatalog.Definition definition = MetricCatalog.get(code);
        return new Metric(code, definition.category(), definition.weight(), level, value);
    }
}

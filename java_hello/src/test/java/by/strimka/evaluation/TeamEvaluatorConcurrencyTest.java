package by.strimka.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import by.strimka.domain.PeriodRecord;
import by.strimka.input.CsvReadResult;
import by.strimka.input.CsvReader;
import by.strimka.validation.InputValidator;
import by.strimka.validation.ValidationResult;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Проверяет, что расчёт не зависит от потока, в котором он выполняется.
 * Эталон считается однопоточно. Потоки стартуют одновременно через CountDownLatch,
 * иначе они могут отработать по очереди и гонки не будет.
 * Тест показывает стабильность результата на этих данных. Он не доказывает потокобезопасность в общем случае.
 */
@Tag("component")
@Epic("Калькулятор качества")
@Feature("Расчёт вердикта")
class TeamEvaluatorConcurrencyTest {

    private static final int THREADS = 8;
    private static final int CALLS_PER_THREAD = 4;
    private static final long TIMEOUT_SECONDS = 30;

    @Test
    @DisplayName("Параллельные вызовы evaluateAll дают тот же результат, что однопоточный расчёт")
    void evaluateAll_returnsSameResult_fromManyThreads() {
        List<PeriodRecord> records = loadRecords();
        List<QualityEntry> expected = TeamEvaluator.evaluateAll(records);
        Allure.step("Act: параллельные вызовы evaluateAll стартуют одновременно", step -> {
            List<List<QualityEntry>> results = evaluateConcurrently(records);
            Allure.step("Assert: каждый результат равен однопоточному эталону", s -> {
                for (List<QualityEntry> result : results) {
                    assertEquals(expected, result);
                }
            });
        });
    }

    // Возвращает результаты всех параллельных вызовов. Исключения оборачиваем, потому что лямбды Allure.step
    // не объявляют checked-исключений.
    private static List<List<QualityEntry>> evaluateConcurrently(List<PeriodRecord> records) {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<List<QualityEntry>>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS * CALLS_PER_THREAD; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return TeamEvaluator.evaluateAll(records);
                }));
            }
            // Все задачи уже в очереди: отпускаем их одновременно.
            startGate.countDown();
            List<List<QualityEntry>> results = new ArrayList<>();
            for (Future<List<QualityEntry>> future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return results;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("тест прерван", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("параллельный расчёт не завершился", exception);
        } finally {
            pool.shutdown();
        }
    }

    // Два варианта в одной выборке: команда с PASSED и команда с FAILED, поэтому эталон содержит оба.
    private static List<PeriodRecord> loadRecords() {
        try {
            // Путь относительно корня проекта: Maven запускает тесты оттуда.
            Path samplePath = Path.of("samples/valid/all-metrics-passed.csv");
            assertTrue(Files.exists(samplePath),
                    "нет файла " + samplePath.toAbsolutePath() + ": тест должен запускаться из корня проекта");
            String sample = Files.readString(samplePath).strip();
            String bad = "9;2026;Bad;Тест;BestEfforts;50;100;90;900;1000;200;10;5;3000\n"
                    + "10;2026;Bad;Тест;BestEfforts;50;100;90;900;1000;200;10;5;3000\n"
                    + "11;2026;Bad;Тест;BestEfforts;50;100;90;900;1000;200;10;5;3000";
            CsvReadResult read = CsvReader.read(new StringReader(sample + "\n" + bad));
            ValidationResult validation = InputValidator.validate(read);
            assertTrue(validation.errors().isEmpty(), "входные данные для теста должны быть валидны");
            return List.copyOf(validation.records());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

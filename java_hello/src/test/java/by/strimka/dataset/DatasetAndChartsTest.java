package by.strimka.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import by.strimka.evaluation.QualityEntry;
import by.strimka.evaluation.TeamEvaluator;
import by.strimka.input.CsvReadResult;
import by.strimka.input.CsvReader;
import by.strimka.validation.InputValidator;
import by.strimka.validation.ValidationResult;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Проверяет генератор и графики: сгенерированные данные проходят тот же пайплайн, что и реальный CSV,
 * а для каждой команды появляется свой PNG. Файлы кладутся в target/, чтобы их можно было открыть после прогона.
 */
@Tag("component")
@Epic("Калькулятор качества")
@Feature("Датасет и графики")
class DatasetAndChartsTest {

    private static final long SEED = 42L;
    private static final Path DATASET_DIR = Path.of("target", "dataset");
    private static final Path CHARTS_DIR = Path.of("target", "charts");
    private static final long TIMEOUT_SECONDS = 30;

    @ParameterizedTest(name = "{0} команд: год проходит валидацию")
    @ValueSource(ints = {5, 8})
    @DisplayName("Сгенерированный год проходит валидацию на границах 5 и 8 команд")
    void generate_producesValidYear_forBoundaryTeamCounts(int teamCount) {
        String csv = DatasetGenerator.generate(SEED, teamCount);
        Allure.step("Act: разбор и валидация сгенерированного CSV", step -> {
            ValidationResult validation = validate(csv);
            Allure.step("Assert: ошибок нет, по 12 месяцев на команду", s -> {
                assertTrue(validation.errors().isEmpty(), "ошибки валидации: " + validation.errors());
                assertEquals(teamCount * 12, validation.records().size());
            });
        });
    }

    @Test
    @DisplayName("Одинаковый сид даёт одинаковые данные")
    void generate_isDeterministic_forSameSeed() {
        Allure.step("Act: генерация дважды с одним сидом", step -> {
            String first = DatasetGenerator.generate(SEED, 6);
            String second = DatasetGenerator.generate(SEED, 6);
            Allure.step("Assert: тексты совпадают", s -> {
                assertEquals(first, second);
            });
        });
    }

    @Test
    @DisplayName("Для каждой команды создаётся свой PNG-график")
    void render_writesOnePngPerTeam() throws IOException {
        int teamCount = 6;
        String csv = DatasetGenerator.generate(SEED, teamCount);
        Files.createDirectories(DATASET_DIR);
        Files.writeString(DATASET_DIR.resolve("quality-2026.csv"), csv);
        Files.createDirectories(CHARTS_DIR);

        List<QualityEntry> entries = TeamEvaluator.evaluateAll(validate(csv).records());
        renderConcurrently(entries);
        for (QualityEntry entry : entries) {
            attachChart(CHARTS_DIR.resolve(entry.team().name() + ".png"));
        }

        Allure.step("Assert: по одному непустому PNG на каждую команду", s -> {
            assertEquals(teamCount, entries.size());
            for (QualityEntry entry : entries) {
                Path png = CHARTS_DIR.resolve(entry.team().name() + ".png");
                assertTrue(Files.isRegularFile(png), "нет файла " + png);
                assertTrue(sizeOf(png) > 0, "пустой файл " + png);
            }
        });
    }

    // Каждый график рисуется в своей задаче пула. Файлы у команд разные, поэтому задачи не пересекаются.
    // Future.get с таймаутом не даёт тесту зависнуть, если отрисовка застрянет.
    // Пул закрывается в try-with-resources: close() ждёт завершения задач.
    private static void renderConcurrently(List<QualityEntry> entries) {
        int poolSize = Math.max(1, Math.min(entries.size(), Runtime.getRuntime().availableProcessors()));
        try (ExecutorService pool = Executors.newFixedThreadPool(poolSize)) {
            List<Future<Path>> futures = new ArrayList<>();
            for (QualityEntry entry : entries) {
                futures.add(pool.submit(() -> {
                    String name = entry.team().name();
                    Path png = CHARTS_DIR.resolve(name + ".png");
                    QualityChartRenderer.render(name, entry.result().months(), png);
                    return png;
                }));
            }
            for (Future<Path> future : futures) {
                future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("отрисовка прервана", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof IOException io) {
                throw new UncheckedIOException(io);
            }
            throw new IllegalStateException("отрисовка графика не удалась", exception);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("отрисовка графика не уложилась в таймаут", exception);
        }
    }

    // Прикрепляем PNG к результату теста: график виден в Allure-отчёте рядом с проверкой.
    // Allure читает вложения из target/allure-results, а этот каталог уже уходит в CI-артефакты и на Pages.
    private static void attachChart(Path png) {
        try (InputStream content = Files.newInputStream(png)) {
            Allure.addAttachment(png.getFileName().toString(), "image/png", content, "png");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static ValidationResult validate(String csv) {
        try {
            CsvReadResult read = CsvReader.read(new StringReader(csv));
            return InputValidator.validate(read);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

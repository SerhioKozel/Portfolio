package by.strimka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Сквозные тесты на образцах: проверяем код возврата, наличие отчёта и ключевые поля JSON.
 * Файлы копируются во временную папку, чтобы отчёт не появлялся в samples/.
 */
@Tag("e2e")
@Epic("Калькулятор качества")
@Feature("Запуск приложения")
class SampleRunTest {

    @TempDir
    Path tempDir;

    // Лямбды Allure.step не объявляют checked-исключений, поэтому IOException оборачиваем здесь.
    private Path copySample(String relative) {
        Path target = tempDir.resolve(Path.of(relative).getFileName());
        try {
            Files.copy(Path.of(relative), target);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return target;
    }

    private JsonNode readReport(String name) {
        try {
            return new ObjectMapper().readTree(tempDir.resolve(name).toFile());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Test
    @DisplayName("Образец без ошибок: отчёт содержит команду, вердикт и ИТОГ 100 %")
    void run_writesExpectedFields_forPaymentsSample() {
        Allure.step("Act: образец без ошибок: отчёт содержит команду, вердикт и ИТОГ 100 %", step -> {
            Path input = copySample("samples/valid/all-metrics-passed.csv");
            int exitCode = App.run(new String[] {input.toString()});
            JsonNode report = readReport("all-metrics-passed-report.json");
            Allure.step("Assert: код возврата 0", s -> {
                assertEquals(0, exitCode);
            });
            Allure.step("Assert: команда Payments с вердиктом PASSED", s -> {
                assertEquals("Payments", report.at("/entries/0/team/name").asText());
                assertEquals("PASSED", report.at("/entries/0/result/verdict").asText());
            });
            Allure.step("Assert: иТОГ 100 %, 8 метрик, компоненты DORA 8 и quality 4", s -> {
                assertEquals(100.0, report.at("/entries/0/result/months/0/percent").asDouble());
                assertEquals(8, report.at("/entries/0/result/months/0/metrics").size());
                assertEquals(8, report.at("/entries/0/result/months/0/components/total/dora").asInt());
                assertEquals(4, report.at("/entries/0/result/months/0/components/total/quality").asInt());
            });
        });
    }
}

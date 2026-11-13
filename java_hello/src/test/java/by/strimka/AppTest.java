package by.strimka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("e2e")
@Epic("Калькулятор качества")
@Feature("Запуск приложения")
class AppTest {

    @TempDir
    Path tempDir;

    private static final String HEADER = "month;year;teamName;purpose;criticality;prodBugs;closedTasks;testBugs;"
            + "testingTime;leadTime;changesLeadTime;releases;failures;mttr";
    private static final String VALID_ROW = "9;2026;Payments;Платежи;BestEfforts;5;100;30;200;1000;50;40;1;30";

    // Лямбды Allure.step не объявляют checked-исключений, поэтому IOException оборачиваем здесь.
    private Path copySample(String relative, String name) {
        Path target = tempDir.resolve(name);
        try {
            Files.copy(Path.of(relative), target);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return target;
    }

    private Path write(String name, String content) {
        Path target = tempDir.resolve(name);
        try {
            Files.writeString(target, content);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return target;
    }

    private String readSampleText(String relative) {
        try {
            return Files.readString(Path.of(relative));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private JsonNode readReport(String name) {
        try {
            return new ObjectMapper().readTree(tempDir.resolve(name).toFile());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    // App.run выполняется в отдельном потоке и прерывается по таймауту, если зависнет.
    private static int runWithTimeout(String[] args) {
        return assertTimeoutPreemptively(Duration.ofSeconds(30), () -> App.run(args));
    }

    @Test
    @DisplayName("Запуск без аргументов завершается кодом 1")
    void run_returnsOne_withoutArguments() {
        Allure.step("Act: запуск без аргументов завершается кодом 1", step -> {
            int exitCode = runWithTimeout(new String[] {});
            Allure.step("Assert: код возврата 1", s -> {
                assertEquals(1, exitCode);
            });
        });
    }

    @Test
    @DisplayName("Несуществующий файл завершается кодом 1")
    void run_returnsOne_whenFileNotFound() {
        Allure.step("Act: несуществующий файл завершается кодом 1", step -> {
            int exitCode = runWithTimeout(new String[] {tempDir.resolve("missing.csv").toString()});
            Allure.step("Assert: код возврата 1", s -> {
                assertEquals(1, exitCode);
            });
        });
    }

    @Test
    @DisplayName("Образец без ошибок завершается кодом 0 и создаёт отчёт рядом со входом")
    void run_returnsZero_andWritesReport_forValidSample() {
        Allure.step("Act: образец без ошибок завершается кодом 0 и создаёт отчёт рядом со входом", step -> {
            Path input = copySample("samples/valid/all-metrics-passed.csv", "all-metrics-passed.csv");
            int exitCode = runWithTimeout(new String[] {input.toString()});
            Allure.step("Assert: код возврата 0", s -> {
                assertEquals(0, exitCode);
            });
            boolean reportExists = Files.exists(tempDir.resolve("all-metrics-passed-report.json"));
            Allure.step("Assert: отчёт all-metrics-passed-report.json создан рядом со входом", s -> {
                assertTrue(reportExists);
            });
        });
    }

    @Test
    @DisplayName("Некорректная критичность завершается кодом 1 без отчёта")
    void run_returnsOne_andWritesNoReport_forCriticalityError() {
        Allure.step("Act: некорректная критичность завершается кодом 1 без отчёта", step -> {
            Path input = copySample("samples/invalid/wrong-criticality-case.csv", "wrong-criticality-case.csv");
            int exitCode = runWithTimeout(new String[] {input.toString()});
            Allure.step("Assert: код возврата 1", s -> {
                assertEquals(1, exitCode);
            });
            boolean reportExists = Files.exists(tempDir.resolve("wrong-criticality-case-report.json"));
            Allure.step("Assert: отчёт не создан", s -> {
                assertFalse(reportExists);
            });
        });
    }

    @Test
    @DisplayName("Команда с тремя месяцами ниже 50 % завершается кодом 2 и создаёт отчёт")
    void run_returnsTwo_whenTeamFails() {
        Allure.step("Act: команда с тремя месяцами ниже 50 % завершается кодом 2 и создаёт отчёт", step -> {
            String bad = "9;2026;Bad;Тест;BestEfforts;50;100;90;900;1000;200;10;5;3000";
            String bad2 = "10;2026;Bad;Тест;BestEfforts;50;100;90;900;1000;200;10;5;3000";
            String bad3 = "11;2026;Bad;Тест;BestEfforts;50;100;90;900;1000;200;10;5;3000";
            Path input = write("bad-team.csv", HEADER + "\n" + bad + "\n" + bad2 + "\n" + bad3 + "\n");
            int exitCode = runWithTimeout(new String[] {input.toString()});
            Allure.step("Assert: код возврата 2", s -> {
                assertEquals(2, exitCode);
            });
            boolean reportExists = Files.exists(tempDir.resolve("bad-team-report.json"));
            Allure.step("Assert: отчёт bad-team-report.json создан", s -> {
                assertTrue(reportExists);
            });
            JsonNode report = readReport("bad-team-report.json");
            Allure.step("Assert: вердикт команды в JSON — FAILED", s -> {
                assertEquals("FAILED", report.at("/entries/0/result/verdict").asText());
            });
        });
    }

    @Test
    @DisplayName("Файл с BOM в начале читается без ошибок")
    void run_returnsZero_forFileWithBom() {
        Allure.step("Act: образец с BOM в начале файла", step -> {
            Path input = write("bom-sample.csv", "﻿" + readSampleText("samples/valid/all-metrics-passed.csv"));
            int exitCode = runWithTimeout(new String[] {input.toString()});
            Allure.step("Assert: код возврата 0", s -> {
                assertEquals(0, exitCode);
            });
        });
    }

    @Test
    @DisplayName("Файл только с заголовком завершается кодом 1 без отчёта")
    void run_returnsOne_andWritesNoReport_forHeaderOnlyFile() {
        Allure.step("Act: файл содержит только строку заголовка", step -> {
            Path input = write("header-only.csv", HEADER + "\n");
            int exitCode = runWithTimeout(new String[] {input.toString()});
            Allure.step("Assert: код возврата 1", s -> {
                assertEquals(1, exitCode);
            });
            boolean reportExists = Files.exists(tempDir.resolve("header-only-report.json"));
            Allure.step("Assert: отчёт не создан", s -> {
                assertFalse(reportExists);
            });
        });
    }

    @Test
    @DisplayName("Файл не в кодировке UTF-8 завершается кодом 1 без отчёта")
    void run_returnsOne_andWritesNoReport_forNonUtf8File() {
        Allure.step("Act: файл с байтами, которые не являются UTF-8", step -> {
            Path input = tempDir.resolve("not-utf8.csv");
            try {
                Files.write(input, new byte[] {(byte) 0xFF, (byte) 0xFE, 0x41});
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
            int exitCode = runWithTimeout(new String[] {input.toString()});
            Allure.step("Assert: код возврата 1", s -> {
                assertEquals(1, exitCode);
            });
            boolean reportExists = Files.exists(tempDir.resolve("not-utf8-report.json"));
            Allure.step("Assert: отчёт не создан", s -> {
                assertFalse(reportExists);
            });
        });
    }

    @Test
    @DisplayName("Внутренняя ошибка расчёта завершается кодом 4 и не создаёт отчёт")
    void run_returnsFour_whenCalculationFails() {
        Allure.step("Act: расчёт бросает исключение после успешной валидации", step -> {
            Path input = write("internal-error.csv", HEADER + "\n" + VALID_ROW + "\n");
            App.Calculator failing = records -> {
                throw new IllegalStateException("сбой расчёта");
            };
            int exitCode = assertTimeoutPreemptively(Duration.ofSeconds(30),
                    () -> App.run(new String[] {input.toString()}, failing));
            Allure.step("Assert: код возврата 4", s -> {
                assertEquals(4, exitCode);
            });
            boolean reportExists = Files.exists(tempDir.resolve("internal-error-report.json"));
            Allure.step("Assert: отчёт не создан", s -> {
                assertFalse(reportExists);
            });
        });
    }
}

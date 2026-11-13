package by.strimka.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import by.strimka.input.CsvReadError;
import by.strimka.input.CsvReader;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unit")
@Epic("Калькулятор качества")
@Feature("Валидация входных данных")
class InputValidatorTest {

    static final String HEADER = "month;year;teamName;purpose;criticality;prodBugs;closedTasks;testBugs;"
            + "testingTime;leadTime;changesLeadTime;releases;failures;mttr";
    static final String ROW = "9;2026;Payments;Платежи;BestEfforts;5;100;30;200;1000;50;40;1;30";

    // Лямбды Allure.step не объявляют checked-исключений, поэтому IOException оборачиваем здесь.
    static ValidationResult validate(String rows) {
        try {
            return InputValidator.validate(CsvReader.read(new StringReader(HEADER + "\n" + rows)));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** Подменяет одно значение в строке ROW по индексу колонки (0 = month). */
    static String withValue(int index, String value) {
        String[] parts = ROW.split(";", -1);
        parts[index] = value;
        return String.join(";", parts);
    }

    @Test
    @DisplayName("Валидная строка проходит проверку и создает запись")
    void validate_acceptsValidRow() {
        Allure.step("Act: валидная строка проходит проверку и создает запись", step -> {
            ValidationResult result = validate(ROW);
            Allure.step("Assert: ошибок нет", s -> {
                assertTrue(result.isValid());
            });
            Allure.step("Assert: создана одна запись", s -> {
                assertEquals(1, result.records().size());
            });
        });
    }

    @Test
    @DisplayName("Ненулевые релизы при нулевых сбоях проходят проверку")
    void validate_acceptsReleasesFiveAndFailuresZero_p5() {
        Allure.step("Act: ненулевые релизы при нулевых сбоях проходят проверку", step -> {
            ValidationResult result = validate(withValue(11, "5").replace(";1;30", ";0;30"));
            Allure.step("Assert: ошибок нет", s -> {
                assertTrue(result.isValid());
            });
        });
    }

    @Test
    @DisplayName("Отрицательный счётчик — ошибка строки")
    void validate_reportsNegativeValue() {
        Allure.step("Act: отрицательный счётчик — ошибка строки", step -> {
            ValidationResult result = validate(withValue(5, "-1"));
            Allure.step("Assert: строка 2, колонка prodBugs: значение отрицательное", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(2, "prodBugs", "значение не может быть отрицательным")));
            });
        });
    }

    @Test
    @DisplayName("Пустое значение — ошибка строки")
    void validate_reportsEmptyValue() {
        Allure.step("Act: пустое значение — ошибка строки", step -> {
            ValidationResult result = validate(withValue(5, ""));
            Allure.step("Assert: строка 2, колонка prodBugs: значение пустое", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, "prodBugs", "значение пустое")));
            });
        });
    }

    @ParameterizedTest
    @DisplayName("Месяц вне диапазона 1–12 — ошибка строки")
    @CsvSource({"0", "13"})
    void validate_reportsMonthOutOfRange(String month) {
        Allure.step("Act: месяц " + month + " вне диапазона 1–12 — ошибка строки", step -> {
            ValidationResult result = validate(withValue(0, month));
            Allure.step("Assert: строка 2, колонка month, диапазон 1–12", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(2, "month", "ожидалось от 1 до 12, получено " + month)));
            });
        });
    }

    @Test
    @DisplayName("Год вне диапазона 2020–2100 — ошибка строки")
    void validate_reportsYearOutOfRange() {
        Allure.step("Act: год вне диапазона 2020–2100 — ошибка строки", step -> {
            ValidationResult result = validate(withValue(1, "2019"));
            Allure.step("Assert: строка 2, колонка year, диапазон 2020–2100", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(2, "year", "ожидалось от 2020 до 2100, получено 2019")));
            });
        });
    }

    @Test
    @DisplayName("Критичность в неверном регистре — ошибка с перечнем допустимых значений")
    void validate_reportsWrongCriticalityCase() {
        Allure.step("Act: критичность в неверном регистре — ошибка с перечнем допустимых значений", step -> {
            ValidationResult result = validate(withValue(4, "bestEfforts"));
            Allure.step("Assert: допустимые значения перечислены в тексте ошибки", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, "criticality",
                        "допустимо BestEfforts, BusinessCritical, MissionCritical; получено «bestEfforts»")));
            });
        });
    }

    @Test
    @DisplayName("Повтор команды и месяца — ошибка строки со ссылкой на первую")
    void validate_reportsDuplicatePeriod() {
        Allure.step("Act: повтор команды и месяца — ошибка строки со ссылкой на первую", step -> {
            ValidationResult result = validate(ROW + "\n" + ROW);
            Allure.step("Assert: строка 3 ссылается на строку 2", s -> {
                assertTrue(result.errors().contains(new CsvReadError(3, null,
                        "дубликат команды «Payments» за 9/2026 (впервые в строке 2)")));
            });
        });
    }

    @Test
    @DisplayName("Другое назначение у той же команды — ошибка строки")
    void validate_reportsPurposeMismatchWithinTeam() {
        Allure.step("Act: другое назначение у той же команды — ошибка строки", step -> {
            ValidationResult result = validate(ROW + "\n" + withValue(0, "10").replace("Платежи", "Карты"));
            Allure.step("Assert: строка 3, колонка purpose", s -> {
                assertTrue(result.errors().contains(new CsvReadError(3, "purpose",
                        "у команды «Payments» назначение отличается от строки 2")));
            });
        });
    }

    @Test
    @DisplayName("closedTasks = 0 — ошибка строки с пояснением о долях багов")
    void validate_reportsZeroClosedTasks() {
        Allure.step("Act: closedTasks = 0 — ошибка строки с пояснением о долях багов", step -> {
            ValidationResult result = validate(withValue(6, "0"));
            Allure.step("Assert: сообщение о том, что доли багов не рассчитываются", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, null,
                        "closedTasks = 0, доли багов не рассчитываются")));
            });
        });
    }

    @Test
    @DisplayName("prodBugs = 0 и testBugs = 0 — ошибка строки с пояснением о протекании")
    void validate_reportsZeroTotalBugs() {
        Allure.step("Act: prodBugs = 0 и testBugs = 0 — ошибка строки с пояснением о протекании", step -> {
            String[] parts = ROW.split(";", -1);
            parts[5] = "0";
            parts[7] = "0";
            ValidationResult result = validate(String.join(";", parts));
            Allure.step("Assert: сообщение о том, что протекание не рассчитывается", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, null,
                        "prodBugs и testBugs равны 0, протекание не рассчитывается")));
            });
        });
    }

    @Test
    @DisplayName("leadTime = 0 — ошибка строки с пояснением о TCT")
    void validate_reportsZeroLeadTime() {
        Allure.step("Act: leadTime = 0 — ошибка строки с пояснением о TCT", step -> {
            ValidationResult result = validate(withValue(9, "0"));
            Allure.step("Assert: сообщение о том, что TCT не рассчитывается", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, null,
                        "leadTime = 0, Testing Cycle Time не рассчитывается")));
            });
        });
    }

    @Test
    @DisplayName("releases = 0 — строка отклоняется с пояснением о CFR")
    void validate_reportsZeroReleases() {
        Allure.step("Act: releases = 0 — строка отклоняется с пояснением о CFR", step -> {
            ValidationResult result = validate(withValue(11, "0"));
            Allure.step("Assert: сообщение о том, что CFR не рассчитывается", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, null,
                        "releases = 0, Change Failure Rate не рассчитывается")));
            });
            Allure.step("Assert: файл не проходит валидацию", s -> {
                assertFalse(result.isValid());
            });
        });
    }

    @Test
    @DisplayName("failures больше releases — ошибка строки")
    void validate_reportsFailuresAboveReleases() {
        Allure.step("Act: failures больше releases — ошибка строки", step -> {
            ValidationResult result = validate(ROW.replace(";40;1;30", ";1;2;30"));
            Allure.step("Assert: строка 2: failures больше releases", s -> {
                assertTrue(result.errors().contains(new CsvReadError(2, null, "failures больше releases")));
            });
        });
    }

    @Test
    @DisplayName("Ошибки из разных строк попадают в результат все сразу")
    void validate_reportsErrorsFromAllRows() {
        Allure.step("Act: строка 2 с prodBugs = -1, строка 3 корректна, строка 4 с месяцем 13", step -> {
            ValidationResult result = validate(withValue(5, "-1") + "\n" + withValue(0, "10") + "\n"
                    + withValue(0, "13"));
            Allure.step("Assert: ошибка отрицательного prodBugs в строке 2", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(2, "prodBugs", "значение не может быть отрицательным")));
            });
            Allure.step("Assert: ошибка месяца в строке 4", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(4, "month", "ожидалось от 1 до 12, получено 13")));
            });
        });
    }

    @Test
    @DisplayName("Повтор команды и месяца в конце файла — ошибка последней строки со ссылкой на первую")
    void validate_reportsDuplicateAtEndOfFile() {
        Allure.step("Act: строка 2 и строка 3 разные, строка 4 повторяет строку 2", step -> {
            ValidationResult result = validate(ROW + "\n" + withValue(0, "10") + "\n" + ROW);
            Allure.step("Assert: ошибка дубликата в строке 4 ссылается на строку 2", s -> {
                assertTrue(result.errors().contains(new CsvReadError(4, null,
                        "дубликат команды «Payments» за 9/2026 (впервые в строке 2)")));
            });
            Allure.step("Assert: файл не проходит валидацию", s -> {
                assertFalse(result.isValid());
            });
        });
    }

    @Test
    @DisplayName("Повтор и отрицательный счётчик в одной строке дают две ошибки")
    void validate_reportsDuplicateAndNegativeValueInSameRow() {
        Allure.step("Act: строка 3 повторяет строку 2 и содержит prodBugs = -1", step -> {
            ValidationResult result = validate(ROW + "\n" + withValue(5, "-1"));
            Allure.step("Assert: ошибка отрицательного prodBugs в строке 3", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(3, "prodBugs", "значение не может быть отрицательным")));
            });
            Allure.step("Assert: ошибка дубликата в строке 3", s -> {
                assertTrue(result.errors().contains(new CsvReadError(3, null,
                        "дубликат команды «Payments» за 9/2026 (впервые в строке 2)")));
            });
        });
    }

    @Test
    @DisplayName("Повтор строк с месяцем 13 не считается дубликатом: ключ невалиден")
    void validate_doesNotReportDuplicateForInvalidMonthKey() {
        Allure.step("Act: две строки команды Payments с месяцем 13", step -> {
            ValidationResult result = validate(withValue(0, "13") + "\n" + withValue(0, "13"));
            Allure.step("Assert: ошибка месяца в строке 2", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(2, "month", "ожидалось от 1 до 12, получено 13")));
            });
            Allure.step("Assert: ошибки дубликата нет", s -> {
                assertFalse(result.errors().contains(new CsvReadError(3, null,
                        "дубликат команды «Payments» за 13/2026 (впервые в строке 2)")));
            });
        });
    }
}

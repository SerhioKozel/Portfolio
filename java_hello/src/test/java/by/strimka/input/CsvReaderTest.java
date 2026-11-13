package by.strimka.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("component")
@Epic("Калькулятор качества")
@Feature("Разбор CSV")
class CsvReaderTest {

    static final String HEADER = "month;year;teamName;purpose;criticality;prodBugs;closedTasks;testBugs;"
            + "testingTime;leadTime;changesLeadTime;releases;failures;mttr";
    static final String ROW = "9;2026;Payments;Платежи;BestEfforts;5;100;30;200;1000;50;40;1;30";

    // Лямбды Allure.step не объявляют checked-исключений, поэтому IOException оборачиваем здесь.
    static CsvReadResult read(String text) {
        try {
            return CsvReader.read(new StringReader(text));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Test
    @DisplayName("Валидная строка разбирается без ошибок, значения попадают в поля")
    void read_parsesValidRow() {
        Allure.step("Act: валидная строка разбирается без ошибок, значения попадают в поля", step -> {
            CsvReadResult result = read(HEADER + "\n" + ROW);
            Allure.step("Assert: ошибок нет, строка одна", s -> {
                assertTrue(result.errors().isEmpty());
                assertEquals(1, result.rows().size());
            });
            Allure.step("Assert: значения полей соответствуют строке", s -> {
                CsvRow row = result.rows().get(0);
                assertEquals("Payments", row.getTeamName());
                assertEquals(9, row.getMonth());
                assertEquals(5, row.getProdBugs());
                assertEquals(30.0, row.getMttr());
            });
        });
    }

    @Test
    @DisplayName("Колонки в произвольном порядке сопоставляются по имени")
    void read_acceptsColumnsInAnyOrder() {
        Allure.step("Act: колонки в произвольном порядке сопоставляются по имени", step -> {
            String header = "teamName;purpose;criticality;month;year;prodBugs;closedTasks;testBugs;"
                    + "testingTime;leadTime;changesLeadTime;releases;failures;mttr";
            String row = "Payments;Платежи;BestEfforts;9;2026;5;100;30;200;1000;50;40;1;30";
            CsvReadResult result = read(header + "\n" + row);
            Allure.step("Assert: ошибок нет", s -> {
                assertTrue(result.errors().isEmpty());
            });
            Allure.step("Assert: значения сопоставлены по именам колонок", s -> {
                assertEquals(9, result.rows().get(0).getMonth());
                assertEquals("Payments", result.rows().get(0).getTeamName());
            });
        });
    }

    @Test
    @DisplayName("Пробелы вокруг значений обрезаются, концы строк CRLF допустимы")
    void read_trimsSpacesAndAcceptsCrlf() {
        Allure.step("Act: пробелы вокруг значений обрезаются, концы строк CRLF допустимы", step -> {
            String spaced = ROW.replace(";", " ; ");
            CsvReadResult result = read(HEADER + "\r\n" + spaced + "\r\n");
            Allure.step("Assert: ошибок нет", s -> {
                assertTrue(result.errors().isEmpty());
            });
            Allure.step("Assert: значения обрезаны", s -> {
                assertEquals("Payments", result.rows().get(0).getTeamName());
                assertEquals(2026, result.rows().get(0).getYear());
            });
        });
    }

    @Test
    @DisplayName("Разделитель внутри кавычек не разбивает поле")
    void read_keepsSemicolonInsideQuotes() {
        Allure.step("Act: разделитель внутри кавычек не разбивает поле", step -> {
            String row = "9;2026;Payments;\"Платежи; карты\";BestEfforts;5;100;30;200;1000;50;40;1;30";
            CsvReadResult result = read(HEADER + "\n" + row);
            Allure.step("Assert: ошибок нет", s -> {
                assertTrue(result.errors().isEmpty());
            });
            Allure.step("Assert: поле purpose целиком", s -> {
                assertEquals("Платежи; карты", result.rows().get(0).getPurpose());
            });
        });
    }

    @Test
    @DisplayName("Отсутствие колонки mttr в заголовке — ошибка уровня файла")
    void read_reportsMissingHeaderColumn_atFileLevel() {
        Allure.step("Act: отсутствие колонки mttr в заголовке — ошибка уровня файла", step -> {
            String header = HEADER.replace(";mttr", "");
            CsvReadResult result = read(header + "\n" + ROW.substring(0, ROW.lastIndexOf(';')));
            Allure.step("Assert: ошибка уровня файла с текстом контракта", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(0, null, "отсутствует обязательная колонка «mttr»")));
            });
        });
    }

    @Test
    @DisplayName("Неизвестная колонка — ошибка уровня файла с её именем")
    void read_reportsUnknownColumn() {
        Allure.step("Act: неизвестная колонка — ошибка уровня файла с её именем", step -> {
            CsvReadResult result = read(HEADER + ";extra\n" + ROW + ";x");
            Allure.step("Assert: ошибка уровня файла с именем колонки extra", s -> {
                assertTrue(result.errors().contains(
                        new CsvReadError(0, null, "неизвестная колонка «extra»")));
            });
        });
    }

    @Test
    @DisplayName("Запятая в числе — ошибка с номером строки и колонки")
    void read_reportsDecimalComma_withLineAndColumn() {
        Allure.step("Act: запятая в числе — ошибка с номером строки и колонки", step -> {
            String row = "9;2026;Payments;Платежи;BestEfforts;5;100;30;200,5;1000;50;40;1;30";
            CsvReadResult result = read(HEADER + "\n" + row);
            Allure.step("Assert: одна ошибка: строка 2, колонка testingTime, текст контракта", s -> {
                assertEquals(1, result.errors().size());
                assertEquals(new CsvReadError(2, "testingTime", "используйте точку, получено «200,5»"),
                        result.errors().get(0));
            });
        });
    }

    @Test
    @DisplayName("Нечисловое значение — ошибка с номером строки и колонки")
    void read_reportsNonNumber_withLineAndColumn() {
        Allure.step("Act: нечисловое значение — ошибка с номером строки и колонки", step -> {
            String row = "9;2026;Payments;Платежи;BestEfforts;5;abc;30;200;1000;50;40;1;30";
            CsvReadResult result = read(HEADER + "\n" + row);
            Allure.step("Assert: строка 2, колонка closedTasks", s -> {
                assertEquals(new CsvReadError(2, "closedTasks", "ожидалось целое число, получено «abc»"),
                        result.errors().get(0));
            });
        });
    }

    @Test
    @DisplayName("Ошибка во второй строке данных указывает строку 3")
    void read_reportsLineNumber_forSecondDataRow() {
        Allure.step("Act: ошибка во второй строке данных указывает строку 3", step -> {
            String bad = "9;2026;Cards;Карты;BestEfforts;5;100;30;200;1000;50;40;1;x";
            CsvReadResult result = read(HEADER + "\n" + ROW + "\n" + bad);
            Allure.step("Assert: номер строки файла равен 3", s -> {
                assertEquals(3, result.errors().get(0).line());
            });
        });
    }

    @Test
    @DisplayName("Лишнее поле в строке — ошибка с номером строки и числом полей")
    void read_reportsWrongFieldCount() {
        Allure.step("Act: лишнее поле в строке — ошибка с номером строки и числом полей", step -> {
            CsvReadResult result = read(HEADER + "\n" + ROW + ";extra");
            Allure.step("Assert: строка 2, ожидалось 14 полей, получено 15", s -> {
                assertEquals(new CsvReadError(2, null, "ожидалось полей 14, получено 15"),
                        result.errors().get(0));
            });
        });
    }

    @Test
    @DisplayName("Пустой файл — ошибка уровня файла")
    void read_reportsEmptyFile() {
        Allure.step("Act: пустой файл — ошибка уровня файла", step -> {
            CsvReadResult result = read("");
            Allure.step("Assert: ошибка «файл пуст»", s -> {
                assertEquals(new CsvReadError(0, null, "файл пуст"), result.errors().get(0));
            });
        });
    }

    @Test
    @DisplayName("Незакрытая кавычка — ошибка строки 2 с текстом контракта")
    void read_reportsUnclosedQuote_withContractMessage() {
        Allure.step("Act: незакрытая кавычка — ошибка строки 2 с текстом контракта", step -> {
            String bad = "9;2026;\"Payments;Платежи;BestEfforts;5;100;30;200;1000;50;40;1;30";
            CsvReadResult result = read(HEADER + "\n" + bad);
            Allure.step("Assert: строка 2, «незакрытые кавычки»", s -> {
                assertEquals(new CsvReadError(2, null, "незакрытые кавычки"), result.errors().get(0));
            });
        });
    }

    @Test
    @DisplayName("Формы Double.parseDouble, не являющиеся десятичным числом, отвергаются с номером строки и колонки")
    void read_rejectsNonDecimalNumberForms() {
        Allure.step("Act: суффикс d, шестнадцатеричная запись, знак плюс, экспонента и NaN отвергаются", step -> {
            for (String value : List.of("200d", "0x1p3", "+5", "1e3", "NaN")) {
                String row = "9;2026;Payments;Платежи;BestEfforts;5;100;30;" + value + ";1000;50;40;1;30";
                CsvReadResult result = read(HEADER + "\n" + row);
                Allure.step("Assert: одна ошибка для значения «" + value + "»", s -> {
                    assertEquals(List.of(new CsvReadError(2, "testingTime",
                            "ожидалось число, получено «" + value + "»")), result.errors());
                });
            }
        });
    }

    @Test
    @DisplayName("Дробное число с точкой принимается, значение попадает в поле")
    void read_acceptsDecimalWithDot() {
        Allure.step("Act: дробное число с точкой принимается", step -> {
            String row = "9;2026;Payments;Платежи;BestEfforts;5;100;30;200.5;1000;50;40;1;30";
            CsvReadResult result = read(HEADER + "\n" + row);
            Allure.step("Assert: ошибок нет, testingTime = 200.5", s -> {
                assertTrue(result.errors().isEmpty());
                assertEquals(200.5, result.rows().get(0).getTestingTime());
            });
        });
    }
}

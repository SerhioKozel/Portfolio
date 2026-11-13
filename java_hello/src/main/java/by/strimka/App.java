package by.strimka;

import by.strimka.domain.PeriodRecord;
import by.strimka.evaluation.QualityEntry;
import by.strimka.evaluation.TeamEvaluator;
import by.strimka.input.CsvReadError;
import by.strimka.input.CsvReadResult;
import by.strimka.input.CsvReader;
import by.strimka.output.ConsoleRenderer;
import by.strimka.output.ReportJsonWriter;
import by.strimka.report.QualityReport;
import by.strimka.report.ReportMapper;
import by.strimka.validation.InputValidator;
import by.strimka.validation.ValidationResult;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Точка входа логики приложения: читает CSV, проверяет, считает метрики,
 * выводит отчёт и пишет JSON. Возвращает код выхода, а не вызывает System.exit,
 * поэтому логику можно вызывать из тестов без завершения JVM.
 */
public final class App {

    // Логгер нужен только для ошибок записи отчёта. Остальной вывод идёт через ConsoleRenderer.
    private static final Logger log = LogManager.getLogger(App.class);

    // Коды возврата: 0 — все команды выполнили требования; 1 — ошибка входных данных;
    // 2 — есть команды, не выполнившие требования; 3 — не удалось записать JSON-отчёт;
    // 4 — внутренняя ошибка расчёта (неожиданное исключение, не связанное с входными данными).
    private static final int EXIT_PASSED = 0;
    private static final int EXIT_FAILED = 2;
    private static final int EXIT_INVALID_INPUT = 1;
    private static final int EXIT_WRITE_ERROR = 3;
    private static final int EXIT_INTERNAL_ERROR = 4;

    // BOM — невидимый символ в начале файла, который Excel добавляет к CSV в UTF-8.
    // Если его не убрать, первое имя колонки не совпадёт с ожидаемым.
    private static final char BOM = '\uFEFF';

    /** Расчёт команд по записям. Точка подмены: по умолчанию {@link TeamEvaluator#evaluateAll}. */
    @FunctionalInterface
    interface Calculator {
        List<QualityEntry> calculate(List<PeriodRecord> records);
    }

    private App() {
    }

    /**
     * Обрабатывает один входной файл и возвращает код выхода.
     * Порядок: аргументы → существование файла → чтение и разбор → валидация →
     * расчёт → вывод в консоль → запись JSON. Ошибка входа прерывает обработку:
     * последующие действия не выполняются.
     */
    static int run(String[] args) {
        return run(args, TeamEvaluator::evaluateAll);
    }

    /**
     * То же, что {@link #run(String[])}, но расчёт передаётся снаружи. Нужно тестам, чтобы
     * воспроизвести внутреннюю ошибку, которую валидный CSV вызвать не может.
     */
    static int run(String[] args, Calculator calculator) {
        if (args.length != 1) {
            return invalidInput("ожидается один аргумент: путь к CSV-файлу, получено " + args.length);
        }
        Path input;
        try {
            input = Path.of(args[0]);
        } catch (InvalidPathException exception) {
            return invalidInput("некорректный путь к файлу: " + args[0]);
        }
        // Проверяем существование заранее, чтобы сообщить о проблеме понятным текстом,
        // а не получить IOException при чтении.
        if (!Files.isRegularFile(input)) {
            return invalidInput("файл не найден: " + input);
        }

        // Файл читается потоком: CsvReader берёт записи по одной, весь текст в памяти не держим.
        // Files.newBufferedReader декодирует UTF-8 и бросает исключение на некорректных байтах.
        CsvReadResult read;
        try (BufferedReader reader = Files.newBufferedReader(input)) {
            skipBom(reader);
            read = CsvReader.read(reader);
        } catch (IOException exception) {
            return invalidInput("файл не читается: " + input);
        }

        // Ошибки разбора и ошибки валидации объединяются в один список,
        // чтобы пользователь увидел все проблемы файла за один запуск.
        ValidationResult validation = InputValidator.validate(read);
        List<CsvReadError> errors = new ArrayList<>(validation.errors());
        // Файл из одного заголовка не содержит ошибок формата, но и расчёт по нему невозможен.
        // Сообщаем об этом отдельно.
        if (read.rows().isEmpty() && errors.isEmpty()) {
            errors.add(new CsvReadError(0, null, "нет строк данных после заголовка"));
        }
        // Частичный отчёт по неверным данным мог бы ввести в заблуждение, поэтому при любой ошибке
        // отчёт не формируется.
        if (!errors.isEmpty()) {
            ConsoleRenderer.printErrors(errors);
            return EXIT_INVALID_INPUT;
        }

        // Расчёт выполняется только по записям, прошедшим валидацию.
        List<QualityEntry> entries;
        try {
            entries = calculator.calculate(validation.records());
        } catch (RuntimeException exception) {
            // Данные уже прошли валидацию, поэтому любое исключение здесь — дефект или сбой среды.
            log.error("внутренняя ошибка расчёта", exception);
            return EXIT_INTERNAL_ERROR;
        }
        QualityReport report = ReportMapper.toReport(entries);
        ConsoleRenderer.printReport(report);

        // JSON кладётся рядом со входным файлом. Ошибка записи имеет собственный код возврата,
        // потому что результат к этому моменту уже выведен в консоль.
        Path reportPath = ReportJsonWriter.reportPath(input);
        try {
            ReportJsonWriter.write(report, reportPath);
        } catch (IOException exception) {
            log.error("не удалось записать отчёт «" + reportPath + "»: " + exception.getMessage());
            return EXIT_WRITE_ERROR;
        }

        // Код 0 или 2 зависит только от вердиктов команд: ошибки входа сюда не доходят.
        boolean allPassed = entries.stream().allMatch(entry -> entry.result().passed());
        return allPassed ? EXIT_PASSED : EXIT_FAILED;
    }

    /**
     * Единая точка для ошибок, связанных со входом (аргументы, файл, кодировка).
     * Сообщение выводится в том же формате, что и ошибки CSV, с номером строки 0 (уровень файла).
     */
    private static int invalidInput(String message) {
        ConsoleRenderer.printErrors(List.of(new CsvReadError(0, null, message)));
        return EXIT_INVALID_INPUT;
    }

    /**
     * Пропускает BOM в начале потока. Если первый символ не BOM, возвращаем позицию назад,
     * чтобы его не потерять.
     */
    private static void skipBom(BufferedReader reader) throws IOException {
        reader.mark(1);
        if (reader.read() != BOM) {
            reader.reset();
        }
    }
}
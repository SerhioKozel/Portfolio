package by.strimka.output;

import by.strimka.report.QualityReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Запись JSON-отчёта. ObjectMapper создаётся один раз: после настройки его можно использовать повторно.
 */
public final class ReportJsonWriter {

    private static final String REPORT_SUFFIX = "-report.json";

    // Используется настройка по умолчанию. Отступы включаются в writer ниже.
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReportJsonWriter() {
    }

    /**
     * Имя отчёта рядом со входом: data/payments.csv → data/payments-report.json.
     */
    public static Path reportPath(Path input) {
        String fileName = input.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        Path parent = input.getParent();
        String reportName = baseName + REPORT_SUFFIX;
        return parent == null ? Path.of(reportName) : parent.resolve(reportName);
    }

    /**
     * Пишет отчёт с отступами. Сначала запись идёт во временный файл рядом с целевым, затем он
     * атомарно переименовывается. Так при сбое не остаётся обрезанный отчёт. Ошибка записи пробрасывается наверх:
     * вызывающий код решает, какой код возврата вернуть.
     */
    public static void write(QualityReport report, Path target) throws IOException {
        Path absolute = target.toAbsolutePath();
        // Временный файл в той же папке: переименование внутри одного раздела атомарно.
        Path temp = Files.createTempFile(absolute.getParent(), absolute.getFileName().toString(), ".tmp");
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), report);
            Files.move(temp, absolute, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}

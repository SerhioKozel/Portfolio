package by.strimka.input;

/**
 * Ошибка разбора файла. line = 0 для ошибок уровня файла (заголовок),
 * column = null, если ошибка относится ко всей строке.
 */
public record CsvReadError(long line, String column, String message) {
}

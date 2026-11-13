package by.strimka.domain;

/**
 * Команда из входного файла. Название, назначение и критичность одинаковы во всех месяцах команды.
 */
public record Team(String name, String purpose, Criticality criticality) {
}

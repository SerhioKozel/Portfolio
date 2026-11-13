package by.strimka.evaluation;

import by.strimka.domain.Team;

/**
 * Команда и её итог. Это одна строка отчёта.
 */
public record QualityEntry(Team team, QualityResult result) {
}

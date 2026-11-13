package by.strimka.domain;

import java.util.Optional;

/**
 * Критичность команды. csvValue — значение из входного CSV.
 * Значения хранятся в enum, чтобы строки не сравнивались в нескольких местах.
 */
public enum Criticality {
    BEST_EFFORTS("BestEfforts"),
    BUSINESS_CRITICAL("BusinessCritical"),
    MISSION_CRITICAL("MissionCritical");

    // Значение, которое ожидается в колонке criticality.
    private final String csvValue;

    Criticality(String csvValue) {
        this.csvValue = csvValue;
    }

    /**
     * Значение критичности, как оно записано в CSV.
     */
    public String csvValue() {
        return csvValue;
    }

    /**
     * Ищет критичность по значению из CSV. Неизвестное значение — ожидаемая ошибка данных,
     * поэтому возвращается empty, а не исключение. Ошибку показывает валидатор.
     */
    public static Optional<Criticality> fromCsv(String raw) {
        for (Criticality criticality : values()) {
            if (criticality.csvValue.equals(raw)) {
                return Optional.of(criticality);
            }
        }
        return Optional.empty();
    }
}

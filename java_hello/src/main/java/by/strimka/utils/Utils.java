package by.strimka.utils;

import by.strimka.exception.InvalidInputException;
import java.util.Locale;

/**
 * Вспомогательные функции: форматирование и проценты. Состояния нет, конструктор приватный.
 */
public final class Utils {

    private static final String ZERO_DENOMINATOR_TEMPLATE = "Параметр %s не может быть равен 0";

    private Utils() {
    }

    // Locale.ROOT фиксирует формат чисел и строк независимо от настроек ОС.

    /**
     * Форматирует строку с Locale.ROOT, чтобы числа не зависели от настроек ОС.
     */
    public static String format(String template, Object... args) {
        return String.format(Locale.ROOT, template, args);
    }

    /**
     * Процент = numerator / denominator × 100. Единственный способ считать проценты в проекте.
     * Нулевой знаменатель во входе отсекает валидатор, поэтому здесь это внутренний инвариант:
     * исключение защищает от тихого NaN в отчёте, если проверка во входе когда-нибудь пропадёт.
     */
    public static double percent(double numerator, double denominator, String denominatorName) {
        if (denominator == 0) {
            throw new InvalidInputException(format(ZERO_DENOMINATOR_TEMPLATE, denominatorName));
        }
        return numerator * 100.0 / denominator;
    }
}

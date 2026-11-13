package by.strimka.exception;

/**
 * Нарушение инварианта входных данных, которое дошло до расчёта.
 * Непроверяемое исключение: означает, что валидатор пропустил данные, а не что вызывающий код
 * должен что-то обработать.
 */
public class InvalidInputException extends RuntimeException {

    /**
     * Создаёт исключение с описанием нарушения.
     */
    public InvalidInputException(String message) {
        super(message);
    }
}
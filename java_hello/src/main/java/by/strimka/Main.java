package by.strimka;

/**
 * Точка входа JVM. Передаёт результат App.run в код выхода процесса.
 * Вся логика находится в App.
 */
public class Main {

    /**
     * Запускает приложение. Код выхода процесса — результат {@link App#run}.
     */
    public static void main(String[] args) {
        System.exit(App.run(args));
    }
}

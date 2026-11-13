# Ожидаемый вывод для samples/invalid/

Запуск: `java ... by.strimka.Main samples\invalid\<файл>.csv`. Во всех случаях: код возврата 1, отчёт не создаётся.

## wrong-criticality-case.csv
```
строка 2, колонка «criticality»: допустимо BestEfforts, BusinessCritical, MissionCritical; получено «bestEfforts»
Найдено ошибок: 1. Отчёт не создан.
```

## zero-closed-tasks.csv
closedTasks = 0 и testBugs = 0 одновременно вызывают две логические ошибки. Порядок: closedTasks, затем протекание.
```
строка 2: closedTasks = 0, доли багов не рассчитываются
строка 2: prodBugs и testBugs равны 0, протекание не рассчитывается
Найдено ошибок: 2. Отчёт не создан.
```

## duplicate-period.csv
```
строка 3: дубликат команды «Payments» за 9/2026 (впервые в строке 2)
Найдено ошибок: 1. Отчёт не создан.
```

## missing-mttr-column.csv
Колонки mttr нет, строки данных не разбираются.
```
файл: отсутствует обязательная колонка «mttr»
Найдено ошибок: 1. Отчёт не создан.
```

## comma-decimal.csv
```
строка 2, колонка «testingTime»: используйте точку, получено «200,5»
Найдено ошибок: 1. Отчёт не создан.
```

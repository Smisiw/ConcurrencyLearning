## SafeCounter - Реализация счетчика с потокобезопасностью
### **Анализ результатов**
#### **1. Тест с 1000 потоками (создание отдельных потоков)**
| Реализация         | Время (нс)  |
|--------------------|------------|
| `LongAdderSafeCounter` | 247123 |
| `AtomicSafeCounter` | 304237 |
| `SynchronizedSafeCounter` | 417735 |
| `LockSafeCounter` | 554341 |

- **Лучший результат у `LongAdderSafeCounter`**  
  `LongAdder` спроектирован для высокой конкуренции, он использует сегментацию, чтобы уменьшить блокировки между потоками. Поэтому он эффективнее при большом количестве потоков.  

- **Хуже всего работает `LockSafeCounter`**  
  `ReentrantLock` вызывает накладные расходы на блокировки и снятие блокировок. Потоки вынуждены ожидать освобождения `lock`, что приводит к задержкам.  

#### **2. Тест с ThreadPoolExecutor (10 потоков)**
| Реализация         | Время (нс)  |
|--------------------|------------|
| `LongAdderSafeCounter` | 119927 |
| `AtomicSafeCounter` | 79861 |
| `SynchronizedSafeCounter` | 779631 |
| `LockSafeCounter` | 1678200 |

- **Лучший результат у `AtomicSafeCounter`**  
  В условиях фиксированного количества потоков `AtomicInteger` работает лучше, так как не требует дополнительных структур данных, как `LongAdder`.  

- **Плохая работа `SynchronizedSafeCounter` и `LockSafeCounter`**  
  `synchronized` приводит к высокому числу блокировок, а `ReentrantLock` ещё хуже справляется с конкуренцией в пуле потоков. Это объясняет значительно большие значения времени выполнения.
---
## MultithreadedDataProcessing - Параллельное вычисление суммы массива
### **Анализ результатов**
| Метод вычисления | Время выполнения (нс) | Итоговая сумма |
|-----------------|------------------|---------------|
| **Однопоточное суммирование** | 5_139_000 | 49_557_054 |
| **Ручное управление потоками (`ExecutorService`)** | 35_285_001 | 49_557_054 |
| **Parallel Stream** | 30_238_699 | 49_557_054 |
| **ForkJoinPool (`List<Integer>`)** | 43_783_999 | 49_557_054 |
| **ForkJoinPool (`int[]`)** | 12_484_800 | 49_557_054 |

**Задача оказалась слишком простой из-за чего однопоточное суммирование справилось быстрее всего (5.1 мс), так как при параллельных вычислениях добавлялись накладные расходы на создание потоков.**

**Ручное управление потоками через ExecutorService оказалось самым медленным**  
   - Из-за накладных расходов на управление потоками и объединение результатов через `Future.get()`, метод отработал дольше всех (35.3 мс). Этот метод полезен, если нужно управлять задачами вручную, но не подходит для простых задач типа суммирования.

**ForkJoinPool с `List<Integer>` показал худший результат среди ForkJoinPool реализаций** (43.8 мс)  
   - `List<Integer>` вызывает autoboxing, что приводит к дополнительным затратам памяти и процессорного времени.

**Parallel Stream быстрее ForkJoinPool с `List<Integer>`**  
   - parallelStream справился быстрее, так как он оптимизирован для работы с потоками данных. 

**Самый быстрый метод** → **ForkJoinPool с `int[]`** (12.4 мс)  
   - Использование примитивного массива `int[]` позволило избежать накладных расходов на autoboxing и работу с `List<Integer>`, что улучшило скорость в 3-4 раза. 

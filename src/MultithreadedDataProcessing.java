import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

class Adder implements Callable<Integer> {
    private final List<Integer> list;
    public Adder(List<Integer> list) {
        this.list = list;
    }
    @Override
    public Integer call() {
        int sum = 0;
        for (Integer i : list) {
            sum += i;
        }
        return sum;
    }
}

class ForkSumCounter1 extends RecursiveTask<Integer> {
    private static final int THRESHOLD = 10_000; //порог разбиения задачи
    private final List<Integer> list;

    public ForkSumCounter1(List<Integer> list) {
        this.list = list;
    }

    @Override
    protected Integer compute() {
        if (list.size() <= THRESHOLD) {
            return list.stream().mapToInt(Integer::intValue).sum();
        }

        int mid = list.size() / 2;
        ForkSumCounter1 left = new ForkSumCounter1(new ArrayList<>(list.subList(0, mid)));
        ForkSumCounter1 right = new ForkSumCounter1(new ArrayList<>(list.subList(mid, list.size())));

        invokeAll(left, right);

        return left.join() + right.join();
    }
}

class ForkSumCounter2 extends RecursiveTask<Integer> {
    //List<Integer> заменен на int[], увеличен порог разбиения, вторая часть задачи выполняется в том же потоке
    private static final int THRESHOLD = 200_000;
    private final int[] array;
    private final int start, end;

    public ForkSumCounter2(int[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    @Override
    protected Integer compute() {
        if (end - start <= THRESHOLD) {
            int sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        }

        int mid = (start + end) / 2;
        ForkSumCounter2 left = new ForkSumCounter2(array, start, mid);
        ForkSumCounter2 right = new ForkSumCounter2(array, mid, end);

        left.fork();
        int rightResult = right.compute();
        return left.join() + rightResult;
    }
}


public class MultithreadedDataProcessing {
    public static void main(String[] args) {
        int numbersCount = 1_000_000;
        int threadCount = 10;
        int simpleSum = 0;
        int multithreadedSum = 0;
        List<Integer> list = new ArrayList<>();
        Random random = new Random();

        // Заполняем список случайными числами
        for (int i = 0; i < numbersCount; i++) {
            list.add(random.nextInt(100));
        }

        // Однопоточное суммирование
        long startTime = System.nanoTime();
        for (int i : list) {
            simpleSum += i;
        }
        long endTime = System.nanoTime();
        long totalTime1 = endTime - startTime;

        // Многопоточное суммирование с ручным управлением потоками
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        startTime = System.nanoTime();
        for (int i = 0; i < threadCount; i++) {
            List<Integer> sublist = new ArrayList<>(list.subList(i * numbersCount / threadCount, (i + 1) * numbersCount / threadCount));
            futures.add(executorService.submit(new Adder(sublist)));
        }

        // Дожидаемся выполнения всех потоков
        for (Future<Integer> future : futures) {
            try {
                multithreadedSum += future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        endTime = System.nanoTime();
        long totalTime2 = endTime - startTime;

        executorService.shutdown();

        // Параллельное вычисление с parallelStream()
        startTime = System.nanoTime();
        int parallelSum = list.parallelStream().mapToInt(Integer::intValue).sum();
        endTime = System.nanoTime();
        long totalTime3 = endTime - startTime;

        //Параллельное вычисление с ForkJoinPool
        ForkSumCounter1 forkSumCounter1 = new ForkSumCounter1(list);
        ForkSumCounter2 forkSumCounter2 = new ForkSumCounter2(list.stream().mapToInt(i -> i).toArray(), 0,list.size());
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        startTime = System.nanoTime();
        int forkSum1 = forkJoinPool.invoke(forkSumCounter1);
        endTime = System.nanoTime();
        long totalTime4 = endTime - startTime;
        startTime = System.nanoTime();
        int forkSum2 = forkJoinPool.invoke(forkSumCounter2);
        endTime = System.nanoTime();
        long totalTime5 = endTime - startTime;

        // Выводим результаты
        System.out.println("Однопоточный подсчет суммы: " + totalTime1);
        System.out.println("Параллельный подсчет суммы с ручным управлением потоками: " + totalTime2);
        System.out.println("Параллельный подсчет суммы с parallelStream: " + totalTime3);
        System.out.println("Параллельный подсчет суммы с ForkJoinPool: " + totalTime4);
        System.out.println("Параллельный подсчет суммы с ForkJoinPool с использованием примитивного типа int: " + totalTime5);
        System.out.println("Сумма в однопоточном режиме: " + simpleSum);
        System.out.println("Сумма в многопоточном режиме с ручным управлением потоками: " + multithreadedSum);
        System.out.println("сумма в многопоточном режиме с parallelStream: " + parallelSum);
        System.out.println("сумма в многопоточном режиме с ForkJoinPool: " + forkSum1);
        System.out.println("сумма в многопоточном режиме с ForkJoinPool с использованием примитивного типа int: " + forkSum2);
    }
}

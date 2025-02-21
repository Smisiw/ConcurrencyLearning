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
    private static final int THRESHOLD = 10_000; //����� ��������� ������
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
    //List<Integer> ������� �� int[], �������� ����� ���������, ������ ����� ������ ����������� � ��� �� ������
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

        // ��������� ������ ���������� �������
        for (int i = 0; i < numbersCount; i++) {
            list.add(random.nextInt(100));
        }

        // ������������ ������������
        long startTime = System.nanoTime();
        for (int i : list) {
            simpleSum += i;
        }
        long endTime = System.nanoTime();
        long totalTime1 = endTime - startTime;

        // ������������� ������������ � ������ ����������� ��������
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        startTime = System.nanoTime();
        for (int i = 0; i < threadCount; i++) {
            List<Integer> sublist = new ArrayList<>(list.subList(i * numbersCount / threadCount, (i + 1) * numbersCount / threadCount));
            futures.add(executorService.submit(new Adder(sublist)));
        }

        // ���������� ���������� ���� �������
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

        // ������������ ���������� � parallelStream()
        startTime = System.nanoTime();
        int parallelSum = list.parallelStream().mapToInt(Integer::intValue).sum();
        endTime = System.nanoTime();
        long totalTime3 = endTime - startTime;

        //������������ ���������� � ForkJoinPool
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

        // ������� ����������
        System.out.println("������������ ������� �����: " + totalTime1);
        System.out.println("������������ ������� ����� � ������ ����������� ��������: " + totalTime2);
        System.out.println("������������ ������� ����� � parallelStream: " + totalTime3);
        System.out.println("������������ ������� ����� � ForkJoinPool: " + totalTime4);
        System.out.println("������������ ������� ����� � ForkJoinPool � �������������� ������������ ���� int: " + totalTime5);
        System.out.println("����� � ������������ ������: " + simpleSum);
        System.out.println("����� � ������������� ������ � ������ ����������� ��������: " + multithreadedSum);
        System.out.println("����� � ������������� ������ � parallelStream: " + parallelSum);
        System.out.println("����� � ������������� ������ � ForkJoinPool: " + forkSum1);
        System.out.println("����� � ������������� ������ � ForkJoinPool � �������������� ������������ ���� int: " + forkSum2);
    }
}

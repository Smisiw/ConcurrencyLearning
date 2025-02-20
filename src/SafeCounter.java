import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

interface ISafeCounter { //��������� ��� ������������� ��������� ���������� safeCounter
    public void increment();
    public void decrement();
    public int getCount();
}

class LongAdderSafeCounter implements ISafeCounter { //���������� SafeCounter � �������������� LongAdder
    private final LongAdder counter = new LongAdder();
    public void increment() { counter.increment(); }
    public void decrement() { counter.decrement(); }
    public int getCount() { return counter.intValue(); }
}

class AtomicSafeCounter implements ISafeCounter { //���������� SafeCounter � �������������� AtomicInteger
    AtomicInteger counter = new AtomicInteger(0);
    public  void increment() {
        counter.incrementAndGet();
    }
    public  void decrement() {
        counter.decrementAndGet();
    }
    public  int getCount() {
        return counter.get();
    }
}

class SynchronizedSafeCounter implements ISafeCounter { //���������� SafeCounter � �������������� synchronized �������
    int counter = 0;
    public synchronized void increment() {
        counter++;
    }
    public synchronized void decrement() {
        counter--;
    }
    public synchronized int getCount() {
        return counter;
    }
}

class LockSafeCounter implements ISafeCounter { //���������� SafeCounter � �������������� Lock
    int counter = 0;
    ReentrantLock lock = new ReentrantLock();
    public void increment() {
        lock.lock();
        try {
            counter++;
        }
        finally {
            lock.unlock();
        }
    }
    public void decrement() {
        lock.lock();
        try {
            counter--;
        }
        finally {
            lock.unlock();
        }
    }
    public int getCount() {
        return counter;
    }
}

public class SafeCounter {
    static long task1(ISafeCounter safeCounter) {
        AtomicLong sumTime = new AtomicLong(0); // ������������ ���������������� ���������� �������

        for (int i = 0; i < 100; i++) {
            List<Thread> threads = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                threads.add(new Thread(() -> {
                    long start = System.nanoTime();
                    safeCounter.increment();
                    safeCounter.decrement();
                    safeCounter.increment();
                    long end = System.nanoTime();
                    sumTime.addAndGet(end - start);
                }));
            }
            threads.forEach(Thread::start);
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return sumTime.get() / 100;
    }

    static long task2(ISafeCounter safeCounter) {
        AtomicLong sumTime = new AtomicLong(0);
        for (int i = 0; i < 100; i++) {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<Future<?>> futures = new ArrayList<>();

            for (int j = 0; j < 1000; j++) {
                futures.add(executor.submit(() -> {
                    long start = System.nanoTime();
                    safeCounter.increment();
                    safeCounter.decrement();
                    safeCounter.increment();
                    long end = System.nanoTime();
                    sumTime.addAndGet(end - start);
                }));
            }

            executor.shutdown(); // ��������� ���������� ����� �����

            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // �������������� ����������, ���� ������ �������
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // ����������, ��� ��� ������ ����������� �������
            for (Future<?> future : futures) {
                try {
                    future.get(); // ������ ����������, ���� ������ �� ����������� ���������
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return sumTime.get() / 100;
    }

    public static void main(String[] args) {
        System.out.println("���� � �������� 1000 �������:");
        System.out.println("LongAdderSafeCounter: " + task1(new LongAdderSafeCounter()));
        System.out.println("AtomicSafeCounter: " + task1(new AtomicSafeCounter()));
        System.out.println("SynchronizedSafeCounter: " + task1(new SynchronizedSafeCounter()));
        System.out.println("LockSafeCounter: " + task1(new LockSafeCounter()));

        System.out.println("\n���� � �������������� ThreadPoolExecutor:");
        System.out.println("LongAdderSafeCounter: " + task2(new LongAdderSafeCounter()));
        System.out.println("AtomicSafeCounter: " + task2(new AtomicSafeCounter()));
        System.out.println("SynchronizedSafeCounter: " + task2(new SynchronizedSafeCounter()));
        System.out.println("LockSafeCounter: " + task2(new LockSafeCounter()));
    }
}
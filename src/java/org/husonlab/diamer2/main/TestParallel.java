package org.husonlab.diamer2.main;

import jdk.jfr.Timespan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestParallel {
    public static void main(String[] args) {

        int sortingThreads = 5;
        int processingThreads = 3;

        BlockingQueue<Long> queue = new LinkedBlockingQueue<>(1000);
        List<BlockingQueue<Long>> resultQueues = new ArrayList<>(processingThreads);
        for( int i = 0; i < processingThreads; i++ ) {
            resultQueues.add(new LinkedBlockingQueue<>(1000));
        }

        Thread readerThread = new Thread( () -> {
            for( long i = 0; i < 20; i++ ) {
                try {
                    queue.put(i);
                    System.out.println("Reader: Put value " + i + " to queue.");
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while putting to queue.");
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while sleeping.");
                }
            }
            for (int i = 0; i < processingThreads; i++) {
                try {
                    queue.put(-1L);
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while putting to queue.");
                }
            }
            System.out.println("Reader: Finished.");
        });

        ExecutorService sortingExecutor = Executors.newFixedThreadPool(sortingThreads);

        for (int i = 0; i < sortingThreads; i++) {
            int finalI = i;
            sortingExecutor.submit( () -> {
                try {
                    while (true) {
                        Long value = queue.take();
                        if (value == -1) {
                            for (int j = 0; j < processingThreads; j++) {
                                resultQueues.get(j).put(-1L);
                            }
                            System.out.println("Sorting Thread " + finalI + " finished.");
                            break;
                        }
                        if (value % 10 == 0) {
                            resultQueues.get(0).put(value);
                            System.out.println("Sorting Thread " + finalI + " put value " + value + " to queue 0");
                        } else if (value % 10 == 1) {
                            resultQueues.get(1).put(value);
                            System.out.println("Sorting Thread " + finalI + " put value " + value + " to queue 1");
                        } else if (value % 10 == 2) {
                            resultQueues.get(2).put(value);
                            System.out.println("Sorting Thread " + finalI + " put value " + value + " to queue 2");
                        }
                        try {
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException e) {
                            System.err.println("Interrupted while sleeping.");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        List<Thread> secondStageThreads = new ArrayList<>(processingThreads);
        for (int i = 0; i < processingThreads; i++) {
            final int threadIndex = i;
            secondStageThreads.add(new Thread( () -> {
                try {
                    while (true) {
                        Long value = resultQueues.get(threadIndex).take();
                        System.out.println("Secondary Thread " + threadIndex + " got value " + value);
                        try {
                            TimeUnit.MILLISECONDS.sleep(500);
                        } catch (InterruptedException e) {
                            System.err.println("Interrupted while sleeping.");
                        }
                        if (value == -1) {
                            System.out.println("Secondary Thread " + threadIndex + " finished.");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        readerThread.start();
        for (Thread thread : secondStageThreads) {
            thread.start();
        }
        sortingExecutor.shutdown();
    }
}

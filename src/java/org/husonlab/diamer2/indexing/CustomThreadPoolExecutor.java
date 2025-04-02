package org.husonlab.diamer2.indexing;

import org.husonlab.diamer2.util.logging.Logger;

import java.util.concurrent.*;

public class CustomThreadPoolExecutor extends ThreadPoolExecutor {

    private final Logger logger;
    private final int waitBeforeShutdown;
    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, int queueSize, int waitBeforeShutdown, Logger logger) {
        super(corePoolSize,
                maximumPoolSize,
                10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadPoolExecutor.CallerRunsPolicy());
        this.waitBeforeShutdown = waitBeforeShutdown;
        this.logger = logger;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?> && ((Future<?>) r).isDone()) {
            try {
                ((Future<?>) r).get();
            } catch (Throwable th) {
                t = th;
            }}
        if (t != null) {
            throw new RuntimeException("Error in task", t);
        }
    }

    @Override
    public void close() {
        shutdown();
        try {
            if (!awaitTermination(waitBeforeShutdown, TimeUnit.MINUTES)) {
                logger.logError("Task timed out.");
                shutdownNow();
                throw new RuntimeException("Task timed out.");
            }
        } catch (InterruptedException e) {
            logger.logError("Task interrupted.");
            shutdownNow();
            throw new RuntimeException("Task interrupted.", e);
        }
    }
}

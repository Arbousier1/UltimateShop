package cn.superiormc.ultimateshop.database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseExecutor {

    private static final long SHUTDOWN_WAIT_SECONDS = 30L;

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private static TrackingExecutor executor;

    private static TrackingExecutor createExecutor() {
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        return new TrackingExecutor(
                threadCount,
                threadCount,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread thread = new Thread(
                            runnable,
                            "UltimateShop-DB-" + THREAD_COUNTER.incrementAndGet()
                    );
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    public static synchronized void start() {
        if (executor == null || executor.isShutdown() || !executor.isAcceptingTasks()) {
            executor = createExecutor();
        }
    }

    public static synchronized ExecutorService getExecutor() {
        if (executor == null || executor.isShutdown() || !executor.isAcceptingTasks()) {
            throw new RejectedExecutionException("UltimateShop database executor is not running");
        }
        return executor;
    }

    public static void stopAcceptingTasks() {
        TrackingExecutor currentExecutor;
        synchronized (DatabaseExecutor.class) {
            currentExecutor = executor;
        }
        if (currentExecutor != null) {
            currentExecutor.stopAcceptingTasks();
        }
    }

    public static boolean isAcceptingTasks() {
        TrackingExecutor currentExecutor;
        synchronized (DatabaseExecutor.class) {
            currentExecutor = executor;
        }
        return currentExecutor != null
                && !currentExecutor.isShutdown()
                && currentExecutor.isAcceptingTasks();
    }

    public static void await() {
        TrackingExecutor currentExecutor;
        synchronized (DatabaseExecutor.class) {
            currentExecutor = executor;
        }
        if (currentExecutor != null
                && !currentExecutor.awaitTasks(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
            // 借鉴 craft-engine：等待超时后 dump 数据库线程栈，方便定位关服卡住的根因。
            StringBuilder report = new StringBuilder(
                    "UltimateShop database tasks did not finish within "
                            + SHUTDOWN_WAIT_SECONDS
                            + " seconds. Blocked thread stacks:\n"
            );
            Thread.getAllStackTraces().forEach((thread, stack) -> {
                if (thread.getName().startsWith("UltimateShop-DB")) {
                    report.append("Thread ").append(thread.getName()).append(" is blocked:\n");
                    for (StackTraceElement element : stack) {
                        report.append("  ").append(element).append('\n');
                    }
                }
            });
            cn.superiormc.ultimateshop.UltimateShop.instance.getLogger().warning(report.toString());
        }
    }

    public static synchronized void shutdown() {
        if (executor != null) {
            executor.stopAcceptingTasks();
            executor.shutdownNow();
            executor = null;
        }
    }

    private static class TrackingExecutor extends ThreadPoolExecutor {

        private final Object taskLock = new Object();

        private boolean acceptingTasks = true;

        private int pendingTasks;

        private TrackingExecutor(int corePoolSize,
                                 int maximumPoolSize,
                                 long keepAliveTime,
                                 TimeUnit unit,
                                 LinkedBlockingQueue<Runnable> workQueue,
                                 java.util.concurrent.ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        public void execute(Runnable command) {
            synchronized (taskLock) {
                if (!acceptingTasks) {
                    throw new RejectedExecutionException("UltimateShop database executor is shutting down");
                }
                pendingTasks++;
            }
            try {
                super.execute(() -> {
                    try {
                        command.run();
                    } finally {
                        synchronized (taskLock) {
                            pendingTasks--;
                            if (pendingTasks == 0) {
                                taskLock.notifyAll();
                            }
                        }
                    }
                });
            } catch (RejectedExecutionException exception) {
                synchronized (taskLock) {
                    pendingTasks--;
                    if (pendingTasks == 0) {
                        taskLock.notifyAll();
                    }
                }
                throw exception;
            }
        }

        private void stopAcceptingTasks() {
            synchronized (taskLock) {
                acceptingTasks = false;
            }
        }

        private boolean isAcceptingTasks() {
            synchronized (taskLock) {
                return acceptingTasks;
            }
        }

        private boolean awaitTasks(long timeout, TimeUnit unit) {
            long remainingNanos = unit.toNanos(timeout);
            long deadline = System.nanoTime() + remainingNanos;
            synchronized (taskLock) {
                while (pendingTasks > 0) {
                    if (remainingNanos <= 0L) {
                        return false;
                    }
                    try {
                        TimeUnit.NANOSECONDS.timedWait(taskLock, remainingNanos);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    remainingNanos = deadline - System.nanoTime();
                }
                return true;
            }
        }
    }
}

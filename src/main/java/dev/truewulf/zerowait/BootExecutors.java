package dev.truewulf.zerowait;

//? if >=1.21.2 {
import net.minecraft.TracingExecutor;
//?}

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public final class BootExecutors {
    private static volatile ForkJoinPool resourceLoadPool;

    private BootExecutors() {
    }

    private static int computeThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        long heapGb = Runtime.getRuntime().maxMemory() >> 30;
        int byCpu = Math.max(4, cores - 1);
        int byMem = (int) Math.min(16L, Math.max(4L, heapGb * 4L));
        return Math.max(2, Math.min(byCpu, byMem));
    }

//? if >=1.21.2 {
    public static java.util.concurrent.Executor resourceLoad(TracingExecutor vanilla, String name) {
        ZeroWaitConfig config = ZeroWaitConfig.get();
        if (!config.dedicatedReloadExecutor || !"resourceLoad".equals(name)) {
            return vanilla.forName(name);
        }
        ForkJoinPool pool = resourceLoadPool;
        if (pool == null) {
            synchronized (BootExecutors.class) {
                pool = resourceLoadPool;
                if (pool == null) {
                    int threads = computeThreads();
                    pool = new ForkJoinPool(threads, new BootThreadFactory(), new BootExceptionHandler(), true);
                    resourceLoadPool = pool;
                    ZeroWait.LOGGER.info("Dedicated resource load executor started with {} threads (cpus={}, heap={}GB)",
                            threads, Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() >> 30);
                }
            }
        }
        return pool;
    }
//?}

    private static final class BootThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setDaemon(true);
            thread.setName("ZeroWait-ResourceLoad-" + counter.incrementAndGet());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    private static final class BootExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            ZeroWait.LOGGER.error("Uncaught exception in {}", thread.getName(), throwable);
        }
    }
}

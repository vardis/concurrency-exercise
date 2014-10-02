package io.giorgos.concurrency;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Runs the concurrency for various input types. It must be run with assertions enabled in order
 * to report a wrong access pattern for Service.
 * <p>
 * java -ea -classpath "..." io.giorgos.concurrency.TimedExperiment
 */
public class TimedExperiment {

    public static final int N_THREADS = 10;
    private static final int MAX_CALLS = 1000000;

    private final Service controller;
    private final int iterations;
    private long startTime;

    static final Random RND = new Random();
    
    public TimedExperiment(int iterations, Service serviceController) {
        this.iterations = iterations;
        this.controller = serviceController;
    }

    public static void main(String[] args) throws InterruptedException {
        int repetitions = 100;
        for (int i = 0; i < repetitions; i++) {
            int times = RND.nextInt(MAX_CALLS);
            int addPermits = 1 + RND.nextInt(N_THREADS);
            int multPermits = 1 + N_THREADS - addPermits;

            Service srv = new VerifyingServiceDecorator(new SimpleService(), addPermits, multPermits);
            Service concSrv = new NonBlockingServiceController(srv, addPermits, multPermits);
            OneOpConstraintController mutexSrv = new OneOpConstraintController(concSrv);            
            
            runExperiment(mutexSrv, times);
        }
    }

    private static void runExperiment(Service controller, int times) throws InterruptedException {        
        try {
            TimedExperiment e = new TimedExperiment(times, controller);
            e.run();
            System.out.printf("N: %d,Time: %.3fs%n", times, e.getElapsedTime() / 1000.f);
        } catch (IllegalStateException e) {
            System.err.printf("Constraints violated.(%s)%n", e.getMessage());
        }

    }

    public void run() throws InterruptedException {
        startTime = System.currentTimeMillis();
        Set<Callable<Integer>> calls = createCalls();

        ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);
        List<Future<Integer>> futures = threadPool.invokeAll(calls);

        try {
            for (Future<Integer> f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof AssertionError) {
                        throw new IllegalStateException(e.getCause().getMessage(), e.getCause());
                    }
                }
            }
        } finally {
            threadPool.shutdown();
        }
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    class AddCall implements Callable<Integer> {

        private final int a;
        private final int b;

        private AddCall(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Integer call() throws Exception {
            int res = controller.add(a, b);
            assert res == (a + b);
            return res;
        }
    }

    class MultCall implements Callable<Integer> {

        private final int a;
        private final int b;

        private MultCall(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public Integer call() throws Exception {
            int res = controller.mult(a, b);
            assert res == (a * b);
            return res;
        }
    }

    private Set<Callable<Integer>> createCalls() {
        Set<Callable<Integer>> calls = new HashSet<>();
        for (int i = 0; i < iterations; i++) {
            Callable<Integer> c = Math.random() < 0.7 ? new AddCall(i, i + 1) : new MultCall(i, i + 1);
            calls.add(c);
        }
        return calls;
    }

}

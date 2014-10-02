package io.giorgos.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Decorates a Service instance to verify that the concurrency constraints are applied.
 *
 * Invariants:
 * a) concurrentAdds must be less or equal to maxAddClients
 * b) concurrentMults must be less or equal to maxMultClients
 * c) concurrentAdds must be zero when we enter mult
 * d) concurrentMults must be zero when we enter add
 *
 */
public class VerifyingServiceDecorator implements Service {

    // maintains the number of active adds, must be less or equal to maxAddClients
    private AtomicInteger concurrentAdds = new AtomicInteger(0);

    // maintains the number of active mults, must be less or equal to maxMultClients
    private AtomicInteger concurrentMults = new AtomicInteger(0);

    private Service service;
    private int maxAddClients;
    private int maxMultClients;

    public VerifyingServiceDecorator(Service service, int addClients, int multClients) {
        this.service = service;
        this.maxAddClients = addClients;
        this.maxMultClients = multClients;
    }

    @Override
    public int add(int a, int b) {
        assert concurrentMults.get() == 0;
        return runAndCheckLimits(maxAddClients, concurrentAdds, () -> service.add(a, b));
    }

    @Override
    public int mult(int a, int b) {
        assert concurrentAdds.get() == 0;
        return runAndCheckLimits(maxMultClients, concurrentMults, () -> service.mult(a, b));
    }

    private int runAndCheckLimits(int limit, AtomicInteger count, Callable<Integer> operation)  {
        int clients = count.incrementAndGet();
        assert  clients <= limit;
        assert  clients > 0;
        try {
            return operation.call();
        } catch (Exception e) {
            // ignore, Service does not throw Exception
            return 0;
        } finally {
            clients = count.decrementAndGet();
            assert  clients < limit;
            assert  clients >= 0;
        }
    }
}

package io.giorgos.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by giorgos on 23/09/14.
 */
public class NonBlockingServiceController implements Service {

    private NonBlockingGate addGate;
    private NonBlockingGate multGate;

    private Service service;

    public NonBlockingServiceController(Service srv, int maxAddPasses, int maxMultPasses) {
        this.service = srv;
        this.addGate = new NonBlockingGate(maxAddPasses);
        this.multGate = new NonBlockingGate(maxMultPasses);
    }

    @Override
    public int add(int a, int b) {
        addGate.acquire();
        try {
            return service.add(a, b);
        } finally {
            addGate.release();
        }
    }

    @Override
    public int mult(int a, int b) {
        multGate.acquire();
        try {
            return service.mult(a, b);
        } finally {
            multGate.release();
        }
    }

    private class NonBlockingGate {
        private AtomicInteger availablePasses;

        public NonBlockingGate(int passes) {
            this.availablePasses = new AtomicInteger(passes);
        }

        public void acquire() {
            while (true) {
                if (availablePasses.decrementAndGet() >= 0) {
                    break;
                } else {
                    availablePasses.incrementAndGet();
                }
            }
        }

        public void release() {
            availablePasses.incrementAndGet();
        }
    }
}

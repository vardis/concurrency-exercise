package io.giorgos.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by giorgos on 23/09/14.
 */
public class LockingServiceController implements Service {

    private Service service;

    private Gate addGate;
    private Gate multGate;

    public LockingServiceController(Service srv, int addPasses, int multPasses) {
        this.service = srv;
        this.addGate = new Gate(addPasses);
        this.multGate = new Gate(multPasses);
    }

    public int add(final int a, final int b) {
        addGate.acquire();
        try {
            return service.add(a, b);
        } finally {
            addGate.release();
        }
    }

    public int mult(final int a, final int b) {
        multGate.acquire();
        try {
            return service.mult(a, b);
        } finally {
            multGate.release();
        }
    }

    private class Gate {
        private final Lock lock = new ReentrantLock();
        private final Condition passAvailable = lock.newCondition();
        private int availablePasses;
        private int maxPasses;

        public Gate(int maxPasses) {
            this.maxPasses = maxPasses;
            this.availablePasses = maxPasses;
        }

        private void acquire() {
            lock.lock();
            assert (availablePasses >= 0 && availablePasses <= maxPasses);

            try {
                while (availablePasses == 0) {
                    passAvailable.awaitUninterruptibly();
                }
                availablePasses--;
            } finally {
                lock.unlock();
            }
        }

        private void release() {
            lock.lock();
            assert (availablePasses >= 0 && availablePasses <= maxPasses);
            try {
                availablePasses++;
                passAvailable.signal();
            } finally {
                lock.unlock();
            }
        }

    }
}

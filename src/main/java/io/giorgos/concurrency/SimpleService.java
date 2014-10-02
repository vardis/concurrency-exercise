package io.giorgos.concurrency;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trivial implementation of Service.
 */
public class SimpleService implements Service {

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public int mult(int a, int b) {
        return a * b;
    }
}

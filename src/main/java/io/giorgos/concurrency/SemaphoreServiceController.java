package io.giorgos.concurrency;

import java.util.concurrent.Semaphore;

/**
 * Created by giorgos on 23/09/14.
 */
public class SemaphoreServiceController implements Service {

    private Service service;

    private Semaphore addSema;
    private Semaphore multSema;

    public SemaphoreServiceController(Service srv, int addPasses, int multPasses) {
        this.service = srv;
        this.addSema = new Semaphore(addPasses);
        this.multSema = new Semaphore(multPasses);
    }

    public int add(final int a, final int b) {
        addSema.acquireUninterruptibly();
        try {
            return service.add(a, b);
        } finally {
            addSema.release();
        }
    }

    public int mult(final int a, final int b) {
        multSema.acquireUninterruptibly();
        try {
            return service.mult(a, b);
        } finally {
            multSema.release();
        }
    }

}

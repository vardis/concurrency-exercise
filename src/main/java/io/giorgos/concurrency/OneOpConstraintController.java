package io.giorgos.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by giorgos on 26/09/14.
 */
public class OneOpConstraintController implements Service {

    enum OP { NONE, ADD, MULT }
    private OP currentOp = OP.NONE;

    private Lock lock = new ReentrantLock();
    private Condition operationCanChange = lock.newCondition();

    private Service service;

    private int addersInProgress = 0;
    private int multsInProgress = 0;

    private int addersWaiting = 0;

    public OneOpConstraintController(Service service) {
        this.service = service;
    }

    public int add(int a, int b)  {
        lock.lock();
        try {
            addersWaiting++;
            while (currentOp == OP.MULT) {
                operationCanChange.awaitUninterruptibly();
            }
            addersWaiting--;
            currentOp = OP.ADD;
            addersInProgress++;
        } finally {
            lock.unlock();
        }

        try {
            return service.add(a, b);
        } finally {
            lock.lock();
            try {
                addersInProgress--;
                if (addersInProgress == 0) {
                    currentOp = OP.NONE;
                    operationCanChange.signalAll();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public int mult(int a, int b)  {
        lock.lock();
        try {
            while (addersWaiting > 0 || currentOp == OP.ADD) {
//                if (multsInProgress == 0 && addersWaiting > 0 && currentOp == OP.MULT) {
//                    currentOp = OP.NONE;
//                    operationCanChange.signalAll();
//                }
                operationCanChange.awaitUninterruptibly();
            }
            currentOp = OP.MULT;
            multsInProgress++;
        } finally {
            lock.unlock();
        }

        try {
            return service.mult(a, b);
        } finally {
            lock.lock();
            try {
                multsInProgress--;
                if (multsInProgress == 0) {
                    currentOp = OP.NONE;

                    // must signal all waiting threads in order to give a chance to the
                    // priority check to run and release any waiting adders
                    operationCanChange.signalAll();
                }
            } finally {
                lock.unlock();
            }

        }

    }
}

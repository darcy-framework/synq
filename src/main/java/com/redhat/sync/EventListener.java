package com.redhat.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EventListener<T> implements Event<T> {
    private CountDownLatch latch = new CountDownLatch(1);
    private T result;
    
    public void trigger(T result) {
        this.result = result;
        latch.countDown();
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        boolean timedOut;
        
        try {
            timedOut = !latch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // TODO: Remove event listener
            return null;
        }
        
        if (timedOut) {
            // TODO: Improve this
            // TODO: Remove event listener
            throw new RuntimeException(new TimeoutException());
        }
        
        return result;
    }
    
}

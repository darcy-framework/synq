package com.redhat.sync;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MultiEvent<T> implements Event<T> {
    private final Event<? extends T> original;
    protected final Event<? extends T> additional;
    private T firstResult;
    private Throwable throwable;
    private UncaughtExceptionHandler exceptionHandler = new MultiEventExceptionHandler();
    private CountDownLatch latch = new CountDownLatch(1);
    
    public MultiEvent(Event<? extends T> original, Event<? extends T> additional) {
        this.original = original;
        this.additional = additional;
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        Thread originalAwaiter = new Thread(() -> {
            finishWithResult(original.waitUpTo(timeout, unit));
        });
        
        Thread additionalAwaiter = new Thread(() -> {
            finishWithResult(additional.waitUpTo(timeout, unit));
        });
        
        originalAwaiter.setUncaughtExceptionHandler(exceptionHandler);
        additionalAwaiter.setUncaughtExceptionHandler(exceptionHandler);
        
        originalAwaiter.start();
        additionalAwaiter.start();
        
        boolean timedOut = false;
        
        try {
            timedOut = !latch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        
        // We don't know which finished first so interrupt them both; it's harmless.
        originalAwaiter.interrupt();
        additionalAwaiter.interrupt();
        
        if (timedOut) {
            // TODO: Make this better
            throw new RuntimeException(new TimeoutException());
        }
        
        if (throwable != null) {
            // TODO: Make this better
            throw new RuntimeException(throwable);
        }
        
        return firstResult;
    }
    
    private synchronized void finishWithResult(T result) {
        if (firstResult == null) {
            firstResult = result;
            latch.countDown();
        }
    }
    
    private synchronized void finishWithException(Throwable t) {
        if (throwable == null) {
            throwable = t;
            latch.countDown();
        }
    }
    
    private class MultiEventExceptionHandler implements UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            finishWithException(e);
        }
        
    }
}

package com.redhat.sync;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultPollEvent<T> implements PollEvent<T> {
    private final Condition<T> condition;
    private long pollingInterval = 1;
    private TimeUnit pollingUnit = TimeUnit.SECONDS;
    private Set<Class<? extends Exception>> ignoredExceptions = new HashSet<>();
    
    public DefaultPollEvent(Condition<T> condition) {
        this.condition = condition;
    }
    
    public DefaultPollEvent<T> pollingEvery(long pollingInterval, TimeUnit pollingUnit) {
        this.pollingInterval = pollingInterval;
        this.pollingUnit = pollingUnit;
        
        return this;
    }
    
    public DefaultPollEvent<T> ignoring(Class<? extends Exception> exception) {
        ignoredExceptions.add(exception);
        return this;
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        boolean met = false;
        T lastResult = null;
        long timeoutTime = now() + MILLISECONDS.convert(timeout, unit);
        
        while (!met) {
            try {
                met = condition.isMet();
                lastResult = condition.lastResult();
                
                if (met) {
                    break;
                }
            } catch (Exception e) {
                throwIfNotIgnored(e);
            }
            
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            
            if (now() >= timeoutTime) {
                // TODO: Improve these exceptions
                throw new RuntimeException(new TimeoutException());
            }
            
            try {
                Thread.sleep(MILLISECONDS.convert(pollingInterval, pollingUnit));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        return lastResult;
    }
    
    private void throwIfNotIgnored(Throwable t) throws RuntimeException {
        for (Class<? extends Exception> ignoredException : ignoredExceptions) {
            // The getCause check may not be necessary
            if (ignoredException.isInstance(t) || ignoredException.isInstance(t.getCause())) {
                return;
            }
        }
        
        if (t instanceof Error) {
            throw (Error) t;
        } else if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else {
            throw new RuntimeException(t);
        }
    }
    
    private long now() {
        return System.currentTimeMillis();
    }
    
}

package com.redhat.sync;

import java.util.concurrent.TimeUnit;

public class ExceptionalPollEvent<T> extends ExceptionalEvent<T> implements PollEvent<T> {
    
    public ExceptionalPollEvent(PollEvent<?> original, Throwable throwable) {
        super(original, throwable);
    }

    @SuppressWarnings("unchecked")
    @Override
    public PollEvent<T> pollingEvery(long pollingInterval, TimeUnit pollingUnit) {
        ((PollEvent<T>) original).pollingEvery(pollingInterval, pollingUnit);
        
        return this;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public PollEvent<T> ignoring(Class<? extends Exception> exception) {
        ((PollEvent<T>) original).ignoring(exception);
        
        return this;
    }
    
}

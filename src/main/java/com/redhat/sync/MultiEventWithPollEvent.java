package com.redhat.sync;

import java.util.concurrent.TimeUnit;

public class MultiEventWithPollEvent<T> extends MultiEvent<T> implements PollEvent<T> {
    
    public MultiEventWithPollEvent(Event<? extends T> original, PollEvent<? extends T> additional) {
        super(original, additional);
    }

    @SuppressWarnings("unchecked")
    @Override
    public PollEvent<T> pollingEvery(long pollingInterval, TimeUnit pollingUnit) {
        ((PollEvent<T>) additional).pollingEvery(pollingInterval, pollingUnit);
        
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public PollEvent<T> ignoring(Class<? extends Exception> exception) {
        ((PollEvent<T>) additional).ignoring(exception);
        
        return this;
    }
}

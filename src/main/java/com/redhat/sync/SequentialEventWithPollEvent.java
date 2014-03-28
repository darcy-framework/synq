package com.redhat.sync;

import java.util.concurrent.TimeUnit;

public class SequentialEventWithPollEvent<T> extends SequentialEvent<T> implements PollEvent<T> {
    
    public SequentialEventWithPollEvent(Event<?> original, PollEvent<? extends T> additional) {
        super(original, additional);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public PollEvent<T> after(Runnable action) {
        return new SequentialEventWithPollEvent<T>(original, 
                new SequentialEventWithPollEvent<>((t, u) -> {action.run(); return null;}, 
                        (PollEvent<T>) additional));
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

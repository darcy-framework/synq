package com.redhat.synq;

import java.util.concurrent.TimeUnit;

public class ForwardingPollEvent<T> implements PollEvent<T> {
    private final PollEvent<T> event;
    
    public ForwardingPollEvent(PollEvent<T> pollEvent) {
        this.event = pollEvent;
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        return event.waitUpTo(timeout, unit);
    }
    
    @Override
    public PollEvent<T> pollingEvery(long pollingInterval, TimeUnit pollingUnit) {
        return event.pollingEvery(pollingInterval, pollingUnit);
    }
    
    @Override
    public PollEvent<T> ignoring(Class<? extends Exception> exception) {
        return event.ignoring(exception);
    }
    
}

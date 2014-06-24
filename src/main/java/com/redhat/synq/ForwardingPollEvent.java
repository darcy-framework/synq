package com.redhat.synq;

import java.time.Duration;

public class ForwardingPollEvent<T> implements PollEvent<T> {
    private final PollEvent<T> event;
    
    public ForwardingPollEvent(PollEvent<T> pollEvent) {
        this.event = pollEvent;
    }
    
    @Override
    public T waitUpTo(Duration duration) {
        return event.waitUpTo(duration);
    }
    
    @Override
    public PollEvent<T> pollingEvery(Duration pollingInterval) {
        return event.pollingEvery(pollingInterval);
    }
    
    @Override
    public PollEvent<T> ignoring(Class<? extends Exception> exception) {
        return event.ignoring(exception);
    }
    
}

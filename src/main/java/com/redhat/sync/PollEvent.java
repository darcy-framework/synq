package com.redhat.sync;

import java.util.concurrent.TimeUnit;

public interface PollEvent<T> extends Event<T> {
    PollEvent<T> pollingEvery(long pollingInterval, TimeUnit pollingUnit);
    PollEvent<T> ignoring(Class<? extends Exception> exception);
    
    @Override
    default PollEvent<T> after(Runnable action) {
        return new SequentialEventWithPollEvent<>((t, u) -> {action.run(); return null;}, this);
    }
    
}

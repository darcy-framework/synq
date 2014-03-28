package com.redhat.sync;

import java.util.concurrent.TimeUnit;

public class ForwardingEvent<T> implements Event<T> {
    private final Event<? extends T> event;
    
    public ForwardingEvent(Event<? extends T> event) {
        this.event = event;
    }

    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        return event.waitUpTo(timeout, unit);
    }
}

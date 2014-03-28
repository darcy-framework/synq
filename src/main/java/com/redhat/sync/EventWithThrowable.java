package com.redhat.sync;

public class EventWithThrowable<T> extends ForwardingEvent<T> implements HasThrowable {
    private final Throwable exception;
    
    public EventWithThrowable(Event<? extends T> event, Throwable exception) {
        super(event);
        this.exception = exception;
    }

    @Override
    public Throwable throwable() {
        return exception;
    }
}

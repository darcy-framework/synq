package com.redhat.sync;

import java.util.concurrent.TimeUnit;

public class SequentialEvent<T> implements Event<T> {
    protected final Event<?> original;
    protected Event<? extends T> additional;
    
    public SequentialEvent(Event<?> original, Event<? extends T> additional) {
        this.original = original;
        this.additional = additional;
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        // TODO: This waits for up to timeout x2 total... is this appropriate?
        
        original.waitUpTo(timeout, unit);
        
        return additional.waitUpTo(timeout, unit);
    }
    
    @Override
    public Event<T> after(Runnable action) {
        return new SequentialEvent<>(original, 
                new SequentialEvent<>((t, u) -> {action.run(); return null;}, additional));
    }
    
    @Override
    public Event<T> or(Event<? extends T> event) {
        return new SequentialEvent<>(original, new MultiEvent<T>(additional, event));
    }
    
    @Override
    public PollEvent<T> or(Condition<? extends T> condition) {
        return new SequentialEventWithPollEvent<>(original, 
                new MultiEventWithPollEvent<T>(additional, condition.asEvent()));
    }
    
    @Override
    public ExceptionalEventFactory<T> throwing(Throwable throwable) {
        return new ExceptionalEventFactory<T>(throwable, this);
    }
    
    @Override
    public Event<T> failIf(Event<?> failEvent) {
        Throwable throwable;
        
        if (failEvent instanceof HasThrowable) {
            throwable = ((HasThrowable) failEvent).throwable();
        } else {
            throwable = new FailEventException(failEvent);
        }
        
        return new SequentialEvent<>(original, 
                new MultiEvent<T>(additional, new ExceptionalEvent<T>(failEvent, throwable)));
    }
    
    @Override
    public PollEvent<T> failIf(Condition<?> failCondition) {
        Throwable throwable;
        PollEvent<?> failEvent = failCondition.asEvent();
        
        if (failCondition instanceof HasThrowable) {
            throwable = ((HasThrowable) failCondition).throwable();
        } else {
            throwable = new FailEventException(failEvent);
        }
        
        return new SequentialEventWithPollEvent<T>(original, 
                new MultiEventWithPollEvent<T>(additional, 
                        new ExceptionalPollEvent<T>(failEvent, throwable)));
    }
}

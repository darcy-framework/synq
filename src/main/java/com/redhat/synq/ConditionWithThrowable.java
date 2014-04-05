package com.redhat.synq;

public class ConditionWithThrowable<T> extends ForwardingCondition<T> implements HasThrowable {
    private final Throwable throwable;
    
    public ConditionWithThrowable(Condition<? extends T> condition, Throwable throwable) {
        super(condition);
        this.throwable = throwable;
    }

    @Override
    public Throwable throwable() {
        return throwable;
    }
    
}

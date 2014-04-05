package com.redhat.synq;

public class ForwardingCondition<T> implements Condition<T> {
    private final Condition<? extends T> condition;
    
    public ForwardingCondition(Condition<? extends T> condition) {
        this.condition = condition;
    }
    
    @Override
    public boolean isMet() throws Exception {
        return condition.isMet();
    }
    
    @Override
    public T lastResult() {
        return condition.lastResult();
    }
}

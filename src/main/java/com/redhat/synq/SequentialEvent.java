/*
 Copyright 2014 Red Hat, Inc. and/or its affiliates.

 This file is part of synq.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redhat.synq;

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

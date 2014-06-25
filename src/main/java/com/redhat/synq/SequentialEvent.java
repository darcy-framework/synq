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

import java.time.Duration;
import java.time.Instant;

public class SequentialEvent<T> implements Event<T> {
    protected final Event<?> original;
    protected Event<? extends T> additional;
    
    public SequentialEvent(Event<?> original, Event<? extends T> additional) {
        this.original = original;
        this.additional = additional;
    }
    
    @Override
    public T waitUpTo(Duration duration) {
        Instant start = Instant.now();

        try {
            original.waitUpTo(duration);

            Duration remaining = duration.minus(Duration.between(start, Instant.now()));

            return additional.waitUpTo(remaining);
        } catch (TimeoutException t) {
            throw new TimeoutException(this, duration);
        }
    }
    
    @Override
    public Event<T> after(Runnable action) {
        return new SequentialEvent<>(original, 
                new SequentialEvent<>(d -> {
                    action.run();
                    return null;
                }, additional));
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
    public FailEvent<T> failIf(Event<?> failEvent) {
        return new SequentialEventWithFailEvent<>(original, 
                new MultiEventWithFailEvent<T>(additional, new ForwardingFailEvent<T>(failEvent)));
    }
    
    @Override
    public FailPollEvent<T> failIf(Condition<?> failCondition) {
        PollEvent<?> failEvent = failCondition.asEvent();
        
        return new SequentialEventWithFailPollEvent<T>(original, 
                new MultiEventWithFailPollEvent<T>(additional, 
                        new ForwardingFailPollEvent<T>(failEvent)));
    }

    @Override
    public String toString() {
        return original + " and then " + additional;
    }
}

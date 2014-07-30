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
import java.util.Objects;
import java.util.function.Supplier;

public class SequentialEvent<T> implements Event<T> {
    protected final Event<?> original;
    protected final Event<? extends T> additional;
    protected final TimeKeeper timeKeeper;
    
    public SequentialEvent(Event<?> original, Event<? extends T> additional) {
        this(original, additional, TimeKeeper.systemTimeKeeper());
    }

    public SequentialEvent(Event<?> original, Event<? extends T> additional,
            TimeKeeper timeKeeper) {
        this.original = Objects.requireNonNull(original, "original");
        this.additional = Objects.requireNonNull(additional, "additional");
        this.timeKeeper = Objects.requireNonNull(timeKeeper, "timeKeeper");
    }
    
    @Override
    public T waitUpTo(Duration duration) {
        Instant start = timeKeeper.instant();

        try {
            original.waitUpTo(duration);

            Duration remaining = duration.minus(Duration.between(start, timeKeeper.instant()));

            return additional.waitUpTo(remaining);
        } catch (TimeoutException t) {
            throw new TimeoutException(this, duration);
        }
    }

    @Override
    public Event<T> describedAs(Supplier<String> description) {
        additional.describedAs(description);

        return this;
    }

    @Override
    public String toString() {
        return original + "\nand then " + additional;
    }

    @Override
    public Event<T> after(Runnable action) {
        return new SequentialEvent<T>(original,
                new SequentialEvent<T>(new ActionEvent(action), additional, timeKeeper),
                timeKeeper);
    }
    
    @Override
    public Event<T> or(Event<? extends T> event) {
        return new SequentialEvent<>(original, new MultiEvent<T>(additional, event), timeKeeper);
    }
    
    @Override
    public PollEvent<T> or(Condition<? extends T> condition) {
        return new SequentialEventWithPollEvent<>(original,
                new MultiEventWithPollEvent<T>(additional, condition.asEvent(timeKeeper)));
    }
    
    @Override
    public FailEvent<T> failIf(Event<?> failEvent) {
        return new SequentialEventWithFailEvent<T>(original,
                new MultiEventWithFailEvent<T>(additional, new ForwardingFailEvent<T>(failEvent)));
    }
    
    @Override
    public FailPollEvent<T> failIf(Condition<?> failCondition) {
        PollEvent<?> failEvent = failCondition.asEvent(timeKeeper);
        
        return new SequentialEventWithFailPollEvent<T>(original, 
                new MultiEventWithFailPollEvent<T>(additional, 
                        new ForwardingFailPollEvent<T>(failEvent)));
    }
}

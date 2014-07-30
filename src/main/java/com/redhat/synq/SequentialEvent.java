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
    protected final Event<?> first;
    protected final Event<? extends T> second;
    protected final TimeKeeper timeKeeper;
    
    public SequentialEvent(Event<?> first, Event<? extends T> second) {
        this(first, second, TimeKeeper.systemTimeKeeper());
    }

    public SequentialEvent(Event<?> first, Event<? extends T> second,
            TimeKeeper timeKeeper) {
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
        this.timeKeeper = Objects.requireNonNull(timeKeeper, "timeKeeper");
    }
    
    @Override
    public T waitUpTo(Duration duration) {
        Instant start = timeKeeper.instant();

        try {
            first.waitUpTo(duration);

            Duration remaining = duration.minus(Duration.between(start, timeKeeper.instant()));

            return second.waitUpTo(remaining);
        } catch (TimeoutException t) {
            throw new TimeoutException(this, duration);
        }
    }

    @Override
    public Event<T> describedAs(Supplier<String> description) {
        second.describedAs(description);

        return this;
    }

    @Override
    public String toString() {
        return first + "\nand then " + second;
    }

    @Override
    public Event<T> after(Runnable action) {
        return new SequentialEvent<T>(first,
                new SequentialEvent<T>(new ActionEvent(action), second, timeKeeper),
                timeKeeper);
    }
    
    @Override
    public Event<T> or(Event<? extends T> event) {
        return new SequentialEvent<>(first, new MultiEvent<T>(second, event), timeKeeper);
    }
    
    @Override
    public PollEvent<T> or(Condition<? extends T> condition) {
        return new SequentialEventWithPollEvent<>(first,
                new MultiEventWithPollEvent<T>(second, condition.asEvent(timeKeeper)));
    }
    
    @Override
    public FailEvent<T> failIf(Event<?> failEvent) {
        return new SequentialEventWithFailEvent<T>(first,
                new MultiEventWithFailEvent<T>(second, new ForwardingFailEvent<T>(failEvent)));
    }
    
    @Override
    public FailPollEvent<T> failIf(Condition<?> failCondition) {
        PollEvent<?> failEvent = failCondition.asEvent(timeKeeper);
        
        return new SequentialEventWithFailPollEvent<T>(first,
                new MultiEventWithFailPollEvent<T>(second,
                        new ForwardingFailPollEvent<T>(failEvent)));
    }
}

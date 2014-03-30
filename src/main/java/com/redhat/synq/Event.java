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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.hamcrest.Matcher;

public interface Event<T> {
    /**
     * Block the thread until the event has occurred.
     * 
     * @param timeout
     * @param unit
     * @return The result of the event.
     */
    T waitUpTo(long timeout, TimeUnit unit);
    
    /**
     * Perform some action before waiting. Will always run before waiting begins, unless after an
     * {@link #andThenExpect(Event)}, in which case the action will run, and the first set of events
     * will be awaited. Once that set is satisfied, then any actions defined after an 
     * {@link #andThenExpect(Event)} will run, likewise before that following set of conditions are 
     * awaited.
     * 
     * @param action
     * @return
     */
    default Event<T> after(Runnable action) {
        return new SequentialEvent<>((t, u) -> {action.run(); return null;}, this);
    }
    
    /**
     * Compose a new event that will wait until the first of two events: the original or the event
     * passed.
     * 
     * @param event
     * @return
     */
    default Event<T> or(Event<? extends T> event) {
        return new MultiEvent<T>(this, event);
    }
    
    /**
     * Compose a new event that will wait until the first of two events: the original or the event
     * passed.
     * 
     * @param condition
     *            Converted to a {@link PollEvent} by polling at some regular interval (which you
     *            can specify fluently, or trust the defaults).
     * @return
     */
    default PollEvent<T> or(Condition<? extends T> condition) {
        return new MultiEventWithPollEvent<T>(this, condition.asEvent());
    }
    
    default PollEvent<T> or(Callable<? extends T> returnsTrueOrNonNull) {
        return or(HamcrestCondition.isTrueOrNonNull(returnsTrueOrNonNull));
    }
    
    default PollEvent<T> or(T item, Predicate<? super T> predicate) {
        return or(() -> item, predicate);
    }
    
    default PollEvent<T> or(Callable<T> item, Predicate<? super T> predicate) {
        return or(Condition.match(item, predicate));
    }
    
    default PollEvent<T> or(T item, Matcher<? super T> matcher) {
        return or(() -> item, matcher);
    }
    
    default PollEvent<T> or(Callable<T> item, Matcher<? super T> matcher) {
        return or(new HamcrestCondition<>(item, matcher));
    }
    
    default ExceptionalEventFactory<T> throwing(Throwable throwable) {
        return new ExceptionalEventFactory<T>(throwable, this);
    }
    
    /**
     * If this event occurs before the others, then an exception will be thrown as defined by the
     * event itself (via {@link #throwing(Throwable)}). If no exception is associated with the 
     * event, a generic {@link FailEventException} will be thrown.
     * 
     * @param failEvent
     * @return
     */
    default Event<T> failIf(Event<?> failEvent) {
        Throwable throwable;
        
        if (failEvent instanceof HasThrowable) {
            throwable = ((HasThrowable) failEvent).throwable();
        } else {
            throwable = new FailEventException(failEvent);
        }
        
        return new MultiEvent<T>(this, new ExceptionalEvent<T>(failEvent, throwable));
    }
    
    default PollEvent<T> failIf(Condition<?> failCondition) {
        Throwable throwable;
        PollEvent<?> failEvent = failCondition.asEvent();
        
        if (failCondition instanceof HasThrowable) {
            throwable = ((HasThrowable) failCondition).throwable();
        } else {
            throwable = new FailEventException(failEvent);
        }
        
        return new MultiEventWithPollEvent<T>(this, 
                new ExceptionalPollEvent<T>(failEvent, throwable));
    }
    
    default PollEvent<T> failIf(Callable<?> returnsTrueOrNonNull) {
        return failIf(HamcrestCondition.isTrueOrNonNull(returnsTrueOrNonNull));
    }
    
    default <R> PollEvent<T> failIf(R item, Predicate<? super R> predicate) {
        return failIf(() -> item, predicate);
    }
    
    default <R> PollEvent<T> failIf(Callable<R> item, Predicate<? super R> predicate) {
        return failIf(Condition.match(item, predicate));
    }
    
    default <R> PollEvent<T> failIf(R item, Matcher<? super R> matcher) {
        return failIf(() -> item, matcher);
    }
    
    default <R> PollEvent<T> failIf(Callable<R> item, Matcher<? super R> matcher) {
        return failIf(new HamcrestCondition<>(item, matcher));
    }
    
    /**
     * Causes the previous actions to be run and events to be awaited before awaiting the next event
     * passed. If the next event goes on to define some action(s) to run before waiting, those will
     * only run after the previous set of events is awaited (those that came before the 
     * {@link #andThenExpect(Event)} call).
     * 
     * @param nextEvent
     * @return
     */
    default <U> Event<U> andThenExpect(Event<U> nextEvent) {
        return new SequentialEvent<U>(this, nextEvent);
    }
    
    /**
     * Causes the previous actions to be run and events to be awaited before awaiting the next event
     * passed. If the next event goes on to define some action(s) to run before waiting, those will
     * only run after the previous set of events is awaited (those that came before the 
     * {@link #andThenExpect(Condition)} call).
     * 
     * @param condition
     * @return
     */
    default <U> PollEvent<U> andThenExpect(Condition<U> condition) {
        return new SequentialEventWithPollEvent<U>(this, condition.asEvent());
    }
    
    /**
     * @see #andThenExpect(Condition)
     * @param toReturnTrueOrNonNull
     * @return
     */
    default <U> PollEvent<U> andThenExpect(Callable<U> toReturnTrueOrNonNull) {
        return andThenExpect(HamcrestCondition.isTrueOrNonNull(toReturnTrueOrNonNull));
    }
    
    /**
     * @see #andThenExpect(Condition)
     * @param item
     * @param predicate
     * @return
     */
    default <U> PollEvent<U> andThenExpect(U item, Predicate<? super U> predicate) {
        return andThenExpect(() -> item, predicate);
    }
    
    /**
     * @see #andThenExpect(Condition)
     * @param item
     * @param predicate
     * @return
     */
    default <U> PollEvent<U> andThenExpect(Callable<U> item, Predicate<? super U> predicate) {
        return andThenExpect(Condition.match(item, predicate));
    }
    
    /**
     * @see #andThenExpect(Condition)
     * @param item
     * @param matcher
     * @return
     */
    default <U> PollEvent<U> andThenExpect(U item, Matcher<? super U> matcher) {
        return andThenExpect(() -> item, matcher);
    }
    
    /**
     * @see #andThenExpect(Condition)
     * @param item
     * @param matcher
     * @return
     */
    default <U> PollEvent<U> andThenExpect(Callable<U> item, Matcher<? super U> matcher) {
        return andThenExpect(new HamcrestCondition<>(item, matcher));
    }
    
    class ExceptionalEventFactory<V> {
        private final Throwable throwable;
        private final Event<V> original;
        
        ExceptionalEventFactory(Throwable throwable, Event<V> original) {
            this.throwable = throwable;
            this.original = original;
        }
        
        public Event<V> when(Event<?> isTriggered) {
            return original.failIf(new EventWithThrowable<Object>(isTriggered, throwable));
        }
        
        public PollEvent<V> when(Condition<?> isMet) {
            return original.failIf(isMet);
        }
        
        public PollEvent<V> when(Callable<?> returnsTrueOrNonNull) {
            return when(HamcrestCondition.isTrueOrNonNull(returnsTrueOrNonNull));
        }
        
        public <R> PollEvent<V> when(R item, Predicate<? super R> predicate) {
            return when(() -> item, predicate);
        }
        
        public <R> PollEvent<V> when(Callable<R> item, Predicate<? super R> predicate) {
            return when(Condition.match(item, predicate));
        }
        
        public <R> PollEvent<V> when(R item, Matcher<? super R> matcher) {
            return when(() -> item, matcher);
        }
        
        public <R> PollEvent<V> when(Callable<R> item, Matcher<? super R> matcher) {
            return when(new HamcrestCondition<>(item, matcher));
        }
    }
}

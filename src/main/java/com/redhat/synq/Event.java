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

import org.hamcrest.Matcher;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * An Event represents something that may happen in the future, and can be awaited. Awaiting that 
 * Event (via {@link #waitUpTo(long, ChronoUnit)}) returns some result associated with the Event
 * you were waiting for.
 * @param <T> The type of the result of this Event.
 */
public interface Event<T> {
    /**
     * Block the thread until the event has occurred.
     * 
     * @param timeout
     * @param unit
     * @return The result of the event.
     */
    default T waitUpTo(long timeout, ChronoUnit unit) {
        return waitUpTo(Duration.of(timeout, unit));
    }

    T waitUpTo(Duration duration);
    
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
        return new SequentialEvent<>(d -> {
            action.run();
            return null;
        }, this);
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
        return or(new Callable<T>() {
            
            @Override
            public T call() throws Exception {
                return item;
            }
            
        }, predicate);
    }
    
    default PollEvent<T> or(Callable<T> item, Predicate<? super T> predicate) {
        return or(Condition.match(item, predicate));
    }
    
    default PollEvent<T> or(T item, Matcher<? super T> matcher) {
        return or(new Callable<T>() {
            
            @Override
            public T call() throws Exception {
                return item;
            }
            
        }, matcher);
    }
    
    default PollEvent<T> or(Callable<T> item, Matcher<? super T> matcher) {
        return or(new HamcrestCondition<>(item, matcher));
    }
    
    /**
     * If this event occurs before the others, then an exception will be thrown as defined by the
     * Throwable parameter.
     * 
     * @param failEvent
     * @param throwable
     * @return
     */
    default FailEvent<T> failIf(Event<?> failEvent) {
        return new MultiEventWithFailEvent<T>(this, new ForwardingFailEvent<T>(failEvent));
    }
    
    default FailPollEvent<T> failIf(Condition<?> failCondition) {
        PollEvent<?> failEvent = failCondition.asEvent();
        
        return new MultiEventWithFailPollEvent<T>(this, new ForwardingFailPollEvent<T>(failEvent));
    }
    
    default FailPollEvent<T> failIf(Callable<?> returnsTrueOrNonNull) {
        return failIf(HamcrestCondition.isTrueOrNonNull(returnsTrueOrNonNull));
    }
    
    default <R> FailPollEvent<T> failIf(R item, Predicate<? super R> predicate) {
        return failIf(new Callable<R>() {
            
            @Override
            public R call() throws Exception {
                return item;
            }
            
        }, predicate);
    }
    
    default <R> FailPollEvent<T> failIf(Callable<R> item, Predicate<? super R> predicate) {
        return failIf(Condition.match(item, predicate));
    }
    
    default <R> FailPollEvent<T> failIf(R item, Matcher<? super R> matcher) {
        return failIf(new Callable<R>() {
            
            @Override
            public R call() throws Exception {
                return item;
            }
            
        }, matcher);
    }
    
    default <R> FailPollEvent<T> failIf(Callable<R> item, Matcher<? super R> matcher) {
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
        return andThenExpect(new Callable<U>() {
            
            @Override
            public U call() throws Exception {
                return item;
            }
            
        }, predicate);
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
        return andThenExpect(new Callable<U>() {
            
            @Override
            public U call() throws Exception {
                return item;
            }
            
        }, matcher);
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
}

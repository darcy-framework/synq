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
import java.util.function.Supplier;

/**
 * An Event represents something that may happen in the future, and can be awaited. Awaiting that
 * Event (via {@link #waitUpTo(Duration)}) returns some result associated with the Event you were
 * waiting for.
 *
 * <p>Events have several default methods for taking an existing an event and composing another
 * from it. Many of them are simply overrides of others for convenient syntax. Here are the core
 * default methods that are ultimately used by the various overrides:
 *
 * <ul>
 * <li>{@link #after(Runnable)} - When this Event is awaited, it will first run the action.</li>
 * <li>{@link #failIf(Event)} - While waiting for the original event, throw an exception if this
 * other event occurs first.</li>
 * <li>{@link #or(Event)} - Returns an Event that will trigger when either of these Events occurs,
 * returning the value associated with whichever happened first.</li>
 * <li>{@link #andThenExpect(Event)} - Returns an Event that will wait for the the original, then
 * wait for the second, in that order.</li>
 * </ul>
 *
 * @param <T> The type of the result of this Event.
 */
public interface Event<T> {
    /**
     * Block the thread until the event has occurred. Will block the thread for a maximum of the
     * specified duration, at which point a {@link com.redhat.synq.TimeoutException} will be
     * thrown.
     *
     * <p>If a wait operation should break, you can interrupt the thread currently waiting. That is,
     * Event implementations must respond to thread interrupts by ceasing to block the thread and
     * returning null. Other Event types use interrupts when they no longer care whether or when
     * a particular Event is satisfied. Interrupts are a means to tell Events to stop doing
     * unnecessary work.
     *
     * @return The "result" of the event, which varies per implementation. If there is some value
     * that is being examined, generally when the value meets the expected criteria it is that value
     * that should be returned.
     * @throws com.redhat.synq.TimeoutException if the specified amount of time passes before the
     * Event occurs.
     */
    T waitUpTo(Duration duration);

    /**
     * Most Event objects are constructed in a such a way that it is difficult to programmatically
     * determine an appropriate description to return for {@link #toString()}. The description in
     * this message will change the output of {@link #toString()} to <code>description</code>.
     *
     * <p>The output of {@link #toString()} is what is used in
     * {@link com.redhat.synq.TimeoutException TimeoutExceptions}, in the form, "Timed out after
     * ${duration} waiting for ${event.toString()}."
     *
     * @param description A description that works well with a timeout message. That is, it should
     * fit grammatically in the sentence, "Timed out after ${duration} waiting for ${description}."
     */
    default Event<T> describedAs(String description) {
        return describedAs(() -> description);
    }

    /**
     * Most Event objects are constructed in a such a way that it is difficult to programmatically
     * determine an appropriate description to return for {@link #toString()}. The description in
     * this message will change the output of {@link #toString()} to <code>description</code>.
     *
     * <p>The output of {@link #toString()} is what is used in
     * {@link com.redhat.synq.TimeoutException TimeoutExceptions}, in the form, "Timed out after
     * ${duration} waiting for ${event.toString()}."
     *
     * @param description A description that works well with a timeout message. That is, it should
     * fit grammatically in the sentence, "Timed out after ${duration} waiting for ${description}."
     */
    Event<T> describedAs(Supplier<String> description);

    /**
     * Returns the description of the event. Depending on the implementation, a readable description
     * may be constructable programmatically, however most of the time it is a good idea to assign
     * a description manually for an event via {@link #describedAs(String)}.
     */
    @Override
    String toString();

    // Default methods

    /**
     * Block the thread until the event has occurred. Will block the thread for a maximum of the
     * specified duration, at which point a {@link com.redhat.synq.TimeoutException} will be
     * thrown.
     *
     * <p>If a wait operation should break, you can interrupt the thread currently waiting. That is,
     * Event implementations must respond to thread interrupts by ceasing to block the thread and
     * returning null. Other Event types use interrupts when they no longer care whether or when
     * a particular Event is satisfied. Interrupts are a means to tell Events to stop doing
     * unnecessary work.
     *
     * @return The "result" of the event, which varies per implementation. If there is some value
     * that is being examined, generally when the value meets the expected criteria it is that value
     * that should be returned.
     * @throws com.redhat.synq.TimeoutException if the specified amount of time passes before the
     * Event occurs.
     */
    default T waitUpTo(long timeout, ChronoUnit unit) {
        return waitUpTo(Duration.of(timeout, unit));
    }

    /**
     * Perform some action before waiting. Will always run before waiting begins, unless after an
     * {@link #andThenExpect(Event)}, in which case the action will run, and the first set of events
     * will be awaited. Once that set is satisfied, then any actions defined after an {@link
     * #andThenExpect(Event)} will run, likewise before that following set of conditions are
     * awaited.
     */
    default Event<T> after(Runnable action) {
        return new SequentialEvent<>(new ActionEvent(action), this);
    }

    /**
     * Compose a new event that will wait until the first of two events: the original or the event
     * passed.
     */
    default Event<T> or(Event<? extends T> event) {
        return new MultiEvent<T>(this, event);
    }

    /**
     * Compose a new event that will wait until the first of two events: the original or the event
     * passed.
     *
     * @param condition Converted to a {@link PollEvent} by polling at some regular interval (which
     * you can specify fluently, or trust the defaults).
     */
    default PollEvent<T> or(Condition<? extends T> condition) {
        return new MultiEventWithPollEvent<T>(this, condition.asEvent());
    }

    default PollEvent<T> or(Callable<? extends T> returnsTrueOrNonNull) {
        return or(HamcrestCondition.isTrueOrNonNull(returnsTrueOrNonNull));
    }

    default PollEvent<T> or(T item, CheckedPredicate<? super T> predicate) {
        return orCallTo(() -> item, predicate);
    }

    default PollEvent<T> orCallTo(Callable<T> item, CheckedPredicate<? super T> predicate) {
        return or(Condition.matchCallTo(item, predicate));
    }

    default PollEvent<T> or(T item, Matcher<? super T> matcher) {
        return orCallTo(() -> item, matcher);
    }

    default PollEvent<T> orCallTo(Callable<T> item, Matcher<? super T> matcher) {
        return or(new HamcrestCondition<>(item, matcher));
    }

    /**
     * If this event occurs before the others, then an exception will be thrown as defined by the
     * Throwable parameter.
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

    default <R> FailPollEvent<T> failIf(R item, CheckedPredicate<? super R> predicate) {
        return failIfCallTo(() -> item, predicate);
    }

    default <R> FailPollEvent<T> failIfCallTo(Callable<R> item, CheckedPredicate<? super R> predicate) {
        return failIf(Condition.matchCallTo(item, predicate));
    }

    default <R> FailPollEvent<T> failIf(R item, Matcher<? super R> matcher) {
        return failIfCallTo(() -> item, matcher);
    }

    default <R> FailPollEvent<T> failIfCallTo(Callable<R> item, Matcher<? super R> matcher) {
        return failIf(new HamcrestCondition<>(item, matcher));
    }

    /**
     * Causes the previous actions to be run and events to be awaited before awaiting the next event
     * passed. If the next event goes on to define some action(s) to run before waiting, those will
     * only run after the previous set of events is awaited (those that came before the {@link
     * #andThenExpect(Event)} call).
     */
    default <U> Event<U> andThenExpect(Event<U> nextEvent) {
        return new SequentialEvent<U>(this, nextEvent);
    }

    /**
     * Causes the previous actions to be run and events to be awaited before awaiting the next event
     * passed. If the next event goes on to define some action(s) to run before waiting, those will
     * only run after the previous set of events is awaited (those that came before the {@link
     * #andThenExpect(Condition)} call).
     */
    default <U> PollEvent<U> andThenExpect(Condition<U> condition) {
        return new SequentialEventWithPollEvent<U>(this, condition.asEvent());
    }

    /**
     * @see #andThenExpect(Condition)
     */
    default <U> PollEvent<U> andThenExpect(Callable<U> toReturnTrueOrNonNull) {
        return andThenExpect(HamcrestCondition.isTrueOrNonNull(toReturnTrueOrNonNull));
    }

    /**
     * @see #andThenExpect(Condition)
     */
    default <U> PollEvent<U> andThenExpect(U item, CheckedPredicate<? super U> predicate) {
        return andThenExpectCallTo(() -> item, predicate);
    }

    /**
     * @see #andThenExpect(Condition)
     */
    default <U> PollEvent<U> andThenExpectCallTo(Callable<U> item,
            CheckedPredicate<? super U> predicate) {
        return andThenExpect(Condition.matchCallTo(item, predicate));
    }

    /**
     * @see #andThenExpect(Condition)
     */
    default <U> PollEvent<U> andThenExpect(U item, Matcher<? super U> matcher) {
        return andThenExpectCallTo(() -> item, matcher);
    }

    /**
     * @see #andThenExpect(Condition)
     */
    default <U> PollEvent<U> andThenExpectCallTo(Callable<U> item, Matcher<? super U> matcher) {
        return andThenExpect(new HamcrestCondition<>(item, matcher));
    }
}

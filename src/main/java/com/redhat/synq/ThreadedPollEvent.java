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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/**
 * Polls until a condition is met, where all condition evaluation (via
 * {@link com.redhat.synq.Condition#isMet()}) is gated through the work queue of a single thread
 * executor associated with the thread that created the poll event. That is, if a thread is
 * simultaneously awaiting multiple ThreadedPollEvents that it constructed using the familiar,
 * fluent {@link com.redhat.synq.Event} API, each poll event will evaluate their conditions using a
 * shared single threaded executor service. Therefore, conditions will never be evaluated
 * simultaneously, allowing non-thread-safe code to be examined in conditions.
 */
public class ThreadedPollEvent<T> implements PollEvent<T> {
    /**
     * Each thread that constructs a poll event gets one, and only, associated single thread
     * executor.
     *
     * @see #ThreadedPollEvent(Condition)
     * @see #ThreadedPollEvent(Condition, TimeKeeper)
     */
    private static final ThreadLocal<ExecutorService> POLLERS = new ThreadLocal<ExecutorService>() {
        @Override
        protected ExecutorService initialValue() {
            return Executors.newSingleThreadExecutor();
        }
    };

    private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(1);

    private final Condition<T> condition;
    private final TimeKeeper timeKeeper;
    private final ExecutorService poller;

    private Duration pollingInterval = DEFAULT_POLLING_INTERVAL;
    private Set<Class<? extends Exception>> ignoredExceptions = new HashSet<>();

    /**
     * Creates a ThreadedPollEvent that evaluates the specified condition, using the system's real
     * clock and the current thread's associated single thread executor service.
     *
     * @see #POLLERS
     */
    public ThreadedPollEvent(Condition<T> condition) {
        this(condition, TimeKeeper.systemTimeKeeper(), POLLERS.get());
    }

    /**
     * Creates a ThreadedPollEvent that evaluates the specified condition, using the specified
     * {@link com.redhat.synq.TimeKeeper}, and the current thread's associated single thread
     * executor service. Useful for testing with a deterministic, mock TimeKeeper.
     *
     * @see #POLLERS
     */
    public ThreadedPollEvent(Condition<T> condition, TimeKeeper timeKeeper) {
        this(condition, timeKeeper, POLLERS.get());
    }

    public ThreadedPollEvent(Condition<T> condition, TimeKeeper timeKeeper,
            ExecutorService poller) {
        this.condition = condition;
        this.timeKeeper = timeKeeper;
        this.poller = poller;
    }

    @Override
    public T waitUpTo(Duration duration) {
        boolean met = false;
        T lastResult = null;
        Instant timeoutTime = timeKeeper.instant().plus(duration);
        Instant now;

        while (!met) {
            now = timeKeeper.instant();

            if (now.isAfter(timeoutTime)
                    || now.equals(timeoutTime)) {
                throw new TimeoutException(this, duration);
            }

            try {
                met = poller.submit(condition::isMet).get();
                lastResult = condition.lastResult();
            } catch (RejectedExecutionException | CancellationException e) {
                throw new SynqException(e);
            } catch (ExecutionException e) {
                throwIfNotIgnored(e.getCause());
            } catch (InterruptedException e) {
                throw new SleepInterruptedException(e);
            }

            if (!met) {
                timeKeeper.sleepFor(pollingInterval);
            }
        }

        return lastResult;
    }

    public ThreadedPollEvent<T> pollingEvery(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;

        return this;
    }

    public ThreadedPollEvent<T> ignoring(Class<? extends Exception> exception) {
        ignoredExceptions.add(exception);
        return this;
    }

    @Override
    public PollEvent<T> describedAs(String description) {
        condition.describedAs(description);

        return this;
    }

    @Override
    public PollEvent<T> describedAs(Supplier<String> description) {
        condition.describedAs(description);

        return this;
    }

    @Override
    public String toString() {
        return condition.toString() +
                " (as determined by polling every " + pollingInterval.toString() +")";
    }

    private void throwIfNotIgnored(Throwable t) throws RuntimeException {
        for (Class<? extends Exception> ignoredException : ignoredExceptions) {
            // The getCause check may not be necessary
            if (ignoredException.isInstance(t) || ignoredException.isInstance(t.getCause())) {
                return;
            }
        }

        throw new ConditionEvaluationException(t);
    }
}

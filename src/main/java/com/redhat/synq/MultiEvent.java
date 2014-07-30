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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.lang.Thread.UncaughtExceptionHandler;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class MultiEvent<T> extends AbstractEvent<T> {
    private final Event<? extends T> original;
    protected final Event<? extends T> additional;
    private T firstResult;
    private Throwable throwable;
    private UncaughtExceptionHandler exceptionHandler = new MultiEventExceptionHandler();
    private CountDownLatch latch = new CountDownLatch(1);

    public MultiEvent(Event<? extends T> original, Event<? extends T> additional) {
        this.original = original;
        this.additional = additional;

        describedAs(original + " or " + additional);
    }

    @Override
    public T waitUpTo(Duration duration) {
        // Could using an executor service instead make testing easier?
        Thread originalWaiter = new Thread(() -> finishWithResult(original.waitUpTo(duration)));
        Thread additionalWaiter = new Thread(() -> finishWithResult(additional.waitUpTo(duration)));

        originalWaiter.setUncaughtExceptionHandler(exceptionHandler);
        additionalWaiter.setUncaughtExceptionHandler(exceptionHandler);

        originalWaiter.start();
        additionalWaiter.start();

        boolean timedOut;

        try {
            // Also check for TimeoutExceptions from the inner events themselves. If either of their
            // threads beats the latch towards a timeout, we want to make sure we throw our own
            // MultiEvent TimeoutException instead of an individual event's.
            timedOut = !latch.await(duration.toMillis(), MILLISECONDS)
                    || throwable instanceof TimeoutException;
        } catch (InterruptedException e) {
            originalWaiter.interrupt();
            additionalWaiter.interrupt();

            throw new SleepInterruptedException(e);
        }

        // We don't know which finished first so interrupt them both; it's harmless.
        originalWaiter.interrupt();
        additionalWaiter.interrupt();

        if (timedOut) {
            throw new TimeoutException(this, duration);
        }

        if (throwable != null) {
            throw new EventException(throwable);
        }

        return firstResult;
    }

    private synchronized void finishWithResult(T result) {
        if (firstResult == null) {
            firstResult = result;
            latch.countDown();
        }
    }

    private synchronized void finishWithException(Throwable t) {
        if (throwable == null) {
            throwable = t;
            latch.countDown();
        }
    }

    private class MultiEventExceptionHandler implements UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            finishWithException(e);
        }

    }
}

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

package com.redhat.synq.testing.doubles;

import com.redhat.synq.Event;
import com.redhat.synq.EventListener;
import com.redhat.synq.TimeKeeper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A TimeKeeper who's only means of recording the passage of time is by calling
 * {@link #sleepFor(java.time.Duration)}. Useful for determinate testing.
 *
 * <p>You can imagine this as modeling a special universe where all computations are instantaneous;
 * time only passes when the computations specifically ask to pause (sleep) for a bit.
 */
public class FakeTimeKeeper extends TimeKeeper {
    private final ZoneId zone;
    private Instant now;
    private List<ScheduledCallback> callbacks = new ArrayList<>();

    public FakeTimeKeeper() {
        this(ZoneId.systemDefault());
    }

    public FakeTimeKeeper(ZoneId zone) {
        this.zone = zone;

        now = Instant.now(Clock.system(zone));
    }

    /**
     * Schedules something to happen after some amount of time from now, as specified by
     * <code>timeUntilCallback</code>.
     */
    public <T> void scheduleCallback(Runnable callback, Duration timeUntilCallback) {
        callbacks.add(new ScheduledCallback(instant().plus(timeUntilCallback), callback));
    }

    /**
     * Mimics the interruption behavior of Thread.sleep.
     */
    @Override
    public void sleepFor(Duration duration) {
        now = now.plus(duration);

        Iterator<ScheduledCallback> callbackIterator = callbacks.iterator();

        while (callbackIterator.hasNext()) {
            ScheduledCallback e = callbackIterator.next();

            if (now.isAfter(e.instant) || now.equals(e.instant)) {
                e.callback.run();

                if (Thread.currentThread().isInterrupted()) {
                    // The callback interrupted the thread... this would halt the sleep at the time
                    // of the interrupt.
                    now = e.instant;
                }

                callbackIterator.remove();
            }
        }
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(this.zone)) {
            return this;
        }

        return new FakeTimeKeeper(zone);
    }

    @Override
    public Instant instant() {
        return now;
    }

    private class ScheduledCallback {
        Instant instant;
        Runnable callback;

        ScheduledCallback(Instant instant, Runnable callback) {
            this.instant = instant;
            this.callback = callback;
        }
    }
}

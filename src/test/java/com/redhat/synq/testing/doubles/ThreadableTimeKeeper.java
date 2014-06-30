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

import com.redhat.synq.TimeKeeper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A TimeKeeper who's sleep "busy waits" until some other clock (to be managed by another thread)
 * reports that enough time has passed.
 *
 * <p>Intended usage is with a fake clock that programmatically passes time.
 *
 * @see com.redhat.synq.testing.doubles.FakeTimeKeeper
 */
public class ThreadableTimeKeeper extends TimeKeeper {
    private Clock clock;

    public ThreadableTimeKeeper(Clock clock) {
        this.clock = clock;
    }

    /**
     * Mimics the interruption behavior of Thread.sleep.
     */
    @Override
    public void sleepFor(Duration duration) {
        Instant then = clock.instant().plus(duration);

        while (clock.instant().isBefore(then)) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }
    }

    @Override
    public ZoneId getZone() {
        return clock.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        if (zone.equals(clock.getZone())) {
            return this;
        }

        return new ThreadableTimeKeeper(clock.withZone(zone));
    }

    @Override
    public Instant instant() {
        return clock.instant();
    }
}

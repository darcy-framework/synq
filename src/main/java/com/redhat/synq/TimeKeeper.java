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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * A Clock that also encapsulates the passage of time via {@link #sleepFor(java.time.Duration)}.
 */
public abstract class TimeKeeper extends Clock {
    public static TimeKeeper systemTimeKeeper() {
        return new SystemTimeKeeper();
    }

    /**
     * Pause execution for some duration, flagging the thread as interrupted if an interrupt should
     * occur.
     *
     * @throws SleepInterruptedException if thread was interrupted while waiting. If you don't want
     * this to propagate you should catch this exception.
     */
    public abstract void sleepFor(Duration duration);

    /**
     * Pause execution for some duration, flagging the thread as interrupted if an interrupt should
     * occur.
     *
     * @throws SleepInterruptedException if thread was interrupted while waiting. If you don't want
     * this to propagate you should catch this exception.
     */
    public void sleepFor(long amount, ChronoUnit unit) {
        sleepFor(Duration.of(amount, unit));
    }

    private static class SystemTimeKeeper extends TimeKeeper {
        private final Clock systemClock = Clock.systemUTC();

        @Override
        public void sleepFor(Duration duration) {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                throw new SleepInterruptedException(e);
            }
        }

        @Override
        public ZoneId getZone() {
            return systemClock.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return systemClock.withZone(zone);
        }

        @Override
        public Instant instant() {
            return systemClock.instant();
        }
    }
}

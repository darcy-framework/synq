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

import com.redhat.synq.AbstractEvent;
import com.redhat.synq.TimeKeeper;
import com.redhat.synq.TimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class FakeEvent<T> extends AbstractEvent<T> {
    private TimeKeeper timeKeeper;
    private Instant occurTime;
    private Supplier<T> result;

    public FakeEvent(Duration timeUntilOccurs) {
        this(timeUntilOccurs, TimeKeeper.systemTimeKeeper());
    }

    public FakeEvent(Duration timeUntilOccurs, TimeKeeper timeKeeper) {
        this(() -> null, timeUntilOccurs, timeKeeper);
    }

    public FakeEvent(Supplier<T> result, Duration timeUntilOccurs) {
        this(result, timeUntilOccurs, TimeKeeper.systemTimeKeeper());
    }

    public FakeEvent(Supplier<T> result, Duration timeUntilOccurs, TimeKeeper timeKeeper) {
        this.timeKeeper = timeKeeper;
        this.result = result;

        occurTime = timeKeeper.instant().plus(timeUntilOccurs);

        describedAs("a fake event that always occurs after " + timeUntilOccurs +
                " (at " + occurTime + ")");
    }

    @Override
    public T waitUpTo(Duration duration) {
        Duration sleepTime = Duration.between(timeKeeper.instant(), occurTime);

        if (sleepTime.compareTo(Duration.ZERO) <= 0) {
            sleepTime = Duration.ZERO;
        }

        if (sleepTime.compareTo(duration) > 0) {
            timeKeeper.sleepFor(duration);

            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            throw new TimeoutException(this, duration);
        }

        timeKeeper.sleepFor(sleepTime);

        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        return result.get();
    }
}

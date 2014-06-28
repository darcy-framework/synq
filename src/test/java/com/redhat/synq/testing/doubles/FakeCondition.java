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

import com.redhat.synq.AbstractCondition;
import com.redhat.synq.TimeKeeper;

import java.time.Duration;
import java.time.Instant;

/**
 * A fake condition that is satisfied after some predetermined amount of time, with some
 * predetermined result.
 */
public class FakeCondition<T> extends AbstractCondition<T> {
    private final T finalResult;
    private final TimeKeeper timeKeeper;
    private final Instant metTime;

    /**
     * Creates a new condition which will be met after some amount of time after condition has been
     * instantiated, as specified by <code>timeUntilMet</code>.
     */
    public FakeCondition(Duration timeUntilMet, TimeKeeper timeKeeper) {
        this(null, timeUntilMet, timeKeeper);
    }

    /**
     * Creates a new condition which will be met after some amount of time after condition has been
     * instantiated, as specified by <code>timeUntilMet</code>. The result of this condition is null
     * until it is met, in which case the result is <code>finalResult</code>.
     */
    public FakeCondition(T finalResult, Duration timeUntilMet, TimeKeeper timeKeeper) {
        this.finalResult = finalResult;
        this.timeKeeper = timeKeeper;
        this.metTime = Instant.now(timeKeeper).plus(timeUntilMet);

        describedAs("FakeCondition to be met (which happens after " + timeUntilMet + ")");
    }

    @Override
    public boolean isMet() throws Exception {
        return Instant.now(timeKeeper).isAfter(metTime);
    }

    @Override
    public T lastResult() {
        try {
            if (isMet()) {
                return finalResult;
            }
        } catch (Exception ignored) {
            // fall through
        }

        return null;
    }
}

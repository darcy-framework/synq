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

import static com.redhat.synq.ThrowableUtil.throwUnchecked;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Essentially transforms an event to its "inverse." See {@link FailEvent} javadoc.
 */
public class ForwardingFailEvent<T> extends AbstractEvent<T> implements FailEvent<T> {
    protected Event<?> original;
    private Function<AssertionError, Throwable> throwable;
    
    public ForwardingFailEvent(Event<?> original) {
        this.original = original;

        describedAs(original::toString);
    }
    
    @Override
    public T waitUpTo(Duration duration) {
        Object result;

        try {
            result = original.waitUpTo(duration);
        } catch (TimeoutException e) {
            // If a fail event times out, this is okay -- it means nothing "failed" in the given 
            // time, which is what we would like to see.
            return null;
        }
        
        if (!Thread.currentThread().isInterrupted()) {
            // If we got here, then we got a result before the timeout. For a fail event, this is
            // the condition to throw the associated exception.

            StringBuilder detailMessage = new StringBuilder("Fail event occurred, ")
                    .append("'").append(original).append("'\n")
                    .append("Result of event was: ").append(result);

            AssertionError e = new AssertionError(detailMessage);

            if (throwable != null) {
                throwUnchecked(throwable.apply(e).fillInStackTrace());
            } else {
                throw e;
            }
        }

        return null;
    }

    @Override
    public FailEvent<T> throwing(Function<AssertionError, Throwable> throwable) {
        this.throwable = throwable;

        return this;
    }

    @Override
    public FailEvent<T> describedAs(String description) {
        super.describedAs(description);

        return this;
    }

    @Override
    public FailEvent<T> describedAs(Supplier<String> description) {
        super.describedAs(description);

        return this;
    }
}

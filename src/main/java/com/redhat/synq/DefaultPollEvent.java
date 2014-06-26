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

public class DefaultPollEvent<T> implements PollEvent<T> {
    private static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(1);

    private final Condition<T> condition;

    private Duration pollingInterval = DEFAULT_POLLING_INTERVAL;
    private Set<Class<? extends Exception>> ignoredExceptions = new HashSet<>();
    
    public DefaultPollEvent(Condition<T> condition) {
        this.condition = condition;
    }
    
    public DefaultPollEvent<T> pollingEvery(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
        
        return this;
    }
    
    public DefaultPollEvent<T> ignoring(Class<? extends Exception> exception) {
        ignoredExceptions.add(exception);
        return this;
    }

    @Override
    public PollEvent<T> describedAs(String description) {
        condition.describedAs(description);

        return this;
    }

    @Override
    public T waitUpTo(Duration duration) {
        boolean met = false;
        T lastResult = null;
        Instant timeoutTime = Instant.now().plus(duration);
        
        while (!met) {
            try {
                met = condition.isMet();
                lastResult = condition.lastResult();
                
                if (met) {
                    break;
                }
            } catch (Exception e) {
                throwIfNotIgnored(e);
            }
            
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            
            if (Instant.now().isAfter(timeoutTime)) {
                throw new TimeoutException(this, duration);
            }
            
            try {
                // TODO: Encapsulate Thread.sleep for testability
                Thread.sleep(pollingInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        
        return lastResult;
    }

    @Override
    public String toString() {
        return condition.toString() + " (polling every " + pollingInterval.toString() +")";
    }
    
    private void throwIfNotIgnored(Throwable t) throws RuntimeException {
        for (Class<? extends Exception> ignoredException : ignoredExceptions) {
            // The getCause check may not be necessary
            if (ignoredException.isInstance(t) || ignoredException.isInstance(t.getCause())) {
                return;
            }
        }
        
        throw ThrowableUtil.throwUnchecked(t);
    }
}

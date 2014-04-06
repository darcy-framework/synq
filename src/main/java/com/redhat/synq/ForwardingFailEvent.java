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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Essentially transforms an event to its "inverse." See {@link FailEvent} javadoc.
 * 
 * @author ahenning
 *
 * @param <T>
 */
public class ForwardingFailEvent<T> implements FailEvent<T> {
    protected Event<?> original;
    private Throwable throwable;
    
    public ForwardingFailEvent(Event<?> original) {
        this(original, new FailEventException(original));
    }
    
    public ForwardingFailEvent(Event<?> original, Throwable throwable) {
        this.original = original;
        this.throwable = throwable;
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        try {
            original.waitUpTo(timeout, unit);
        } catch (Exception e) {
            // TODO: When waitUpTo throws something different, update this
            if (e.getCause() instanceof TimeoutException) {
                return null;
            } else {
                throw e;
            }
        }
        
        if (!Thread.currentThread().isInterrupted()) {
            // If we got here, then we got a result before the timeout.
            throw new RuntimeException(throwable);
        }
        
        return null;
    }
    
    public FailEvent<T> throwing(Throwable throwable) {
        this.throwable = throwable;
        
        return this;
    }
}

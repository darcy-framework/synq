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

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

public class EventListener<T> extends AbstractEvent<T> {
    private CountDownLatch latch = new CountDownLatch(1);
    private T result;
    
    public void trigger(T result) {
        this.result = result;
        latch.countDown();
    }
    
    @Override
    public T waitUpTo(Duration duration) {
        boolean timedOut;
        
        try {
            timedOut = !latch.await(duration.toMillis(), MILLISECONDS);
        } catch (InterruptedException e) {
            // TODO: Remove event listener
            throw new SleepInterruptedException(e);
        }
        
        if (timedOut) {
            throw new TimeoutException(this, duration);
        }
        
        return result;
    }
    
}

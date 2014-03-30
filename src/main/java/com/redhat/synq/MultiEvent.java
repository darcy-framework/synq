/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MultiEvent<T> implements Event<T> {
    private final Event<? extends T> original;
    protected final Event<? extends T> additional;
    private T firstResult;
    private Throwable throwable;
    private UncaughtExceptionHandler exceptionHandler = new MultiEventExceptionHandler();
    private CountDownLatch latch = new CountDownLatch(1);
    
    public MultiEvent(Event<? extends T> original, Event<? extends T> additional) {
        this.original = original;
        this.additional = additional;
    }
    
    @Override
    public T waitUpTo(long timeout, TimeUnit unit) {
        Thread originalAwaiter = new Thread(() -> {
            finishWithResult(original.waitUpTo(timeout, unit));
        });
        
        Thread additionalAwaiter = new Thread(() -> {
            finishWithResult(additional.waitUpTo(timeout, unit));
        });
        
        originalAwaiter.setUncaughtExceptionHandler(exceptionHandler);
        additionalAwaiter.setUncaughtExceptionHandler(exceptionHandler);
        
        originalAwaiter.start();
        additionalAwaiter.start();
        
        boolean timedOut = false;
        
        try {
            timedOut = !latch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        
        // We don't know which finished first so interrupt them both; it's harmless.
        originalAwaiter.interrupt();
        additionalAwaiter.interrupt();
        
        if (timedOut) {
            // TODO: Make this better
            throw new RuntimeException(new TimeoutException());
        }
        
        if (throwable != null) {
            // TODO: Make this better
            throw new RuntimeException(throwable);
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

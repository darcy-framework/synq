package com.redhat.synq;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class EventCallbacks {
    /**
     * 
     * @param times Number of attempts to allow before letting an exception propogate
     * @param timeout
     * @param unit
     * @return
     */
    public static <T, U extends Throwable> BiFunction<Event<T>, U, T> retry(int times, long timeout, 
            TimeUnit unit) {
        return new BiFunction<Event<T>, U, T>() {
            private int attempted = 0;
            
            @Override
            public T apply(Event<T> t, U throwable) {
                attempted++;
                
                if (attempted < times) {
                    return t.waitUpTo(timeout, unit);
                } else {
                    throw ThrowableUtil.throwUnchecked(throwable);
                }
            }
            
        };
    }
}

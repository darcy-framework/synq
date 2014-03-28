package com.redhat.sync;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An event that, if triggered, will throw an exception instead of returning with the result of the
 * triggered event.
 * 
 * @author ahenning
 *
 * @param <T>
 */
public class ExceptionalEvent<T> implements Event<T> {
    protected Event<?> original;
    private Throwable throwable;
    
    public ExceptionalEvent(Event<?> original, Throwable throwable) {
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
    
}

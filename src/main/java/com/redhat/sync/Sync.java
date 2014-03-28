package com.redhat.sync;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.hamcrest.Matcher;

/**
 * Helper class that with static factories for default ConfigurableWait implementations.
 * @author ahenning
 *
 */
public final class Sync {
    private Sync() {}
    
    /**
     * Static factory (just for import static sugar).
     * 
     * @return
     */
    public static <T> Event<T> expect(Event<T> toOccur) {
        return toOccur;
    }
    
    public static <T> PollEvent<T> expect(Condition<T> toBeMet) {
        return toBeMet.asEvent();
    }
    
    public static <T> PollEvent<T> expect(Callable<T> toReturnTrueOrNonNull) {
        return expect(HamcrestCondition.isTrueOrNonNull(toReturnTrueOrNonNull));
    }
    
    public static <T> PollEvent<T> expect(T item, Predicate<? super T> predicate) {
        return expect(() -> item, predicate);
    }
    
    public static <T> PollEvent<T> expect(Callable<T> item, Predicate<? super T> predicate) {
        return expect(Condition.match(item, predicate));
    }
    
    public static <T> PollEvent<T> expect(T item, Matcher<? super T> matcher) {
        return expect(() -> item, matcher);
    }
    
    public static <T> PollEvent<T> expect(Callable<T> item, Matcher<? super T> matcher) {
        return expect(new HamcrestCondition<>(item, matcher));
    }
    
    /**
     * Static factory for a WaitFactory that runs something just before waiting.
     * 
     * @param runnable
     * @return
     */
    public static After after(Runnable action) {
        return new After(action);
    }
    
    public static class After {
        private Runnable action;
        
        public After(Runnable action) {
            this.action = action;
        }
        
        public <T> Event<T> expect(Event<T> toOccur) {
            return toOccur.after(action);
        }
        
        public <T> PollEvent<T> expect(Condition<T> toBeMet) {
            return toBeMet.asEvent().after(action);
        }
        
        public <T> PollEvent<T> expect(Callable<T> toReturnTrueOrNonNull) {
            return expect(HamcrestCondition.isTrueOrNonNull(toReturnTrueOrNonNull));
        }
        
        public <T> PollEvent<T> expect(T item, Predicate<? super T> predicate) {
            return expect(() -> item, predicate);
        }
        
        public <T> PollEvent<T> expect(Callable<T> item, Predicate<? super T> predicate) {
            return expect(Condition.match(item, predicate));
        }
        
        public <T> PollEvent<T> expect(T item, Matcher<? super T> matcher) {
            return expect(() -> item, matcher);
        }
        
        public <T> PollEvent<T> expect(Callable<T> item, Matcher<? super T> matcher) {
            return expect(new HamcrestCondition<>(item, matcher));
        }
    }
}

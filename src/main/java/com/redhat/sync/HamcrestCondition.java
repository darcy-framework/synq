package com.redhat.sync;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import java.util.concurrent.Callable;

import org.hamcrest.Matcher;

public class HamcrestCondition<T> implements Condition<T> {
    private static final Matcher<Object> isTrueOrNonNull = 
            not(anyOf(nullValue(), equalTo((Object)Boolean.FALSE)));
    
    private Callable<T> item;
    private Matcher<? super T> matcher;
    private T lastResult = null;
    
    public static <T> HamcrestCondition<T> match(Callable<T> item, Matcher<? super T> matcher) {
        return new HamcrestCondition<T>(item, matcher);
    }

    public static <T> HamcrestCondition<T> match(final T item, Matcher<? super T> matcher) {
        return new HamcrestCondition<T>(() -> item, matcher);
    }
    
    public static <T> HamcrestCondition<T> isTrueOrNonNull(Callable<T> item) {
        return new HamcrestCondition<T>(item, isTrueOrNonNull);
    }
    
    /**
     * Shortcut for a matcher that will match any value that is not null and not false.
     * @param item
     */
    public HamcrestCondition(Callable<T> item) {
        this(item, isTrueOrNonNull);
    }
    
    public HamcrestCondition(Callable<T> item, Matcher<? super T> matcher) {
        this.item = item;
        this.matcher = matcher;
    }

    @Override
    public boolean isMet() throws Exception {
        lastResult = item.call();
        return matcher.matches(lastResult);
    }

    @Override
    public T lastResult() {
        return lastResult;
    }
    
    @Override
    public String toString() {
        return matcher.toString();
    }
    
    public Callable<T> getSupplier() {
        return item;
    }
    
    public Matcher<? super T> getMatcher() {
        return matcher;
    }
}

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

    public static <T> HamcrestCondition<T> match(T item, Matcher<? super T> matcher) {
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

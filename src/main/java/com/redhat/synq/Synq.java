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

import java.util.concurrent.Callable;
import java.util.function.Predicate;

import org.hamcrest.Matcher;

/**
 * Helper class that with static factories for default ConfigurableWait implementations.
 * @author ahenning
 *
 */
public final class Synq {
    private Synq() {}
    
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
        return expect(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return item;
            }
            
        }, predicate);
    }
    
    public static <T> PollEvent<T> expect(Callable<T> item, Predicate<? super T> predicate) {
        return expect(Condition.match(item, predicate));
    }
    
    public static <T> PollEvent<T> expect(T item, Matcher<? super T> matcher) {
        return expect(new Callable<T>() {

            @Override
            public T call() throws Exception {
                return item;
            }
            
        }, matcher);
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
            return expect(new Callable<T>() {

                @Override
                public T call() throws Exception {
                    return item;
                }
                
            }, predicate);
        }
        
        public <T> PollEvent<T> expect(Callable<T> item, Predicate<? super T> predicate) {
            return expect(Condition.match(item, predicate));
        }
        
        public <T> PollEvent<T> expect(T item, Matcher<? super T> matcher) {
            return expect(new Callable<T>() {

                @Override
                public T call() throws Exception {
                    return item;
                }
                
            }, matcher);
        }
        
        public <T> PollEvent<T> expect(Callable<T> item, Matcher<? super T> matcher) {
            return expect(new HamcrestCondition<>(item, matcher));
        }
    }
}

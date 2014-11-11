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
import java.util.function.Supplier;

public interface Condition<T> {
    /**
     * Evaluates the condition, which may include any arbitrary computation(s). The result of that
     * computation under test is typed by T, and available via {@link #lastResult()}.
     * <P>
     * Determining whether or not the condition was met is simply a matter of overriding this
     * function and returning true or false based on whatever logic you want. You can make some
     * interesting object available related to that logic by overriding {@link #lastResult()}.
     * 
     * @return True if the condition was met. False if not.
     * @throws com.redhat.synq.ConditionEvaluationException if a checked exception occurred while
     * trying to determine if the condition was met.
     */
    boolean isMet();
    
    /**
     * Requires the condition be first evaluated via {@link #isMet()}.
     * 
     * @return The last value returned by the computation under test, or a reference to the object
     * under examination.
     * @throws IllegalStateException if the test was not yet run via {@link #isMet()}, or the
     * previous test failed.
     */
    T lastResult();

    default Condition<T> describedAs(String description) {
        return describedAs(() -> description);
    }

    @Experimental
    Condition<T> describedAs(Supplier<String> description);
    
    /**
     * Converts this condition to an {@link Event} by polling at some default interval until the
     * condition is met, and then firing the event.
     * <P>
     * Note that while polling is very flexible, it has significant reliability drawbacks when
     * compared to true event listeners.
     * 
     * @see com.redhat.synq.ThreadedPollEvent
     * @return The condition as a {@link PollEvent}
     */
    default PollEvent<T> asEvent() {
        return new ThreadedPollEvent<T>(this);
    }

    /**
     * Converts this condition to an {@link Event} by polling at some default interval until the
     * condition is met, and then firing the event.
     * <P>
     * Note that while polling is very flexible, it has significant reliability drawbacks when
     * compared to true event listeners.
     *
     * @see com.redhat.synq.ThreadedPollEvent
     * @return The condition as a {@link PollEvent}
     */
    default PollEvent<T> asEvent(TimeKeeper timeKeeper) {
        return new ThreadedPollEvent<T>(this, timeKeeper);
    }
    
    static <T> Condition<T> matchCallTo(Callable<T> item, CheckedPredicate<? super T> predicate) {
        return new AbstractCondition<T>() {
            private T lastResult = null;
            
            @Override
            public boolean isMet() {
                try {
                    lastResult = item.call();
                    return predicate.test(lastResult);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ConditionEvaluationException(e);
                }
            }
            
            @Override
            public T lastResult() {
                return lastResult;
            }

        }.describedAs(() -> item + " to satisfy " + predicate);
    }
    
    static <T> Condition<T> match(T item, CheckedPredicate<? super T> predicate) {
        return matchCallTo(new Callable<T>() {
            
            @Override
            public T call() throws Exception {
                return item;
            }

            @Override
            public String toString() {
                return item.toString();
            }
            
        }, predicate);
    }
}

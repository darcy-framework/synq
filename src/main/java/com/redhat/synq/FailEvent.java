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

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Fail events should, instead of returning some value when they are triggered, throw an exception.
 * When their timeout is reached, they should simply return null (as opposed to throwing a timeout
 * exception). Fail events are kind of the "inverse" of regular events.
 * <P>
 * Useful for events which you don't want to happen in a given time interval.
 * 
 * @author ahenning
 *
 * @see ForwardingFailEvent
 */
public interface FailEvent<T> extends Event<T> {
    /**
     * Instead of throwing an {@link java.lang.AssertionError}, throw an exception as created by the
     * passed function. This function accepts the error, so you are encouraged to use it as the
     * cause of your custom exception. The assertion error will contain valuable information related
     * to the event.
     * <p/>
     * Method references may proof especially terse if you do not require a detail message:
     * <ul>
     *     <li>{@code throwing(MySpecificException::new) // uses AssertionError as cause}</li>
     *     <li>{@code throwing(e -> new MySpecificException("detail message", relevantObj, e))}</li>
     * </ul>
     */
    FailEvent<T> throwing(Function<AssertionError, Throwable> throwable);

    /**
     * Instead of throwing an {@link java.lang.AssertionError}, throws a specific exception instance
     * instead.
     * <p/>
     * Exception stack traces are filled in at creation, however the FailEvent will overwrite the
     * original stack trace with a new one just before the exception is thrown to add context.
     * <p/>
     * While not as terse, it is encouraged to use {@link #throwing(java.util.function.Function)}
     * instead to retain event information, like so:
     * <ul>
     *     <li>{@code throwing(MySpecificException::new) // uses AssertionError as cause}</li>
     *     <li>{@code throwing(e -> new MySpecificException("detail message", relevantObj, e))}</li>
     * </ul>
     */
    @Experimental
    default FailEvent<T> throwing(Throwable throwable) {
        return throwing(e -> throwable);
    }

    @Override
    FailEvent<T> describedAs(String description);

    @Override
    FailEvent<T> describedAs(Supplier<String> description);
    
    @Override
    default FailEvent<T> after(Runnable action) {
        return new SequentialEventWithFailEvent<>(new ActionEvent(action), this);
    }
}

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

/**
 * Fail events should, instead of returning some value when they are triggered, throw an exception.
 * When their timeout is reached, they should simply return null (as opposed to throwing a timeout
 * exception). Fail events are kind of the "inverse" of regular events.
 * <P>
 * Useful for events which you don't want to happen in a given time interval.
 * 
 * @author ahenning
 *
 * @param <T>
 * @see ForwardingFailEvent
 */
public interface FailEvent<T> extends Event<T> {
    FailEvent<T> throwing(Throwable throwable);
    
    @Override
    default FailEvent<T> after(Runnable action) {
        return new SequentialEventWithFailEvent<>((t, u) -> {
            action.run();
            return null;
        }, this);
    }
}

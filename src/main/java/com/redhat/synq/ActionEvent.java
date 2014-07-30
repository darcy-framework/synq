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

import java.time.Duration;

/**
 * An instantaneously triggered Event that is simply a wrapper around some
 * {@link java.lang.Runnable}. It is a means to use an Event as a means of running some action
 * before awaiting a <em>real</em> event, as in {@link Event#after(Runnable)}.
 *
 * @see com.redhat.synq.SequentialEvent
 */
public class ActionEvent extends AbstractEvent<Void> {
    private final Runnable action;

    public ActionEvent(Runnable action) {
        this.action = action;

        describedAs("action is finished (" + action + ")");
    }

    @Override
    public Void waitUpTo(Duration duration) {
        action.run();

        return null;
    }
}

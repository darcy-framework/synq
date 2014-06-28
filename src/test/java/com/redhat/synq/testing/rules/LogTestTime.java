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

package com.redhat.synq.testing.rules;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

public class LogTestTime extends TestWatcher {
    private static final Logger logger = Logger.getLogger(LogTestTime.class.toString());
    private Instant start;

    @Override
    protected void starting(Description description) {
        super.starting(description);
        start = Instant.now();
    }

    @Override
    protected void finished(Description description) {
        super.finished(description);
        logger.info(description + " took " + Duration.between(start, Instant.now()));
    }
}

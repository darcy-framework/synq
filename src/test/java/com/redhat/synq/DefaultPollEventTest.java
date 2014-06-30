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

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.redhat.synq.testing.TestException;
import com.redhat.synq.testing.doubles.FakeCondition;
import com.redhat.synq.testing.doubles.FakeTimeKeeper;
import com.redhat.synq.testing.doubles.NeverMetCondition;
import com.redhat.synq.testing.rules.LogTestTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.Instant;

@RunWith(JUnit4.class)
public class DefaultPollEventTest {
    @Rule
    public ExpectedException exceptions = ExpectedException.none();

    @Rule
    public LogTestTime logTestTime = new LogTestTime();

    private FakeTimeKeeper timeKeeper = new FakeTimeKeeper();

    private static final Duration TEN_MILLIS = Duration.ofMillis(10);
    private static final Duration THIRTY_MILLIS = Duration.ofMillis(30);
    private static final Duration FIFTY_MILLIS = Duration.ofMillis(50);
    private static final Duration ONE_HUNDRED_MILLIS = Duration.ofMillis(100);

    @Test
    public void shouldReturnOnceConditionIsMet() throws Exception {
        Condition<Object> condition = new FakeCondition<>(FIFTY_MILLIS, timeKeeper);

        new DefaultPollEvent<>(condition, timeKeeper)
                .pollingEvery(10, MILLIS)
                .waitUpTo(100, MILLIS);
    }

    @Test(expected = TimeoutException.class)
    public void shouldThrowTimeoutExceptionIfConditionIsNotMetInTime() {
        new DefaultPollEvent<>(new NeverMetCondition(), timeKeeper)
                .waitUpTo(50, MILLIS);
    }

    @Test
    public void shouldIncludeEventDescriptionInTimeoutExceptionMessage() {
        Condition<Object> condition = new NeverMetCondition();

        exceptions.expectMessage(containsString(condition.toString()));

        new DefaultPollEvent<>(condition, timeKeeper)
                .waitUpTo(50, MILLIS);
    }

    @Test
    public void shouldStopWaitingIfThreadIsInterrupted() {
        timeKeeper.scheduleCallback(() -> Thread.currentThread().interrupt(), THIRTY_MILLIS);

        Instant start = timeKeeper.instant();

        new DefaultPollEvent<>(new NeverMetCondition(), timeKeeper)
                .waitUpTo(50, MILLIS);

        assertEquals(timeKeeper.instant(), start.plus(THIRTY_MILLIS));
        assertTrue(Thread.interrupted()); // Clear interrupt intentional
    }

    @Test
    public void shouldReturnTheLastExaminedResultOfTheCondition() {
        Condition<String> condition = new FakeCondition<>(() -> "Synq", FIFTY_MILLIS, timeKeeper);

        String result = new DefaultPollEvent<>(condition, timeKeeper)
                .pollingEvery(10, MILLIS)
                .waitUpTo(100, MILLIS);

        assertEquals("Synq", result);
    }

    @Test
    public void shouldAllowManuallyChangingDescriptionPresentInTimeoutMessage() {
        exceptions.expectMessage(containsString("a manually described test event to occur"));

        new DefaultPollEvent<>(new NeverMetCondition())
                .describedAs("a manually described test event to occur")
                .waitUpTo(100, MILLIS);
    }

    @Test
    public void shouldContainDescriptionInToString() {
        String eventToString = new DefaultPollEvent<>(new NeverMetCondition())
                .describedAs("a manually described test event to occur")
                .toString();

        assertThat(eventToString, containsString("a manually described test event to occur"));
    }

    @Test(expected = TestException.class)
    public void shouldThrowExceptionInConditionIfNotIgnored() {
        Condition<Object> condition = new FakeCondition<Object>(
                () -> { throw new TestException(); }, FIFTY_MILLIS, timeKeeper);

        new DefaultPollEvent<>(condition, timeKeeper)
                .pollingEvery(10, MILLIS)
                .waitUpTo(100, MILLIS);
    }

    @Test
    public void shouldNotThrowExceptionInConditionIfIgnored() {
        Condition<Object> condition = new FakeCondition<Object>(
                () -> { throw new TestException(); }, FIFTY_MILLIS, timeKeeper);

        new DefaultPollEvent<>(condition, timeKeeper)
                .pollingEvery(10, MILLIS)
                .ignoring(TestException.class)
                .waitUpTo(100, MILLIS);
    }
}

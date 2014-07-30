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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.redhat.synq.testing.doubles.FakeEvent;
import com.redhat.synq.testing.doubles.NeverOccurringEvent;
import com.redhat.synq.testing.rules.LogTestTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;

@RunWith(JUnit4.class)
public class SequentialEventTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public LogTestTime logTestTime = new LogTestTime();

    private static final Duration TEN_MILLIS = Duration.ofMillis(10);
    private static final Duration THIRTY_MILLIS = Duration.ofMillis(30);
    private static final Duration FIFTY_MILLIS = Duration.ofMillis(50);
    private static final Duration ONE_HUNDRED_MILLIS = Duration.ofMillis(100);

    @Test(timeout = 10000)
    public void shouldReturnOnceBothEventsOccur() {
        Event<Object> event1 = new FakeEvent<>(TEN_MILLIS);
        Event<Object> event2 = new FakeEvent<>(THIRTY_MILLIS);

        new SequentialEvent<>(event1, event2)
                .waitUpTo(FIFTY_MILLIS);
    }

    @Test(timeout = 10000)
    public void shouldNotWaitForSecondEventIfFirstEventNeverOccurs() {
        Event<Void> event1 = new NeverOccurringEvent();
        Event event2 = mock(Event.class);

        try {
            new SequentialEvent<Object>(event1, event2)
                    .waitUpTo(500, MILLIS);
        } catch (TimeoutException ignored) {
            // fall through
        }

        verify(event2, never()).waitUpTo(any());
        verify(event2, never()).waitUpTo(anyLong(), any());
    }

    @Test(expected = TimeoutException.class)
    public void shouldTimeoutIfOnlyTheFirstEventOccurs() {
        Event<Object> event1 = new FakeEvent<>(TEN_MILLIS);
        Event<Void> event2 = new NeverOccurringEvent();

        new SequentialEvent<>(event1, event2)
                .waitUpTo(200, MILLIS);
    }

    @Test(expected = TimeoutException.class)
    public void shouldTimeoutIfOnlyTheSecondEventOccurs() {
        Event<Void> event1 = new NeverOccurringEvent();
        Event<Object> event2 = new FakeEvent<>(TEN_MILLIS);

        new SequentialEvent<>(event1, event2)
                .waitUpTo(200, MILLIS);
    }

    @Test
    public void shouldReturnTheResultOfTheSecondEvent() {
        Event<String> event1 = new FakeEvent<>(() -> "first", TEN_MILLIS);
        Event<String> event2 = new FakeEvent<>(() -> "second", THIRTY_MILLIS);

        String result = new SequentialEvent<>(event1, event2)
                .waitUpTo(FIFTY_MILLIS);

        assertEquals("second", result);
    }

    @Test
    public void shouldDescribeBothEventsInTimeoutExceptionIfNeitherEventOccurs() {
        Event<Void> event1 = new NeverOccurringEvent().describedAs("a test event");
        Event<Void> event2 = new NeverOccurringEvent().describedAs("another test event");

        expectedException.expectMessage("a test event");
        expectedException.expectMessage("another test event");

        new SequentialEvent<>(event1, event2)
                .waitUpTo(TEN_MILLIS);
    }

    @Test
    public void shouldNotWaitForSecondEventIfInterruptedWhileWaitingForFirstEvent() {
        // Fake an interrupt
        Event<Object> event1 = new FakeEvent<>(() -> { throw new SleepInterruptedException(); },
                TEN_MILLIS);

        Event event2 = mock(Event.class);

        try {
            new SequentialEvent<Object>(event1, event2)
                    .waitUpTo(200, MILLIS);
        } catch (SleepInterruptedException expected) {
            // fall through
        }

        verify(event2, never()).waitUpTo(any());
        verify(event2, never()).waitUpTo(anyLong(), any());
    }

    @Test(expected = SleepInterruptedException.class)
    public void shouldPropagateSleepInterruptedExceptionFromFirstEvent() {
        // Fake an interrupt
        Event<Object> event1 = new FakeEvent<>(() -> { throw new SleepInterruptedException(); },
                TEN_MILLIS);

        Event<Object> event2 = new FakeEvent<>(THIRTY_MILLIS);

        new SequentialEvent<>(event1, event2)
                .waitUpTo(FIFTY_MILLIS);
    }

    @Test(expected = SleepInterruptedException.class)
    public void shouldPropagateSleepInterruptedExceptionFromSecondEvent() {
        Event<Object> event1 = new FakeEvent<>(THIRTY_MILLIS);

        // Fake an interrupt
        Event<Object> event2 = new FakeEvent<>(() -> { throw new SleepInterruptedException(); },
                TEN_MILLIS);

        new SequentialEvent<>(event1, event2)
                .waitUpTo(FIFTY_MILLIS);
    }

    @Test
    public void shouldReturnNewSequentialEventWithSequentialEventAsSecondEventFromAfterClause() {
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Runnable action = mock(Runnable.class);

        SequentialEvent<Object> event = (SequentialEvent<Object>)
                new SequentialEvent<Object>(event1, event2).after(action);
        SequentialEvent<Object> additional = (SequentialEvent<Object>) event.second;

        assertSame(event1, event.first);
        assertSame(additional.second, event2);

        additional.first.waitUpTo(TEN_MILLIS);
        verify(action).run();
    }
}

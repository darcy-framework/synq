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
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.redhat.synq.testing.TestException;
import com.redhat.synq.testing.doubles.FakeEvent;
import com.redhat.synq.testing.doubles.NeverOccurringEvent;
import com.redhat.synq.testing.rules.LogTestTime;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.util.function.IntSupplier;

@RunWith(JUnit4.class)
public class MultiEventTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public LogTestTime logTestTime = new LogTestTime();
    
    private static final Duration TEN_MILLIS = Duration.ofMillis(10);
    private static final Duration THIRTY_MILLIS = Duration.ofMillis(30);
    private static final Duration FIFTY_MILLIS = Duration.ofMillis(50);
    private static final Duration ONE_HUNDRED_MILLIS = Duration.ofMillis(100);

    @Test
    public void shouldReturnWhenOriginalEventOccurs() {
        Event<Object> event1 = new FakeEvent<>(FIFTY_MILLIS);
        Event<Void> event2 = new NeverOccurringEvent();

        new MultiEvent<>(event1, event2)
                .waitUpTo(ONE_HUNDRED_MILLIS);
    }

    @Test
    public void shouldReturnWhenAdditionalEventOccurs() {
        Event<Void> event1 = new NeverOccurringEvent();
        Event<Object> event2 = new FakeEvent<>(FIFTY_MILLIS);

        new MultiEvent<>(event1, event2)
                .waitUpTo(ONE_HUNDRED_MILLIS);
    }

    @Test
    public void shouldInterruptEventsAfterOneOccurs() throws InterruptedException {
        IntSupplier mockObject = mock(IntSupplier.class);

        Event<Integer> event1 = new FakeEvent<>(mockObject::getAsInt, ONE_HUNDRED_MILLIS);
        Event<Object> event2 = new FakeEvent<>(FIFTY_MILLIS);

        new MultiEvent<>(event1, event2)
                .waitUpTo(200, MILLIS);

        Thread.sleep(400);

        verifyZeroInteractions(mockObject);
    }

    @Test
    public void shouldReturnResultOfFirstEventThatOccurs() {
        Event<String> event1 = new FakeEvent<>(() -> "event1", FIFTY_MILLIS);
        Event<String> event2 = new FakeEvent<>(() -> "event2", ONE_HUNDRED_MILLIS);

        String result = new MultiEvent<>(event1, event2)
                .waitUpTo(200, MILLIS);

        assertEquals("event1", result);
    }

    @Test(expected = TestException.class)
    public void shouldThrowAnExceptionIfOneEventThrowsAnExceptionBeforeTheOtherOccurs() {
        Event<String> event1 = new FakeEvent<>(() -> { throw new TestException(); }, FIFTY_MILLIS);
        Event<String> event2 = new FakeEvent<>(() -> "event2", ONE_HUNDRED_MILLIS);

        new MultiEvent<>(event1, event2)
                .waitUpTo(200, MILLIS);
    }

    @Test(expected = TimeoutException.class)
    public void shouldThrowTimeoutExceptionIfNeitherEventOccurs() {
        new MultiEvent<>(new NeverOccurringEvent(), new NeverOccurringEvent())
                .waitUpTo(10, MILLIS);
    }

    @Test
    public void shouldDescribeBothEventsInTimeoutExceptionIfNeitherEventOccurs() {
        Event<Void> event1 = new NeverOccurringEvent().describedAs("a test event");
        Event<Void> event2 = new NeverOccurringEvent().describedAs("another test event");

        expectedException.expectMessage("a test event");
        expectedException.expectMessage("another test event");

        new MultiEvent<>(event1, event2)
                .waitUpTo(10, MILLIS);
    }

    @Test
    public void shouldReThrowNewTimeoutExceptionIfInnerEventTimesOutFirst() {
        class TestTimeoutException extends TimeoutException {
            public TestTimeoutException() {
                super(new FakeEvent<>(ONE_HUNDRED_MILLIS), ONE_HUNDRED_MILLIS);
            }
        }

        // Force a TimeoutException to happen early
        Event<Void> event1 = new FakeEvent<>(
                () -> { throw new TestTimeoutException(); }, FIFTY_MILLIS);

        // This won't occur because of faked timeout
        Event<Void> event2 = new FakeEvent<>(ONE_HUNDRED_MILLIS);

        expectedException.expect(not(instanceOf(TestTimeoutException.class)));
        expectedException.expect(TimeoutException.class);

        new MultiEvent<>(event1, event2)
                .waitUpTo(200, MILLIS);
    }
}

synq
====
[![Build Status](https://travis-ci.org/darcy-framework/synq.svg?branch=master)](https://travis-ci.org/darcy-framework/synq) [![Coverage Status](https://coveralls.io/repos/darcy-framework/synq/badge.png?branch=master)](https://coveralls.io/r/darcy-framework/synq?branch=master)
[![Stories in Ready](https://badge.waffle.io/darcy-framework/synq.png?label=ready&title=Ready)](https://waffle.io/darcy-framework/synq)

Synq is a small Java 8 library for syncing up a thread with some flexible configuration of external event(s). 

java 8
======
Java 8 features like lambda expressions and default methods in interfaces are core to maintaining readability and flexibility in Synq. If you are unfamiliar with lambda expressions in Java, [there are great tutorials][2]. You'll get a lot more out of the library if you're comfortable with lambdas.

example usage
=============
```java
import static com.redhat.synq.Synq.after;
// New Java 8 time API
import static java.time.temporal.ChronoUnit.SECONDS;

public class AsyncCalculatorTest {
  private Calculator calc = new AsyncCalculator();

  public void asyncAddTest() {
    // Start defining the condition with an after(). This accepts a functional
    // interface that will be called before we start waiting for the expected
    // conditions.
    int result = after(() -> calc.add(2, 2)) 
        
        // What do we want to wait to be true after we call the above function?
        .expectCallTo(calc::getResult, result -> result == 4)
            
        // Throw an exception if some other condition is met first
        .failIfCallTo(calc::getResult, result -> result != null && result != 4)
          .throwing(new AssertionError("Learn to add!"))
              
        // Now call the function defined in after, wait for getResult to return 
        // 4 and return it. If getResult returns a non-null and is not 4, throw
        // an AssertionError. If 10 seconds passes before getResult returns 
        // non-null, throw a TimeoutException.
        .waitUpTo(10, SECONDS);
  }

}
```

example 2: waiting for nothing
==============================
In other synchronization aids, waiting for something *not* to happen historically involves waiting for that something to happen, throwing an exception if it does, otherwise catching the timeout and ignoring it. In contrast, Synq handles this scenario elegantly:

```java
after(myObject::doSomething)
  .failIfCallTo(myObject::getSomeValue, value -> value == badValue)
  .throwing(new SomeException())
  .waitUpTo(10, SECONDS);
```

No timeout exception will be thrown here, because we aren't expecting anything in particular to happen. We only want to fail if something we don't want to happen, happens, and if that something doesn't happen within our timeout (10 seconds in this case), then we'll happily move along.

license
=======

**synq** is licensed under [version 3 of the GPL][1].


  [1]: https://www.gnu.org/copyleft/gpl.html
  [2]: http://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html

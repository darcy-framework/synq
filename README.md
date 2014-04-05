synq
====

Synq is a small Java 8 library for syncing up a thread with some flexible configuration of external event(s). 

Java 8
======
Java 8 features like lambda expressions and default methods in interfaces are core to maintaining readability and flexibility in Synq. If you are unfamiliar with lambda expressions in Java, [there are great tutorials][2]. You'll get a lot more out of the library if you're comfortable with lambdas.

Example usage
=============
```java
import static com.redhat.synq.Synq.after;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AsyncCalculatorTest {
  private Calculator calc = new AsyncCalculator();

  public void asyncAddTest() {
    // Start defining the condition with a after(). This accepts a functional
    // interface that will be called before we start waiting for the expected
    // conditions.
    int result = after(() -> calc.add(2, 2)) 
    
        // What do we want to wait to be true after we call the above function?
        .expect(calc::getResult, (result) -> result == 4)
        
        // Throw an exception if some other condition is met first
        .throwing(new AssertionError("Learn to add!"))
        
          // This is that condition
          .when(calc::getResult, (result) -> result != null && result != 4)
          
        // Wait for getResult to return 4 and return it. If getResult returns a
        // non-null and is not 4, throw an AssertionError. If 10 seconds passes
        // before getResult returns non-null, throw a TimeoutException.
        .waitUpTo(10, SECONDS);
  }

}
```

Example 2: Waiting for nothing
==============================
In other synchronization aids, waiting for something *not* to happen historically involves waiting for that something to happen, throwing an exception if it does, otherwise catching the timeout and ignoring it. In contrast, Synq handles this scenario elegantly:

```java
after(myObject::doSomething)
  .failIf(myObject::getSomeValue, (value) -> value == badValue)
  .waitUpTo(10, SECONDS);
```

No timeout exception will be thrown here, because we aren't expecting anything in particular to happen. We only want to fail if something we don't want to happen, happens, and if that something doesn't happen within our timeout (10 seconds in this case), then we'll happily move along.

As shown in the first example, if you want to control what exception is thrown if the event or condition is met before the time limit, you can use the ```throwing``` and subsequent ```when``` methods. Alternatively, you can pass an additional argument to ```failIf```: the throwable you'd want thrown.

License
=======

**synq** is licensed under [version 3 of the GPL][1].


  [1]: https://www.gnu.org/copyleft/gpl.html
  [2]: http://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html

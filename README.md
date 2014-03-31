synq
====

Synq is a small library for syncing up a thread with some flexible configuration of external event(s).

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

License
=======

**synq** is licensed under [version 3 of the GPL][1].


  [1]: https://www.gnu.org/copyleft/gpl.html

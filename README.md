# Generative - A simple generative test library for Java

[![Build Status](https://travis-ci.com/LiveRamp/generative.svg?branch=master)](https://travis-ci.com/LiveRamp/generative)

Generative and property based testing is getting increasing attention in functional programming circles as a way to test important aspects of code with minimal effort. After a very brief survey, the options in Java didn't feel like they met my needs - the primary option appears to be features of JUnit, which are all implemented using Annotations and feel non-intuitive to me. Given this, I decided to make my own.

A test using Generative:

```java
 @Test
  public void testMultiplyingByTwoGivesAnEvenNumber() {
    Generative.runTests(20, (testNumber, g) -> {
      Integer theInt = g.positiveIntegerLessThan(50).get();
      Integer shouldBeEven = theInt * 2;
      assertTrue(shouldBeEven % 2 == 0);
    });
  }
```

The primary entry point is the `Generative.runTests` static method, which runs a given test block a certain number of times. A test block accepts 2 arguments - a test number, and a Generative object used to create the various inputs to the test. The Generative object can create a number of different generators for primtives with random values. These values can be used check various properties of your code and verify that the properties hold regardless of which value is chosen.

### Why use Generative?

Generative provides 3 key features that may make it more appealing than building and using a Java Random object yourself

#### Helpers for generating inputs with certain properties

With a Java Random, you can only do these things for getting bounded Integers:
```java
Random r = new Random();
Integer mightBeAnyInteger = r.nextInt();
Integer isPositiveAndLessThanN = r.nextInt(n);
```
A generative object has these:
```java
Generative g;
Integer mightBeAnyInteger = g.anyInteger().get();
Integer isZeroToMaxValue = g.anyPositiveInteger().get();
Integer greaterThanN = g.anyIntegerGreaterThan(n).get();
Integer lessThanNButGreaterThan0 = g.anyPositiveIntegerLessThan(n).get();
Integer lessThanNBCouldBeNegative = g.anyIntegerLessThan(n).get();
```

#### Seed and variable management
One really important aspect of generative testing is making sure you can reproduce a test that fails so that you can find and fix the issue. Generating values from a Generative object ensures you use a consistent seed for a given test, and always prints the seed of a failing test in the exception message. Additionally, for getting nice error reports and to enable shrinking, you can name variables:
```java
 runTests(100, (testNumber, g) -> {
        Integer theInt = g.namedVar("The Number").anyPositiveIntegerLessThan(50).get();
        Integer whatWeMultipliedBy = g.namedVar("Multiple").boundedPositiveInteger(2, 4).get();
        Integer shouldBeEven = theInt * whatWeMultipliedBy;
        assertTrue(shouldBeEven % 2 == 0);
      });
```
The above test produces this error:
```
17/12/01 14:53:40 INFO generative.Generative: Returning shrunken test case - performed 6 shrinks
17/12/01 14:53:40 INFO generative.Generative: Generated variables were: 
The Number : 49
Multiple : 3

java.lang.RuntimeException: Shrunken test case failed with seed: 1207756009:1f8b080000000000000033d2310200c2d2482903000000
```
These seeds can be passed as additional arguments to `runTests` to ensure that once you've fixed a specific example, that specific example does not regress.

#### Shrinking

Often a random test will find some edge case, but the data used to find that case will be excessivly large or complicated for figuring out exactly what went wrong. Generative implements a primitive form of _shrinking_, which is simplifying input data to find the simplest possible test case that still fails. Generative provides the following interface:
```java
public interface Arbitrary<T> {

  @NotNull
  T get(Random r);

  default List<T> shrink(T val) {
    return Collections.emptyList();
  }
  // More default methods...
```

`get` is simple enough - this generates a random value. The `shrink` method takes a value generated by the class and returns a (potentially empty) list of simpler alternatives that should be tried. For bounded integers, Generative uses the following shrink method:

```java
 @Override
  public List<Integer> shrink(Integer val) {
    List<Integer> results = new ArrayList<>();
    Collections.addAll(results,
        Math.max(lowerBoundInclusive, 0),
        lowerBoundInclusive,
        upperBoundInclusive);
    return results;
  }
```
This method tries to simplify the test case by using 0 if it's within the bounds and then the bounds themselves to try to find a case that's easier to understand. Alternative values should be provided from simplest to least simple, as the library will use the first value that still fails the test.

It's important to note that Generative will only shrink variables which have been named using the the `namedVar` syntax described above. 

### Advanced Usage of Arbitrary

#### Using your own arbitrary instances

You can write your own Arbitrary instances - you can make something super simple by implementing only the `get` method, or potentially add your own candidates to `shrink` if you want. Once you've written an instance, you can use:

```java
  generative.generate(new MyArbitrary()).get()
```

to use your Arbitrary to generate random variables similar to the built in methods of the Generative object.

#### Map and flatMap

Arbitrary supports the special operation `flatMap` for transforming from one Arbitrary to another. Using the provided instances, you could make an Arbitrary that creates byte[]s of many different sizes using the following construction:

```java
Arbitrary<byte[]> randomlySizedByteArrays = new ArbitraryBoundedInteger(0,10)
  .flatMap(randomSize -> new ArbitraryByteArray(randomSize))
```

The Arbitrary we've created here first uses the root ArbitraryBoundedInteger to generate some size for the random array, then uses that size to create an ArbitraryByteArray that will create an array with random data. This is nifty, but it destroys the `shrink` method built in to `ArbitraryBoundedInteger` - we'd like this new Arbitrary to be able to shrink it's size using the same algorithm as the existing shrink. To do that, we provide a reversing function:

```java
Arbitrary<byte[]> randomlySizedByteArrays = new ArbitraryBoundedInteger(0,10)
  .flatMap(randomSize -> new ArbitraryByteArray(randomSize), 
           aByteArray -> aByteArray.length)
```

Supplying the reversal function means that `randomlySizedByteArrays` will know to shrink the size of the array, as well as simplifying it's contents.

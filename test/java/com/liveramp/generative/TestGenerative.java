package com.liveramp.generative;

import java.util.List;

import org.apache.log4j.Level;
import org.junit.Test;

import com.rapleaf.java_support.CommonJUnit4TestCase;

import static com.liveramp.generative.Generative.*;
import static org.junit.Assert.*;

public class TestGenerative extends CommonJUnit4TestCase {


  public TestGenerative() {
    super(Level.INFO);
  }

  @Test
  public void testShouldPass() {
    runTests(20, (testNumber, g) -> {
      Integer theInt = g.anyPositiveIntegerLessThan(50);
      Integer shouldBeEven = theInt * 2;
      assertTrue(shouldBeEven % 2 == 0);
    });
  }

  @Test
  public void testAlwaysFails() {
    try {
      runTests(10, (testNumber, g) -> {
        Integer theInt = g.anyPositiveIntegerLessThan(50);
        Integer shouldBeEven = theInt * 2;
        assertTrue(shouldBeEven % 2 == 1);
      });
      throw new RuntimeException("Should have failed");
    } catch (Exception e) {
      //expected
    }

  }

  @Test
  public void testFailsForSomeInputs() {
    try {
      runTests(20, (testNumber, g) -> {
        byte[] data = g.namedVar("The Data").anyByteArrayUpToLength(16);
        assertTrue(data.length < 8);
      });
      throw new RuntimeException("Should have failed");
    } catch (Exception e) {
      //expected
    }
  }

  @Test
  public void testBoundedIntegerIsBounded() {
    runTests(1000, (testNumber, g) -> {
      Integer bound1 = g.namedVar("bound1").anyInteger();
      Integer bound2 = g.namedVar("bound2").anyInteger();
      int lowerBound = Math.min(bound1, bound2);
      int upperBound = Math.max(bound1, bound2);
      Integer theNumber = g.namedVar("theNumber").anyBoundedInteger(lowerBound, upperBound);
      assertTrue("Number should be between bounds", theNumber >= lowerBound && theNumber <= upperBound);
    });
  }

  @Test
  public void testShrunkenIntegerIsBounded() {
    runTests(1000, (testNumber, g) -> {
      Integer bound1 = g.namedVar("bound1").anyInteger();
      Integer bound2 = g.namedVar("bound2").anyInteger();
      int lowerBound = Math.min(bound1, bound2);
      int upperBound = Math.max(bound1, bound2);
      ArbitraryBoundedInt arb = new ArbitraryBoundedInt(lowerBound, upperBound);
      Integer theNumber = arb.get(g.getInternalRandom());
      List<Integer> shrinks = arb.shrink(theNumber);
      for (Integer shrink : shrinks) {
        assertTrue("Shrink should be between bounds: "+shrink, shrink >= lowerBound && shrink <= upperBound);
      }
    });
  }
}
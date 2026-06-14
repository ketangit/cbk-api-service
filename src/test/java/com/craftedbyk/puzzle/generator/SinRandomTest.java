package com.craftedbyk.puzzle.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SinRandomTest {

  /**
   * Golden values captured from the reference JavaScript implementation (Node 22 / V8).
   * StrictMath.sin must reproduce them exactly so seeds generate the same puzzles as the original
   * web app.
   */
  @Test
  void matchesReferenceJavaScriptSequence() {
    SinRandom rng = new SinRandom(42);
    assertEquals(0.78452084366290364, rng.next(), 0.0);
    assertEquals(0.25257371401676210, rng.next(), 0.0);
    assertEquals(0.019251054135764889, rng.next(), 0.0);
    assertEquals(0.035245341185145662, rng.next(), 0.0);
    assertEquals(0.88347648809212842, rng.next(), 0.0);
  }

  @Test
  void valuesStayInUnitInterval() {
    SinRandom rng = new SinRandom(7);
    for (int i = 0; i < 10_000; i++) {
      double v = rng.next();
      assertTrue(v >= 0.0 && v < 1.0, "out of range: " + v);
    }
  }

  @Test
  void secureSourceProducesVariedValues() {
    SecureRandomSource rng = new SecureRandomSource();
    assertNotEquals(rng.next(), rng.next());
  }
}

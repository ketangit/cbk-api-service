package com.craftedbyk.puzzle.generator;

/**
 * Deterministic PRNG identical to the reference JavaScript implementation:
 *
 * <pre>
 * var x = Math.sin(seed) * 10000;
 * seed += 1;
 * return x - Math.floor(x);
 * </pre>
 *
 * The same seed always reproduces the same puzzle, which is what makes exported SVGs reproducible
 * from the seed shown in the UI.
 */
public final class SinRandom implements RandomSource {

  private double seed;

  public SinRandom(double seed) {
    this.seed = seed;
  }

  @Override
  public double next() {
    // StrictMath (fdlibm) matches the sin() used by V8/SpiderMonkey,
    // so seeds reproduce the exact same puzzles as the reference web app.
    double x = StrictMath.sin(seed) * 10000.0;
    seed += 1.0;
    return x - Math.floor(x);
  }

  /** Current internal counter; used to reproduce the reference coloring seed. */
  public double currentSeed() {
    return seed;
  }
}

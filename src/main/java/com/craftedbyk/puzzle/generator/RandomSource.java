package com.craftedbyk.puzzle.generator;

/**
 * Source of doubles in [0, 1). Two implementations exist: {@link SinRandom} (deterministic,
 * reproduces the reference generator bit-for-bit) and {@link SecureRandomSource}
 * (non-deterministic, unique every run).
 */
public interface RandomSource {

  /** Next pseudo-random double in [0, 1). */
  double next();

  /** Uniform double in [min, max). Mirrors reference uniform(). */
  default double uniform(double min, double max) {
    return min + next() * (max - min);
  }
}

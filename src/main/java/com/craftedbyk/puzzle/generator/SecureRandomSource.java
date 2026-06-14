package com.craftedbyk.puzzle.generator;

import java.security.SecureRandom;

/**
 * Non-deterministic randomness backed by {@link SecureRandom} (OS entropy). Every generation
 * produces a unique, unrepeatable puzzle.
 */
public final class SecureRandomSource implements RandomSource {

  private final SecureRandom rng = new SecureRandom();

  @Override
  public double next() {
    return rng.nextDouble();
  }
}

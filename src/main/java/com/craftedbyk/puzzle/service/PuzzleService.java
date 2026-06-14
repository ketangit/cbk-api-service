package com.craftedbyk.puzzle.service;

import com.craftedbyk.puzzle.generator.CircleFractalJigsaw;
import com.craftedbyk.puzzle.generator.PuzzleSpec;
import com.craftedbyk.puzzle.generator.SinRandom;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

@Service
public class PuzzleService {

  private final SecureRandom entropy = new SecureRandom();

  /**
   * Generates a puzzle. When {@code spec.nonDeterministic} is set, a fresh seed is drawn from OS
   * entropy ({@link SecureRandom}) so every click yields a unique puzzle; the drawn seed is
   * reported back so the exact same puzzle can still be exported and re-ordered.
   */
  public GeneratedPuzzle generate(PuzzleSpec spec) {
    double seedUsed = spec.nonDeterministic ? (double) entropy.nextInt(100_000_000) : spec.seed;
    SinRandom rng = new SinRandom(seedUsed);
    CircleFractalJigsaw jigsaw =
        new CircleFractalJigsaw(spec.ncols, spec.nrows, spec.minPieceSize, spec.maxPieceSize, rng);
    jigsaw.generateAll();
    return new GeneratedPuzzle(spec, jigsaw, seedUsed, rng.currentSeed());
  }
}
